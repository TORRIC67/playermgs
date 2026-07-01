
package com.playermgs.controller;

import com.playermgs.model.League;
import com.playermgs.service.LeagueService;
import java.util.List;

public class LeagueController {

    private final LeagueService service = new LeagueService();

    public List<League> getAll() {
        return service.findAll();
    }

    public League getById(Long id) {
        return service.findById(id)
            .orElseThrow(() -> new RuntimeException("League not found: " + id));
    }

    public League create(League league) {
        return service.create(league);
    }

    public League update(Long leagueId, String name, String country,
                         String season, int division) {
        return service.update(leagueId, name, country, season, division);
    }

    public boolean delete(Long id) {
        return service.delete(id);
    }

    public League addTeam(Long leagueId, Long teamId) {
        return service.addTeam(leagueId, teamId);
    }

    public League removeTeam(Long leagueId, Long teamId) {
        return service.removeTeam(leagueId, teamId);
    }

    public League advanceSeason(Long leagueId, String newSeason) {
        return service.advanceSeason(leagueId, newSeason);
    }
}