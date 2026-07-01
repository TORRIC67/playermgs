package com.playermgs.service;

import com.playermgs.dao.PlayerDao;
import com.playermgs.dao.TeamDao;
import com.playermgs.model.Player;
import com.playermgs.model.Team;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for Player management — now backed by H2 via JDBC.
 *
 * Uses StatsService via Dependency (⑤) — created fresh, never stored
 * as permanent state beyond this service instance.
 */
public class PlayerService {

    private final PlayerDao     playerDao;
    private final TeamDao       teamDao;
    private final StatsService  statsService;   // Dependency ⑤

    public PlayerService() {
        this.playerDao    = new PlayerDao();
        this.teamDao      = new TeamDao();
        this.statsService  = new StatsService();
    }

    // ── CRUD ──────────────────────────────────────────────

    public Player create(Player player) {
        validatePlayer(player);
        player.updateRating(statsService);
        player.updateMarketValue(statsService);
        return playerDao.insert(player);
    }

    public Optional<Player> findById(Long id) {
        Optional<Player> p = playerDao.findById(id);
        p.ifPresent(player -> player.setMvHistory(playerDao.getMvHistory(id)));
        return p;
    }

    public List<Player> findAll() {
        return playerDao.findAll();
    }

    public List<Player> findByTeam(Long teamId) {
        return playerDao.findByTeam(teamId);
    }

    public List<Player> findByPosition(String position) {
        return playerDao.findByPosition(position);
    }

    public List<Player> findByStatus(String status) {
        return playerDao.findByStatus(status);
    }

    public Player update(Player updated) {
        if (!playerDao.exists(updated.getId()))
            throw new IllegalArgumentException("Player not found: id=" + updated.getId());
        validatePlayer(updated);
        updated.updateRating(statsService);
        updated.updateMarketValue(statsService);
        return playerDao.update(updated);
    }

    public boolean delete(Long id) {
        if (!playerDao.exists(id))
            throw new IllegalArgumentException("Player not found: id=" + id);
        return playerDao.delete(id);
    }

    // ── Transfer ───────────────────────────────────────────

    /**
     * Transfer a player to a new team.
     * Validates team exists and budget is sufficient.
     */
    public Player transfer(Long playerId, Long newTeamId) {
        Player player = playerDao.findById(playerId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Player not found: id=" + playerId));

        Team newTeam = teamDao.findById(newTeamId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Team not found: id=" + newTeamId));

        double cost = player.getMarketValue() * 1_000_000;
        if (newTeam.getBudget() < cost) {
            throw new IllegalStateException(
                "Team '" + newTeam.getName() + "' budget insufficient. " +
                "Required: $" + player.getMarketValue() + "M, " +
                "Available: $" + (newTeam.getBudget() / 1_000_000) + "M");
        }

        teamDao.updateBudget(newTeamId, newTeam.getBudget() - cost);

        player.transferTo(newTeamId);
        player.updateMarketValue(statsService);
        return playerDao.update(player);
    }

    // ── Market value ───────────────────────────────────────

    public Player refreshStats(Long playerId,
                               int goals, int assists, int minutes,
                               int yellows, int reds) {
        Player player = playerDao.findById(playerId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Player not found: id=" + playerId));

        player.setGoals(goals);
        player.setAssists(assists);
        player.setMinutesPlayed(minutes);
        player.setYellowCards(yellows);
        player.setRedCards(reds);

        player.updateRating(statsService);          // Dependency ⑤
        player.updateMarketValue(statsService);

        return playerDao.update(player);
    }

    public Player setMarketValue(Long playerId, double valueMillion) {
        Player player = playerDao.findById(playerId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Player not found: id=" + playerId));

        player.setMarketValue(valueMillion);
        return playerDao.update(player);   // also appends to mv history table
    }

    // ── Validation ─────────────────────────────────────────

    private void validatePlayer(Player p) {
        if (p.getName() == null || p.getName().isBlank())
            throw new IllegalArgumentException("Player name is required.");
        if (p.getAge() < 15 || p.getAge() > 50)
            throw new IllegalArgumentException("Player age must be between 15 and 50.");
        if (p.getPosition() == null ||
            !List.of("FWD","MID","DEF","GK").contains(p.getPosition().toUpperCase()))
            throw new IllegalArgumentException("Position must be FWD, MID, DEF, or GK.");
        if (p.getTeamId() != null && !teamDao.exists(p.getTeamId()))
            throw new IllegalArgumentException("Team not found: id=" + p.getTeamId());
    }
}
