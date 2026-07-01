package com.playermgs.controller;

public class ApiRouter {

    private static final ApiRouter INSTANCE = new ApiRouter();

    public final PlayerController  players = new PlayerController();
    public final TeamController    teams   = new TeamController();
    public final LeagueController  leagues = new LeagueController();

    private ApiRouter() { }

    public static ApiRouter getInstance() {
        return INSTANCE;
    }
}