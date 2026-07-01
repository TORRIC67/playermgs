package com.playermgs.dao;

import com.playermgs.model.Contract;
import com.playermgs.model.Team;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ─────────────────────────────────────────────────────────────
 *  TeamDao — translates Team objects ↔ SQL rows
 * ─────────────────────────────────────────────────────────────
 *  Composition ③: contracts are loaded together with their team
 *  and deleted automatically via ON DELETE CASCADE in the schema.
 * ─────────────────────────────────────────────────────────────
 */
public class TeamDao {

    private Team mapRow(ResultSet rs) throws SQLException {
        Team t = new Team();
        t.setId(rs.getLong("id"));
        t.setName(rs.getString("name"));
        t.setCity(rs.getString("city"));
        t.setStadium(rs.getString("stadium"));
        t.setBudget(rs.getDouble("budget"));
        long leagueId = rs.getLong("league_id");
        t.setLeagueId(rs.wasNull() ? null : leagueId);
        return t;
    }

    public List<Team> findAll() {
        String sql = "SELECT * FROM teams ORDER BY id";
        List<Team> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Team t = mapRow(rs);
                attachContracts(conn, t);
                result.add(t);
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll teams failed", e);
        }
        return result;
    }

    public Optional<Team> findById(Long id) {
        String sql = "SELECT * FROM teams WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Team t = mapRow(rs);
                    attachContracts(conn, t);
                    return Optional.of(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById team failed", e);
        }
        return Optional.empty();
    }

    public List<Team> findByLeague(Long leagueId) {
        String sql = "SELECT * FROM teams WHERE league_id = ? ORDER BY id";
        List<Team> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, leagueId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Team t = mapRow(rs);
                    attachContracts(conn, t);
                    result.add(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByLeague teams failed", e);
        }
        return result;
    }

    public Team insert(Team t) {
        String sql = "INSERT INTO teams (name, city, stadium, budget, league_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, t.getName());
            ps.setString(2, t.getCity());
            ps.setString(3, t.getStadium());
            ps.setDouble(4, t.getBudget());
            if (t.getLeagueId() != null) ps.setLong(5, t.getLeagueId());
            else ps.setNull(5, Types.BIGINT);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) t.setId(keys.getLong(1));
            }
            return t;

        } catch (SQLException e) {
            throw new RuntimeException("insert team failed", e);
        }
    }

    public Team update(Team t) {
        String sql = "UPDATE teams SET name=?, city=?, stadium=?, budget=?, league_id=? WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, t.getName());
            ps.setString(2, t.getCity());
            ps.setString(3, t.getStadium());
            ps.setDouble(4, t.getBudget());
            if (t.getLeagueId() != null) ps.setLong(5, t.getLeagueId());
            else ps.setNull(5, Types.BIGINT);
            ps.setLong(6, t.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) throw new RuntimeException("Team not found: id=" + t.getId());
            return t;

        } catch (SQLException e) {
            throw new RuntimeException("update team failed", e);
        }
    }

    /** Updates only the budget column — used heavily by transfers. */
    public void updateBudget(Long teamId, double newBudget) {
        String sql = "UPDATE teams SET budget = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBudget);
            ps.setLong(2, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateBudget failed", e);
        }
    }

    /** Deletes the team. Contracts cascade-delete automatically (Composition ③).
     *  Players and coaches just have their team_id set to NULL (handled by FK ON DELETE SET NULL). */
    public boolean delete(Long id) {
        String sql = "DELETE FROM teams WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("delete team failed", e);
        }
    }

    public boolean exists(Long id) {
        String sql = "SELECT 1 FROM teams WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("exists team check failed", e);
        }
    }

    // ── Composition ③: Contract operations, always tied to a team ──

    public Contract addContract(Long teamId, Contract c) {
        String sql = """
            INSERT INTO contracts (team_id, salary_per_year, start_date, end_date, status)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, teamId);
            ps.setDouble(2, c.getSalaryPerYear());
            ps.setDate(3, c.getStartDate() != null ? Date.valueOf(c.getStartDate()) : null);
            ps.setDate(4, c.getEndDate()   != null ? Date.valueOf(c.getEndDate())   : null);
            ps.setString(5, c.getStatus());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getLong(1));
            }
            c.setTeamId(teamId);
            return c;

        } catch (SQLException e) {
            throw new RuntimeException("addContract failed", e);
        }
    }

    /** Loads contracts for a team and attaches them (called by findAll/findById). */
    private void attachContracts(Connection conn, Team t) throws SQLException {
        String sql = "SELECT * FROM contracts WHERE team_id = ? ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, t.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Contract c = new Contract();
                    c.setId(rs.getLong("id"));
                    c.setTeamId(rs.getLong("team_id"));
                    c.setSalaryPerYear(rs.getDouble("salary_per_year"));
                    Date start = rs.getDate("start_date");
                    Date end   = rs.getDate("end_date");
                    c.setStartDate(start != null ? start.toLocalDate() : null);
                    c.setEndDate(end != null ? end.toLocalDate() : null);
                    c.setStatus(rs.getString("status"));
                    t.addContract(c);   // Team's own in-memory list, populated from DB
                }
            }
        }
    }
}
