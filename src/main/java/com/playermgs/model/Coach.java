package com.playermgs.model;

/**
 * Relationship ① INHERITANCE  — Coach IS-A Person.
 * Relationship ④ ASSOCIATION  — Coach has a reference to a Team,
 *                                but neither owns the other. Both can
 *                                exist independently.
 */
public class Coach extends Person {

    private String specialty;   // e.g. "Defensive", "Attacking", "Fitness"
    private String license;     // e.g. "UEFA Pro", "UEFA A"
    private int    yearsExp;
    private Long   teamId;      // Association: just a reference, not ownership

    public Coach() {}

    public Coach(Long id, String name, int age, String email, String nationality,
                 String specialty, String license, int yearsExp, Long teamId) {
        super(id, name, age, email, nationality);
        this.specialty = specialty;
        this.license   = license;
        this.yearsExp  = yearsExp;
        this.teamId    = teamId;
    }

    /** Assign this coach to a team (Association — just storing the id). */
    public void assignToTeam(Long teamId)   { this.teamId = teamId; }

    /** Remove this coach from their team. */
    public void leaveTeam()                 { this.teamId = null; }

    public boolean hasTeam()                { return teamId != null; }

    // ── Getters & Setters ──────────────────────────────────
    public String getSpecialty()                  { return specialty; }
    public void   setSpecialty(String specialty)  { this.specialty = specialty; }

    public String getLicense()                { return license; }
    public void   setLicense(String license)  { this.license = license; }

    public int  getYearsExp()               { return yearsExp; }
    public void setYearsExp(int yearsExp)   { this.yearsExp = yearsExp; }

    public Long getTeamId()                 { return teamId; }
    public void setTeamId(Long teamId)      { this.teamId = teamId; }

    @Override
    public String toString() {
        return "Coach{id=" + getId() + ", name='" + getName()
            + "', license='" + license + "', teamId=" + teamId + "}";
    }
}
