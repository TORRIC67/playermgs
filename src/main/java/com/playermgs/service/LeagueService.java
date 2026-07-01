package com.playermgs.service;

import com.playermgs.dao.LeagueDao;
import com.playermgs.dao.TeamDao;
import com.playermgs.model.League;
import com.playermgs.model.Team;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for League management — backed by H2 via JDBC.
 *
 * Aggregation ②: deleting a league does NOT delete its teams.
 * The database foreign key (ON DELETE SET NULL) handles the
 * detachment automatically; this service just confirms it.
 */
public class LeagueService {

    private final LeagueDao leagueDao;
    private final TeamDao   teamDao;

    public LeagueService() {
        this.leagueDao = new LeagueDao();
        this.teamDao   = new TeamDao();
    }

    // ── CRUD ──────────────────────────────────────────────

    public League create(League league) {
        validate(league);
        return leagueDao.insert(league);
    }

    public Optional<League> findById(Long id) {
        return leagueDao.findById(id);
    }

    public List<League> findAll() {
        return leagueDao.findAll();
    }

    /** Edit league details: name, country, season, division. */
    public League update(Long leagueId, String name, String country,
                         String season, int division) {
        League league = leagueDao.findById(leagueId)
            .orElseThrow(() -> new IllegalArgumentException(
                "League not found: id=" + leagueId));

        if (name    != null && !name.isBlank())    league.setName(name);
        if (country != null && !country.isBlank()) league.setCountry(country);
        if (season  != null && !season.isBlank())  league.setSeason(season);
        if (division > 0)                          league.setDivision(division);

        validate(league);
        return leagueDao.update(league);
    }

    /**
     * Delete a league. Teams in that league survive — the database
     * foreign key constraint sets their league_id to NULL automatically.
     */
    public boolean delete(Long leagueId) {
        if (!leagueDao.exists(leagueId))
            throw new IllegalArgumentException("League not found: id=" + leagueId);
        return leagueDao.delete(leagueId);
    }

    // ── Team ↔ League management (Aggregation ②) ──────────

    public League addTeam(Long leagueId, Long teamId) {
        League league = leagueDao.findById(leagueId)
            .orElseThrow(() -> new IllegalArgumentException(
                "League not found: id=" + leagueId));

        Team team = teamDao.findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Team not found: id=" + teamId));

        team.setLeagueId(leagueId);
        teamDao.update(team);

        return leagueDao.findById(leagueId).orElse(league);
    }

    public League removeTeam(Long leagueId, Long teamId) {
        if (!leagueDao.exists(leagueId))
            throw new IllegalArgumentException("League not found: id=" + leagueId);

        teamDao.findById(teamId).ifPresent(t -> {
            t.setLeagueId(null);
            teamDao.update(t);
        });

        return leagueDao.findById(leagueId).orElseThrow();
    }

    public League advanceSeason(Long leagueId, String newSeason) {
        League league = leagueDao.findById(leagueId)
            .orElseThrow(() -> new IllegalArgumentException(
                "League not found: id=" + leagueId));
        league.setSeason(newSeason);
        return leagueDao.update(league);
    }

    // ── Validation ─────────────────────────────────────────

    private void validate(League l) {
        if (l.getName() == null || l.getName().isBlank())
            throw new IllegalArgumentException("League name is required.");
        if (l.getCountry() == null || l.getCountry().isBlank())
            throw new IllegalArgumentException("League country is required.");
        if (l.getSeason() == null || l.getSeason().isBlank())
            throw new IllegalArgumentException("League season is required.");
        if (l.getDivision() < 1)
            throw new IllegalArgumentException("Division must be 1 or higher.");
    }
}
