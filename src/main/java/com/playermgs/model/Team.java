package com.playermgs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Relationship ③ COMPOSITION  — Team owns its Contracts.
 *                                Contracts die when the Team is deleted.
 * Relationship ② AGGREGATION  — Team belongs to a League, but can exist
 *                                without one (leagueId may be null).
 */
public class Team {

    private Long   id;
    private String name;
    private String city;
    private String stadium;
    private double budget;          // transfer budget in USD
    private Long   leagueId;       // Aggregation: nullable foreign key

    /**
     * COMPOSITION: contracts list is created with the Team and
     * cannot exist independently. We never expose the raw list.
     */
    private final List<Contract> contracts = new ArrayList<>();

    public Team() {}

    public Team(Long id, String name, String city, String stadium,
                double budget, Long leagueId) {
        this.id       = id;
        this.name     = name;
        this.city     = city;
        this.stadium  = stadium;
        this.budget   = budget;
        this.leagueId = leagueId;
    }

    // ── Contract management (Composition behaviour) ────────
    public void addContract(Contract c) {
        c.setTeamId(this.id);       // enforce ownership
        contracts.add(c);
    }

    public boolean removeContract(Long contractId) {
        return contracts.removeIf(c -> c.getId().equals(contractId));
    }

    /** Returns an unmodifiable view — callers cannot bypass addContract(). */
    public List<Contract> getContracts() {
        return Collections.unmodifiableList(contracts);
    }

    public int activeContractCount() {
        return (int) contracts.stream().filter(Contract::isActive).count();
    }

    // ── Getters & Setters ──────────────────────────────────
    public Long   getId()                { return id; }
    public void   setId(Long id)         { this.id = id; }

    public String getName()              { return name; }
    public void   setName(String name)   { this.name = name; }

    public String getCity()              { return city; }
    public void   setCity(String city)   { this.city = city; }

    public String getStadium()                { return stadium; }
    public void   setStadium(String stadium)  { this.stadium = stadium; }

    public double getBudget()               { return budget; }
    public void   setBudget(double budget)  { this.budget = budget; }

    public Long   getLeagueId()               { return leagueId; }
    public void   setLeagueId(Long leagueId)  { this.leagueId = leagueId; }

    @Override
    public String toString() {
        return "Team{id=" + id + ", name='" + name + "', city='" + city
            + "', contracts=" + contracts.size() + "}";
    }
}
