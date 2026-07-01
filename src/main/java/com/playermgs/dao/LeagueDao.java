package com.playermgs.dao;

import com.playermgs.model.League;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ─────────────────────────────────────────────────────────────
 *  LeagueDao — translates League objects ↔ SQL rows
 * ─────────────────────────────────────────────────────────────
 *  Aggregation ②: leagues don't "own" their teams in the database
 *  either — teams just carry a league_id foreign key, and the
 *  schema uses ON DELETE SET NULL so deleting a league leaves
 *  its teams alive, only detached.
 * ─────────────────────────────────────────────────────────────
 */
public class LeagueDao {

    private League mapRow(ResultSet rs) throws SQLException {
        League l = new League();
        l.setId(rs.getLong("id"));
        l.setName(rs.getString("name"));
        l.setCountry(rs.getString("country"));
        l.setSeason(rs.getString("season"));
        l.setDivision(rs.getInt("division"));
        return l;
    }

    public List<League> findAll() {
        String sql = "SELECT * FROM leagues ORDER BY id";
        List<League> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                League l = mapRow(rs);
                attachTeamIds(conn, l);
                result.add(l);
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll leagues failed", e);
        }
        return result;
    }

    public Optional<League> findById(Long id) {
        String sql = "SELECT * FROM leagues WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    League l = mapRow(rs);
                    attachTeamIds(conn, l);
                    return Optional.of(l);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById league failed", e);
        }
        return Optional.empty();
    }

    public League insert(League l) {
        String sql = "INSERT INTO leagues (name, country, season, division) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, l.getName());
            ps.setString(2, l.getCountry());
            ps.setString(3, l.getSeason());
            ps.setInt(4, l.getDivision());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) l.setId(keys.getLong(1));
            }
            return l;

        } catch (SQLException e) {
            throw new RuntimeException("insert league failed", e);
        }
    }

    public League update(League l) {
        String sql = "UPDATE leagues SET name=?, country=?, season=?, division=? WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, l.getName());
            ps.setString(2, l.getCountry());
            ps.setString(3, l.getSeason());
            ps.setInt(4, l.getDivision());
            ps.setLong(5, l.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) throw new RuntimeException("League not found: id=" + l.getId());
            return l;

        } catch (SQLException e) {
            throw new RuntimeException("update league failed", e);
        }
    }

    /**
     * Deletes the league. Teams in this league are NOT deleted —
     * the foreign key constraint (ON DELETE SET NULL) detaches them
     * automatically at the database level (Aggregation ②).
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM leagues WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("delete league failed", e);
        }
    }

    public boolean exists(Long id) {
        String sql = "SELECT 1 FROM leagues WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("exists league check failed", e);
        }
    }

    /** Loads the ids of teams currently in this league (Aggregation ②). */
    private void attachTeamIds(Connection conn, League l) throws SQLException {
        String sql = "SELECT id FROM teams WHERE league_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, l.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) l.addTeam(rs.getLong("id"));
            }
        }
    }
}
