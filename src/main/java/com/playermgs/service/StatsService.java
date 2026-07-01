package com.playermgs.service;

/**
 * Relationship ⑤ DEPENDENCY
 * Player methods receive StatsService as a parameter — it is
 * never stored as a field. The dependency is method-scoped only.
 *
 * Responsibilities:
 *  - Calculate a player's performance rating from raw stats
 *  - Estimate market value from rating, age, position, and history
 *  - Compute value trend over seasons
 */
public class StatsService {

    // Position multipliers for market value estimation
    private static final double MV_FWD = 1.30;
    private static final double MV_MID = 1.10;
    private static final double MV_DEF = 1.00;
    private static final double MV_GK  = 0.90;

    // Peak age for market value (younger or older = discount)
    private static final int PEAK_AGE = 26;

    /**
     * Calculate a performance rating (0.0 – 10.0) from raw stats.
     *
     * @param goals     goals scored or conceded (GK: goals allowed)
     * @param assists    assists (or clean sheets for GK)
     * @param minutesPlayed   total minutes played this season
     * @param yellowCards     yellow cards received
     * @param redCards        red cards received
     * @return rating 0.0 – 10.0
     */
    public double calculateRating(int goals, int assists,
                                  int minutesPlayed,
                                  int yellowCards, int redCards) {
        if (minutesPlayed <= 0) return 0.0;

        double base      = 5.0;
        double goalBonus = goals   * 0.30;
        double astBonus  = assists * 0.20;

        // More minutes = more reliable sample
        double minuteFactor = Math.min(minutesPlayed / 900.0, 1.0) * 1.5;

        double cardPenalty = (yellowCards * 0.10) + (redCards * 0.40);

        double raw = base + goalBonus + astBonus + minuteFactor - cardPenalty;
        return Math.max(0.0, Math.min(10.0, Math.round(raw * 10.0) / 10.0));
    }

    /**
     * Estimate market value in millions USD.
     *
     * @param rating   performance rating (0–10)
     * @param age      player age
     * @param position "FWD", "MID", "DEF", or "GK"
     * @return estimated value in $M
     */
    public double estimateMarketValue(double rating, int age, String position) {
        double posMultiplier = switch (position.toUpperCase()) {
            case "FWD" -> MV_FWD;
            case "MID" -> MV_MID;
            case "DEF" -> MV_DEF;
            case "GK"  -> MV_GK;
            default    -> 1.00;
        };

        // Age curve: peak at PEAK_AGE, discount on either side
        double ageFactor;
        int    ageDiff = Math.abs(age - PEAK_AGE);
        if      (ageDiff == 0) ageFactor = 1.00;
        else if (ageDiff == 1) ageFactor = 0.97;
        else if (ageDiff == 2) ageFactor = 0.93;
        else if (ageDiff <= 4) ageFactor = 0.87;
        else                   ageFactor = 0.75;

        // Very young players (potential bonus)
        if (age <= 21) ageFactor = Math.min(ageFactor + 0.10, 1.05);

        double base = rating * rating * posMultiplier * ageFactor;
        return Math.round(base * 10.0) / 10.0;   // rounded to 1 decimal
    }

    /**
     * Calculate season-over-season value trend.
     *
     * @param history  list of market values (oldest first, most recent last)
     * @return delta between most recent and previous season, or 0 if not enough data
     */
    public double valueTrend(java.util.List<Double> history) {
        if (history == null || history.size() < 2) return 0.0;
        double latest   = history.get(history.size() - 1);
        double previous = history.get(history.size() - 2);
        return Math.round((latest - previous) * 10.0) / 10.0;
    }
}
