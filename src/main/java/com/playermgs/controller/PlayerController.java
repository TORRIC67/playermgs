package com.playermgs.controller;

import com.playermgs.model.Player;
import com.playermgs.service.PlayerService;
import java.util.List;

public class PlayerController {

    private final PlayerService service = new PlayerService();

    public List<Player> getAll() {
        return service.findAll();
    }

    public Player getById(Long id) {
        return service.findById(id)
            .orElseThrow(() -> new RuntimeException("Player not found: " + id));
    }

    public List<Player> getByTeam(Long teamId) {
        return service.findByTeam(teamId);
    }

    public List<Player> getByPosition(String position) {
        return service.findByPosition(position);
    }

    public List<Player> getByStatus(String status) {
        return service.findByStatus(status);
    }

    public Player create(Player player) {
        return service.create(player);
    }

    public Player update(Player player) {
        return service.update(player);
    }

    public boolean delete(Long id) {
        return service.delete(id);
    }

    public Player transfer(Long playerId, Long newTeamId) {
        return service.transfer(playerId, newTeamId);
    }

    public Player updateMarketValue(Long playerId, double valueMillion) {
        return service.setMarketValue(playerId, valueMillion);
    }

    public Player refreshStats(Long playerId, int goals, int assists,
                               int minutes, int yellows, int reds) {
        return service.refreshStats(playerId, goals, assists, minutes, yellows, reds);
    }
}