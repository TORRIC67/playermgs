package com.playermgs.service;

import com.playermgs.dao.PlayerDao;
import com.playermgs.dao.TeamDao;
import com.playermgs.model.Contract;
import com.playermgs.model.Player;
import com.playermgs.model.Team;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for Team management — backed by H2 via JDBC.
 *
 * Composition ③: contracts live entirely inside the team's table
 * relationship and are deleted automatically (ON DELETE CASCADE)
 * when the team itself is deleted.
 */
public class TeamService {

    private final TeamDao   teamDao;
    private final PlayerDao playerDao;

    public TeamService() {
        this.teamDao   = new TeamDao();
        this.playerDao = new PlayerDao();
    }

    public Team create(Team team) {
        validate(team);
        return teamDao.insert(team);
    }

    public Optional<Team> findById(Long id) { return teamDao.findById(id); }
    public List<Team>     findAll()         { return teamDao.findAll(); }

    public List<Team> findByLeague(Long leagueId) {
        return teamDao.findByLeague(leagueId);
    }

    public Team update(Team updated) {
        if (!teamDao.exists(updated.getId()))
            throw new IllegalArgumentException("Team not found: id=" + updated.getId());
        validate(updated);
        return teamDao.update(updated);
    }

    /**
     * Delete a team. Composition ③: contracts are cascade-deleted
     * by the database automatically. Players and coaches simply
     * lose their team reference (handled by FK ON DELETE SET NULL),
     * but we also flip player status to "bench" for clarity.
     */
    public boolean delete(Long teamId) {
        if (!teamDao.exists(teamId))
            throw new IllegalArgumentException("Team not found: id=" + teamId);

        for (Player p : playerDao.findByTeam(teamId)) {
            p.setTeamId(null);
            p.setStatus("bench");
            playerDao.update(p);
        }

        return teamDao.delete(teamId);
    }

    public Team updateBudget(Long teamId, double newBudget) {
        if (!teamDao.exists(teamId))
            throw new IllegalArgumentException("Team not found: id=" + teamId);
        if (newBudget < 0)
            throw new IllegalArgumentException("Budget cannot be negative.");
        teamDao.updateBudget(teamId, newBudget);
        return teamDao.findById(teamId).orElseThrow();
    }

    /** Add a new contract to a team (Composition ③). */
    public Contract addContract(Long teamId, Contract contract) {
        if (!teamDao.exists(teamId))
            throw new IllegalArgumentException("Team not found: id=" + teamId);
        return teamDao.addContract(teamId, contract);
    }

    private void validate(Team t) {
        if (t.getName() == null || t.getName().isBlank())
            throw new IllegalArgumentException("Team name is required.");
    }
}
