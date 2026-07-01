package com.playermgs.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ─────────────────────────────────────────────────────────────
 *  Database — single connection point for H2 (JDBC, no ORM)
 * ─────────────────────────────────────────────────────────────
 *  H2 stores everything in one file: playermgs.mv.db
 *  No install needed — the driver is just a .jar on the classpath.
 *
 *  Connection URL breakdown:
 *    jdbc:h2:./playermgs   → file-based DB named "playermgs" in current folder
 *    ;AUTO_SERVER=TRUE     → lets multiple processes (your app + H2 console) connect
 * ─────────────────────────────────────────────────────────────
 */
public final class Database {

    private static final String URL  = "jdbc:h2:./playermgs;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASS = "";

    private Database() { }

    /** Get a fresh JDBC connection. Caller is responsible for closing it
     *  (use try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /**
     * Creates all tables if they don't already exist, and seeds
     * a few demo rows the first time the database is created.
     * Safe to call every time the app starts.
     */
    public static void initSchema() {
        String createLeagues = """
            CREATE TABLE IF NOT EXISTS leagues (
                id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                name      VARCHAR(100) NOT NULL,
                country   VARCHAR(100) NOT NULL,
                season    VARCHAR(20)  NOT NULL,
                division  INT          NOT NULL
            )
            """;

        String createTeams = """
            CREATE TABLE IF NOT EXISTS teams (
                id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                name      VARCHAR(100) NOT NULL,
                city      VARCHAR(100),
                stadium   VARCHAR(100),
                budget    DOUBLE NOT NULL DEFAULT 0,
                league_id BIGINT,
                FOREIGN KEY (league_id) REFERENCES leagues(id) ON DELETE SET NULL
            )
            """;

        // Composition ③: contracts belong to a team and are deleted with it
        String createContracts = """
            CREATE TABLE IF NOT EXISTS contracts (
                id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                team_id         BIGINT NOT NULL,
                salary_per_year DOUBLE NOT NULL,
                start_date      DATE,
                end_date        DATE,
                status          VARCHAR(20) DEFAULT 'ACTIVE',
                FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
            )
            """;

        // Inheritance ①: players + coaches share "person" fields,
        // modelled here as their own tables (table-per-class approach,
        // the simplest mapping for plain JDBC).
        String createPlayers = """
            CREATE TABLE IF NOT EXISTS players (
                id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                name            VARCHAR(100) NOT NULL,
                age             INT NOT NULL,
                email           VARCHAR(150),
                nationality     VARCHAR(100),
                position        VARCHAR(10) NOT NULL,
                status          VARCHAR(20) DEFAULT 'active',
                rating          DOUBLE DEFAULT 0,
                market_value    DOUBLE DEFAULT 0,
                goals           INT DEFAULT 0,
                assists         INT DEFAULT 0,
                minutes_played  INT DEFAULT 0,
                yellow_cards    INT DEFAULT 0,
                red_cards       INT DEFAULT 0,
                team_id         BIGINT,
                FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL
            )
            """;

        // Tracks market value over time for the sparkline / trend
        String createMvHistory = """
            CREATE TABLE IF NOT EXISTS market_value_history (
                id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_id     BIGINT NOT NULL,
                market_value  DOUBLE NOT NULL,
                recorded_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
            )
            """;

        String createCoaches = """
            CREATE TABLE IF NOT EXISTS coaches (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                name        VARCHAR(100) NOT NULL,
                age         INT,
                email       VARCHAR(150),
                nationality VARCHAR(100),
                specialty   VARCHAR(100),
                license     VARCHAR(100),
                years_exp   INT DEFAULT 0,
                team_id     BIGINT,
                FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL
            )
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createLeagues);
            stmt.execute(createTeams);
            stmt.execute(createContracts);
            stmt.execute(createPlayers);
            stmt.execute(createMvHistory);
            stmt.execute(createCoaches);

            seedIfEmpty(conn);

            System.out.println("Database ready: playermgs.mv.db");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    /** Insert a few starter rows only if the leagues table is empty. */
    private static void seedIfEmpty(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM leagues")) {
            rs.next();
            if (rs.getInt(1) > 0) return;   // already seeded
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                INSERT INTO leagues (name, country, season, division)
                VALUES ('Premier League', 'England', '2025/26', 1)
                """);
            stmt.execute("""
                INSERT INTO teams (name, city, stadium, budget, league_id)
                VALUES ('FC United', 'Manchester', 'Old Field', 150000000, 1)
                """);
            stmt.execute("""
                INSERT INTO teams (name, city, stadium, budget, league_id)
                VALUES ('City FC', 'Manchester', 'City Arena', 200000000, 1)
                """);
            stmt.execute("""
                INSERT INTO players
                    (name, age, email, nationality, position, status,
                     rating, market_value, goals, assists, minutes_played,
                     yellow_cards, red_cards, team_id)
                VALUES
                    ('Marcus Silva', 24, 'marcus@fc.com', 'Brazil', 'FWD', 'active',
                     8.7, 42.0, 18, 9, 2700, 2, 0, 1)
                """);
            stmt.execute("""
                INSERT INTO players
                    (name, age, email, nationality, position, status,
                     rating, market_value, goals, assists, minutes_played,
                     yellow_cards, red_cards, team_id)
                VALUES
                    ('Karim Aït', 22, 'karim@city.com', 'Morocco', 'FWD', 'active',
                     9.1, 65.0, 25, 11, 3060, 1, 0, 2)
                """);
        }
        System.out.println("Seeded demo data into empty database.");
    }
}
