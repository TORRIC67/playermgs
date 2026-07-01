package com.playermgs.dao;

import com.playermgs.model.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ─────────────────────────────────────────────────────────────
 *  PlayerDao — translates Player objects ↔ SQL rows
 * ─────────────────────────────────────────────────────────────
 *  Every method opens its own connection (try-with-resources)
 *  and closes it automatically — no connection leaks.
 * ─────────────────────────────────────────────────────────────
 */
public class PlayerDao {

    /** Maps one row of the ResultSet into a Player object. */
    private Player mapRow(ResultSet rs) throws SQLException {
        Player p = new Player();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setAge(rs.getInt("age"));
        p.setEmail(rs.getString("email"));
        p.setNationality(rs.getString("nationality"));
        p.setPosition(rs.getString("position"));
        p.setStatus(rs.getString("status"));
        p.setRating(rs.getDouble("rating"));
        p.setMarketValue(rs.getDouble("market_value"));
        p.setGoals(rs.getInt("goals"));
        p.setAssists(rs.getInt("assists"));
        p.setMinutesPlayed(rs.getInt("minutes_played"));
        p.setYellowCards(rs.getInt("yellow_cards"));
        p.setRedCards(rs.getInt("red_cards"));
        long teamId = rs.getLong("team_id");
        p.setTeamId(rs.wasNull() ? null : teamId);
        return p;
    }

    public List<Player> findAll() {
        String sql = "SELECT * FROM players ORDER BY id";
        List<Player> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) result.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll players failed", e);
        }
        return result;
    }

    public Optional<Player> findById(Long id) {
        String sql = "SELECT * FROM players WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById player failed", e);
        }
        return Optional.empty();
    }

    public List<Player> findByTeam(Long teamId) {
        String sql = "SELECT * FROM players WHERE team_id = ? ORDER BY id";
        List<Player> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByTeam players failed", e);
        }
        return result;
    }

    public List<Player> findByPosition(String position) {
        String sql = "SELECT * FROM players WHERE position = ? ORDER BY id";
        List<Player> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, position.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByPosition players failed", e);
        }
        return result;
    }

    public List<Player> findByStatus(String status) {
        String sql = "SELECT * FROM players WHERE status = ? ORDER BY id";
        List<Player> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByStatus players failed", e);
        }
        return result;
    }

    /** Inserts a new player and sets the generated id back onto the object. */
    public Player insert(Player p) {
        String sql = """
            INSERT INTO players
                (name, age, email, nationality, position, status,
                 rating, market_value, goals, assists, minutes_played,
                 yellow_cards, red_cards, team_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindPlayer(ps, p);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getLong(1));
            }
            recordMvHistory(p.getId(), p.getMarketValue());
            return p;

        } catch (SQLException e) {
            throw new RuntimeException("insert player failed", e);
        }
    }

    /** Updates every column of an existing player. */
    public Player update(Player p) {
        String sql = """
            UPDATE players SET
                name = ?, age = ?, email = ?, nationality = ?, position = ?,
                status = ?, rating = ?, market_value = ?, goals = ?, assists = ?,
                minutes_played = ?, yellow_cards = ?, red_cards = ?, team_id = ?
            WHERE id = ?
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindPlayer(ps, p);
            ps.setLong(15, p.getId());
            int rows = ps.executeUpdate();
            if (rows == 0) throw new RuntimeException("Player not found: id=" + p.getId());

            recordMvHistory(p.getId(), p.getMarketValue());
            return p;

        } catch (SQLException e) {
            throw new RuntimeException("update player failed", e);
        }
    }

    public boolean delete(Long id) {
        String sql = "DELETE FROM players WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("delete player failed", e);
        }
    }

    public boolean exists(Long id) {
        String sql = "SELECT 1 FROM players WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("exists player check failed", e);
        }
    }

    /** Returns market value history (oldest first) for sparklines. */
    public List<Double> getMvHistory(Long playerId) {
        String sql = "SELECT market_value FROM market_value_history WHERE player_id = ? ORDER BY recorded_at";
        List<Double> result = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getDouble("market_value"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("getMvHistory failed", e);
        }
        return result;
    }

    /** Appends one row to market_value_history — called after every save. */
    private void recordMvHistory(Long playerId, double value) {
        String sql = "INSERT INTO market_value_history (player_id, market_value) VALUES (?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, playerId);
            ps.setDouble(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("recordMvHistory failed", e);
        }
    }

    /** Shared parameter binding for insert/update (same column order). */
    private void bindPlayer(PreparedStatement ps, Player p) throws SQLException {
        ps.setString(1, p.getName());
        ps.setInt(2, p.getAge());
        ps.setString(3, p.getEmail());
        ps.setString(4, p.getNationality());
        ps.setString(5, p.getPosition());
        ps.setString(6, p.getStatus());
        ps.setDouble(7, p.getRating());
        ps.setDouble(8, p.getMarketValue());
        ps.setInt(9, p.getGoals());
        ps.setInt(10, p.getAssists());
        ps.setInt(11, p.getMinutesPlayed());
        ps.setInt(12, p.getYellowCards());
        ps.setInt(13, p.getRedCards());
        if (p.getTeamId() != null) ps.setLong(14, p.getTeamId());
        else ps.setNull(14, Types.BIGINT);
    }
}
