package com.playermgs.model;

import java.time.LocalDate;

/**
 * Relationship ③ COMPOSITION
 * A Contract cannot exist without a Team. It is created and
 * destroyed with the Team that owns it.
 */
public class Contract {

    private Long      id;
    private double    salaryPerYear;   // in USD
    private LocalDate startDate;
    private LocalDate endDate;
    private String    status;          // "ACTIVE" | "EXPIRED" | "PENDING"

    // Back-reference to owning Team (set by Team, never standalone)
    private Long teamId;

    public Contract() {}

    public Contract(Long id, double salaryPerYear,
                    LocalDate startDate, LocalDate endDate,
                    String status, Long teamId) {
        this.id             = id;
        this.salaryPerYear  = salaryPerYear;
        this.startDate      = startDate;
        this.endDate        = endDate;
        this.status         = status;
        this.teamId         = teamId;
    }

    /** Convenience: is this contract currently valid? */
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return startDate != null && endDate != null
            && !today.isBefore(startDate)
            && !today.isAfter(endDate)
            && "ACTIVE".equalsIgnoreCase(status);
    }

    /** Years remaining on contract */
    public long yearsRemaining() {
        if (endDate == null) return 0;
        long diff = endDate.getYear() - LocalDate.now().getYear();
        return Math.max(0, diff);
    }

    // ── Getters & Setters ──────────────────────────────────
    public Long      getId()                    { return id; }
    public void      setId(Long id)             { this.id = id; }

    public double    getSalaryPerYear()                      { return salaryPerYear; }
    public void      setSalaryPerYear(double salaryPerYear)  { this.salaryPerYear = salaryPerYear; }

    public LocalDate getStartDate()                   { return startDate; }
    public void      setStartDate(LocalDate startDate){ this.startDate = startDate; }

    public LocalDate getEndDate()                  { return endDate; }
    public void      setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String    getStatus()                { return status; }
    public void      setStatus(String status)   { this.status = status; }

    public Long      getTeamId()                { return teamId; }
    public void      setTeamId(Long teamId)     { this.teamId = teamId; }

    @Override
    public String toString() {
        return "Contract{id=" + id + ", salary=" + salaryPerYear
            + ", end=" + endDate + ", status='" + status + "'}";
    }
}
