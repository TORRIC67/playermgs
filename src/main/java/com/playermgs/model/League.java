package com.playermgs.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Relationship ② AGGREGATION
 * A League has many Teams, but teams can exist without a league.
 * If a league is deleted, the teams survive (leagueId is just set to null).
 */
public class League {

    private Long   id;
    private String name;
    private String country;
    private String season;       // e.g. "2025/26"
    private int    division;     // 1 = top flight, 2 = second division, etc.

    /**
     * AGGREGATION: we store teamIds, not the Team objects themselves.
     * Teams exist independently — this list is just a reference.
     */
    private List<Long> teamIds = new ArrayList<>();

    public League() {}

    public League(Long id, String name, String country, String season, int division) {
        this.id       = id;
        this.name     = name;
        this.country  = country;
        this.season   = season;
        this.division = division;
    }

    // ── Aggregation helpers ────────────────────────────────
    public void addTeam(Long teamId)    { if (!teamIds.contains(teamId)) teamIds.add(teamId); }
    public void removeTeam(Long teamId) { teamIds.remove(teamId); }
    public int  teamCount()             { return teamIds.size(); }

    // ── Getters & Setters ──────────────────────────────────
    public Long   getId()                { return id; }
    public void   setId(Long id)         { this.id = id; }

    public String getName()              { return name; }
    public void   setName(String name)   { this.name = name; }

    public String getCountry()                { return country; }
    public void   setCountry(String country)  { this.country = country; }

    public String getSeason()               { return season; }
    public void   setSeason(String season)  { this.season = season; }

    public int  getDivision()               { return division; }
    public void setDivision(int division)   { this.division = division; }

    public List<Long> getTeamIds()                  { return teamIds; }
    public void       setTeamIds(List<Long> teamIds){ this.teamIds = teamIds; }

    @Override
    public String toString() {
        return "League{id=" + id + ", name='" + name + "', season='" + season
            + "', teams=" + teamIds.size() + "}";
    }
}
