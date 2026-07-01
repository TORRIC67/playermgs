package com.playermgs.model;

import com.playermgs.service.StatsService;
import java.util.ArrayList;
import java.util.List;

/**
 * Relationship ① INHERITANCE  — Player IS-A Person (extends Person).
 * Relationship ④ ASSOCIATION  — Player references a Team via teamId.
 *                                Neither owns the other; both survive independently.
 * Relationship ⑤ DEPENDENCY   — Player uses StatsService as a method parameter,
 *                                never as a stored field.
 */
public class Player extends Person {

    // ── Player-specific fields ─────────────────────────────
    private String position;       // "FWD" | "MID" | "DEF" | "GK"
    private String status;         // "active" | "injured" | "bench"
    private double rating;         // 0.0 – 10.0, updated via StatsService
    private double marketValue;    // current value in $M
    private List<Double> mvHistory = new ArrayList<>();   // value per season

    // ── Season stats (raw numbers fed to StatsService) ─────
    private int goals;
    private int assists;
    private int minutesPlayed;
    private int yellowCards;
    private int redCards;

    // ── Relationships ──────────────────────────────────────
    private Long teamId;      // Association ④ — just a reference

    public Player() {}

    public Player(Long id, String name, int age, String email, String nationality,
                  String position, String status, double rating,
                  double marketValue, Long teamId) {
        super(id, name, age, email, nationality);
        this.position    = position;
        this.status      = status;
        this.rating      = rating;
        this.marketValue = marketValue;
        this.teamId      = teamId;
    }

    // ── Methods that use StatsService (Dependency ⑤) ──────

    /**
     * Recalculate this player's rating using the provided StatsService.
     * StatsService is passed in — never stored as a field.
     */
    public void updateRating(StatsService service) {
        this.rating = service.calculateRating(
            goals, assists, minutesPlayed, yellowCards, redCards
        );
    }

    /**
     * Recalculate and update market value using the provided StatsService.
     * Appends the new value to the history list.
     */
    public void updateMarketValue(StatsService service) {
        double newValue = service.estimateMarketValue(rating, getAge(), position);
        this.marketValue = newValue;
        this.mvHistory.add(newValue);
    }

    /**
     * Returns season-over-season value trend using the provided StatsService.
     */
    public double getValueTrend(StatsService service) {
        return service.valueTrend(mvHistory);
    }

    // ── Transfer helpers ───────────────────────────────────
    /** Move this player to a new team. */
    public void transferTo(Long newTeamId) {
        this.teamId = newTeamId;
        this.status = "active";
    }

    /** Release player (no team). */
    public void release() {
        this.teamId = null;
        this.status = "bench";
    }

    // ── Getters & Setters ──────────────────────────────────
    public String getPosition()                 { return position; }
    public void   setPosition(String position)  { this.position = position; }

    public String getStatus()               { return status; }
    public void   setStatus(String status)  { this.status = status; }

    public double getRating()               { return rating; }
    public void   setRating(double rating)  { this.rating = rating; }

    public double getMarketValue()                    { return marketValue; }
    public void   setMarketValue(double marketValue)  { this.marketValue = marketValue; }

    public List<Double> getMvHistory()                      { return mvHistory; }
    public void         setMvHistory(List<Double> mvHistory){ this.mvHistory = mvHistory; }

    public int  getGoals()              { return goals; }
    public void setGoals(int goals)     { this.goals = goals; }

    public int  getAssists()              { return assists; }
    public void setAssists(int assists)   { this.assists = assists; }

    public int  getMinutesPlayed()                    { return minutesPlayed; }
    public void setMinutesPlayed(int minutesPlayed)   { this.minutesPlayed = minutesPlayed; }

    public int  getYellowCards()                  { return yellowCards; }
    public void setYellowCards(int yellowCards)   { this.yellowCards = yellowCards; }

    public int  getRedCards()               { return redCards; }
    public void setRedCards(int redCards)   { this.redCards = redCards; }

    public Long getTeamId()                 { return teamId; }
    public void setTeamId(Long teamId)      { this.teamId = teamId; }

    @Override
    public String toString() {
        return "Player{id=" + getId() + ", name='" + getName()
            + "', pos='" + position + "', rating=" + rating
            + ", mv=$" + marketValue + "M, teamId=" + teamId + "}";
    }
}
