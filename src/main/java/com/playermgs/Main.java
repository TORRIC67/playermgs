package com.playermgs;

import com.playermgs.controller.ApiRouter;
import com.playermgs.dao.Database;
import com.playermgs.model.*;
import com.playermgs.service.StatsService;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────
 *  PlayerMS — Main entry point & live demo
 * ─────────────────────────────────────────────────────────────
 *  Demonstrates all 5 relationships in action:
 *   ① Inheritance   Player / Coach  extends  Person
 *   ② Aggregation   League  has     Teams    (teams survive league deletion)
 *   ③ Composition   Team    owns    Contracts (contracts die with team)
 *   ④ Association   Player / Coach  reference Team
 *   ⑤ Dependency    Player  uses    StatsService (method-scoped only)
 *
 *  Now backed by a real H2 database via JDBC — data persists
 *  between runs in playermgs.mv.db.
 * ─────────────────────────────────────────────────────────────
 */
public final class Main {

    private static final ApiRouter api = ApiRouter.getInstance();

    private static final String ENGLAND   = "England";
    private static final String SEASON_25 = "2025/26";
    private static final String CREATED   = "Created: ";

    private Main() { }   // utility/demo entry point — never instantiated

    public static void main(String[] args) {

        Database.initSchema();   // creates tables + seeds demo rows on first run

        banner("PlayerMS — Backend Demo");

        // ── 1. Leagues ─────────────────────────────────────
        section("1. Create Leagues  [Aggregation ②]");

        League premierLeague = api.leagues.create(
            new League(null, "Premier League", ENGLAND, SEASON_25, 1));
        League divOne = api.leagues.create(
            new League(null, "Division 1",     ENGLAND, SEASON_25, 2));

        print(CREATED + premierLeague);
        print(CREATED + divOne);

        // ── 2. Edit league ─────────────────────────────────
        section("2. Edit League  [LeagueService.update()]");

        League updatedLeague = api.leagues.update(
            premierLeague.getId(),
            "English Premier League", ENGLAND, SEASON_25, 1);
        print("Updated: " + updatedLeague);

        // ── 3. Teams ───────────────────────────────────────
        section("3. Create Teams  [Aggregation ②]");

        Team fcUnited = api.teams.create(
            new Team(null, "FC United",  "Manchester", "Old Field",   150_000_000.0, premierLeague.getId()));
        Team cityFc = api.teams.create(
            new Team(null, "City FC",    "Manchester", "City Arena",  200_000_000.0, premierLeague.getId()));
        Team roversFc = api.teams.create(
            new Team(null, "Rovers SC",  "Leeds",      "Rovers Park",  80_000_000.0, divOne.getId()));

        // Link teams to leagues
        api.leagues.addTeam(premierLeague.getId(), fcUnited.getId());
        api.leagues.addTeam(premierLeague.getId(), cityFc.getId());
        api.leagues.addTeam(divOne.getId(),        roversFc.getId());

        print(CREATED + fcUnited);
        print(CREATED + cityFc);
        print(CREATED + roversFc);

        // ── 4. Contracts (Composition ③) ───────────────────
        section("4. Add Contracts  [Composition ③]  — die with Team");

        Contract c1 = new Contract(null, 85_000.0,
            LocalDate.of(2023, Month.JULY, 1), LocalDate.of(2026, Month.JUNE, 30), "ACTIVE", fcUnited.getId());
        Contract c2 = new Contract(null, 110_000.0,
            LocalDate.of(2024, Month.JANUARY, 1), LocalDate.of(2028, Month.JUNE, 30), "ACTIVE", roversFc.getId());

        fcUnited.addContract(c1);
        roversFc.addContract(c2);

        print("FC United contracts: " + fcUnited.getContracts().size());
        print("Rovers SC contracts: " + roversFc.getContracts().size());
        print("FC United active:    " + fcUnited.activeContractCount());

        // ── 5. Coaches (Inheritance ① + Association ④) ─────
        section("5. Create Coaches  [Inheritance ① + Association ④]");

        Coach headCoach = new Coach(null, "Tom Harris", 48, "tom@fc.com", ENGLAND,
            "Defensive", "UEFA Pro", 20, fcUnited.getId());

        print("Coach (Person): " + headCoach.getName() + ", Nationality: " + headCoach.getNationality());
        print("Coach specialty: " + headCoach.getSpecialty() + ", License: " + headCoach.getLicense());
        print("Coach has team: " + headCoach.hasTeam() + " → teamId=" + headCoach.getTeamId());

        // ── 6. Players (Inheritance ① + Association ④) ─────
        section("6. Create Players  [Inheritance ① + Association ④]");

        Player marcus = new Player(null, "Marcus Silva", 24, "marcus@fc.com",
            "Brazil", "FWD", "active", 8.7, 42.0, fcUnited.getId());
        marcus.setGoals(18); marcus.setAssists(9);
        marcus.setMinutesPlayed(2700); marcus.setYellowCards(2); marcus.setRedCards(0);

        Player karim = new Player(null, "Karim Aït", 22, "karim@rovers.com",
            "Morocco", "FWD", "active", 9.1, 65.0, roversFc.getId());
        karim.setGoals(25); karim.setAssists(11);
        karim.setMinutesPlayed(3060); karim.setYellowCards(1); karim.setRedCards(0);

        Player lena = new Player(null, "Lena Müller", 31, "lena@fc.com",
            "Germany", "DEF", "injured", 6.9, 11.0, fcUnited.getId());

        Player marcusSaved = api.players.create(marcus);
        Player karimSaved  = api.players.create(karim);
        Player lenaSaved   = api.players.create(lena);

        print(CREATED + marcusSaved);
        print(CREATED + karimSaved);
        print(CREATED + lenaSaved);

        // Inheritance ①: Player IS-A Person — access Person fields directly
        print("\n[Inheritance ①] Player uses Person fields:");
        print("  marcus.getName()        = " + marcusSaved.getName());
        print("  marcus.getAge()         = " + marcusSaved.getAge());
        print("  marcus.getNationality() = " + marcusSaved.getNationality());
        print("  marcus.getPosition()    = " + marcusSaved.getPosition());  // Player-only

        // ── 7. StatsService (Dependency ⑤) ─────────────────
        section("7. Stats Calculation  [Dependency ⑤]");

        print("[Dependency ⑤] StatsService passed as method param — never stored in Player:");
        Player refreshed = api.players.refreshStats(
            marcusSaved.getId(), 20, 10, 2880, 3, 0);
        print("  Marcus rating after refresh: " + refreshed.getRating());
        print("  Marcus market value:         $" + refreshed.getMarketValue() + "M");

        // Show Dependency: StatsService is used directly too (not stored)
        StatsService stats = new StatsService();
        double rating = stats.calculateRating(20, 10, 2880, 3, 0);
        double mv     = stats.estimateMarketValue(rating, 24, "FWD");
        print("  Direct StatsService call → rating=" + rating + ", mv=$" + mv + "M");

        // ── 8. Transfer market ─────────────────────────────
        section("8. Transfer Market  [Association ④]");

        print("Karim's team before transfer: teamId=" + karimSaved.getTeamId());
        print("City FC budget before:        $" + (cityFc.getBudget() / 1_000_000) + "M");

        Player transferred = api.players.transfer(karimSaved.getId(), cityFc.getId());
        Team cityAfter = api.teams.getById(cityFc.getId());

        print("Karim's team after transfer:  teamId=" + transferred.getTeamId());
        print("City FC budget after:         $" + (cityAfter.getBudget() / 1_000_000) + "M");
        print("Karim new market value:       $" + transferred.getMarketValue() + "M");

        // ── 9. Update market value directly ────────────────
        section("9. Update Market Value directly");

        Player mvUpdated = api.players.updateMarketValue(lenaSaved.getId(), 13.5);
        print("Lena old MV: $11.0M  →  new MV: $" + mvUpdated.getMarketValue() + "M");

        // ── 10. Edit League + Season advance ───────────────
        section("10. Edit League & Advance Season");

        League nextSeason = api.leagues.advanceSeason(premierLeague.getId(), "2026/27");
        print("Premier League season now: " + nextSeason.getSeason());

        League renamed = api.leagues.update(
            divOne.getId(), "Championship", ENGLAND, "2026/27", 2);
        print("Division 1 renamed to: " + renamed.getName());

        // ── 11. Aggregation demo ────────────────────────────
        section("11. Delete League  [Aggregation ②]  — Teams survive");

        print("Teams in Div 1 before delete: " + api.teams.getByLeague(divOne.getId()).size());
        api.leagues.delete(divOne.getId());
        Team roversAfter = api.teams.getById(roversFc.getId());
        print("Rovers SC still exists:  " + (roversAfter != null));
        print("Rovers SC leagueId now:  " + roversAfter.getLeagueId() + "  ← null (freed)");

        // ── 12. Summary ────────────────────────────────────
        section("12. Final state");

        List<Player> allPlayers = api.players.getAll();
        print("Total players in system: " + allPlayers.size());
        allPlayers.forEach(p ->
            print("  " + p.getName() + " | " + p.getPosition()
                + " | rating=" + p.getRating()
                + " | $" + p.getMarketValue() + "M"
                + " | teamId=" + p.getTeamId()));

        banner("Demo complete");
    }

    // ── Helpers ────────────────────────────────────────────
    private static void banner(String title) {
        String line = "═".repeat(60);
        System.out.println("\n" + line);
        System.out.println("  " + title);
        System.out.println(line);
    }
    private static void section(String title) {
        System.out.println("\n── " + title + " " + "─".repeat(Math.max(0, 54 - title.length())));
    }
    private static void print(String msg) {
        System.out.println("  " + msg);
    }
}
