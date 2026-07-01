package com.playermgs.controller;

import com.playermgs.model.Contract;
import com.playermgs.model.Team;
import com.playermgs.service.TeamService;
import java.util.List;

public class TeamController {

    private final TeamService service = new TeamService();

    public List<Team> getAll() {
        return service.findAll();
    }

    public Team getById(Long id) {
        return service.findById(id)
            .orElseThrow(() -> new RuntimeException("Team not found: " + id));
    }

    public List<Team> getByLeague(Long leagueId) {
        return service.findByLeague(leagueId);
    }

    public Team create(Team team) {
        return service.create(team);
    }

    public Team update(Team team) {
        return service.update(team);
    }

    public boolean delete(Long id) {
        return service.delete(id);
    }

    public Team updateBudget(Long teamId, double newBudget) {
        return service.updateBudget(teamId, newBudget);
    }

    public Contract addContract(Long teamId, Contract contract) {
        return service.addContract(teamId, contract);
    }
}