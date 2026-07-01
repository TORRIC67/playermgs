package com.playermgs;

import com.google.gson.Gson;
import com.playermgs.controller.ApiRouter;
import com.playermgs.dao.Database;
import com.playermgs.model.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * ─────────────────────────────────────────────────────────────
 *  WebServer — serves BOTH the frontend HTML and the JSON API
 *  on the same port (8080), so there are no CORS issues.
 * ─────────────────────────────────────────────────────────────
 *
 *  Routes:
 *    GET  /                          → serves player-management.html
 *    GET  /api/players               → list all players
 *    GET  /api/players/{id}          → get one player
 *    POST /api/players/{id}/transfer → transfer player to new team
 *    PUT  /api/players/{id}/market-value → update market value
 *    GET  /api/teams                 → list all teams
 *    GET  /api/leagues               → list all leagues
 *    PUT  /api/leagues/{id}          → edit a league
 *
 *  Run with:
 *    java -cp "out:lib/gson-2.10.1.jar" com.playermgs.WebServer
 *  (on Windows use ; instead of : )
 * ─────────────────────────────────────────────────────────────
 */
public class WebServer {

    private static final Gson      gson = new Gson();
    private static final ApiRouter api  = ApiRouter.getInstance();
    private static final int       PORT = 8081;

    public static void main(String[] args) throws IOException {

        Database.initSchema();   // creates H2 tables + seeds demo rows on first run

        int port = System.getenv("PORT") != null
                ? Integer.parseInt(System.getenv("PORT"))
                : PORT;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // ── Serve the frontend ──────────────────────────────
        server.createContext("/", new StaticFileHandler());

        // ── API routes ──────────────────────────────────────
        server.createContext("/api/players", new PlayersHandler());
        server.createContext("/api/teams",   new TeamsHandler());
        server.createContext("/api/leagues", new LeaguesHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("═══════════════════════════════════════════");
        System.out.println("  PlayerMS server running");
        System.out.println("  Open: http://localhost:" + port);
        System.out.println("═══════════════════════════════════════════");
    }

    // ── Serve player-management.html and any static assets ──
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/player-management.html";

            // Look inside resources folder next to compiled classes
            InputStream in = WebServer.class.getResourceAsStream(path);
            if (in == null) {
                respond(exchange, 404, "text/plain", "404 Not Found: " + path);
                return;
            }

            byte[] bytes = in.readAllBytes();
            String contentType = path.endsWith(".html") ? "text/html"
                               : path.endsWith(".css")  ? "text/css"
                               : path.endsWith(".js")   ? "application/javascript"
                               : "application/octet-stream";

            exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // ── /api/players ─────────────────────────────────────────
    static class PlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            String method = exchange.getRequestMethod();
            String path   = exchange.getRequestURI().getPath();   // e.g. /api/players/3/transfer
            String[] parts = path.split("/");                     // ["", "api", "players", "3", "transfer"]

            try {
                if (method.equals("OPTIONS")) { respond(exchange, 204, "text/plain", ""); return; }

                if (method.equals("GET") && parts.length == 3) {
                    // GET /api/players
                    respondJson(exchange, 200, api.players.getAll());
                    return;
                }

                if (method.equals("GET") && parts.length == 4) {
                    // GET /api/players/{id}
                    Long id = Long.parseLong(parts[3]);
                    respondJson(exchange, 200, api.players.getById(id));
                    return;
                }

                if (method.equals("POST") && parts.length == 5 && parts[4].equals("transfer")) {
                    // POST /api/players/{id}/transfer   body: {"teamId": 2}
                    Long id = Long.parseLong(parts[3]);
                    Map<?,?> body = gson.fromJson(readBody(exchange), Map.class);
                    Long teamId = Double.valueOf((Double) body.get("teamId")).longValue();
                    Player result = api.players.transfer(id, teamId);
                    respondJson(exchange, 200, result);
                    return;
                }

                if (method.equals("PUT") && parts.length == 5 && parts[4].equals("market-value")) {
                    // PUT /api/players/{id}/market-value   body: {"value": 45.5}
                    Long id = Long.parseLong(parts[3]);
                    Map<?,?> body = gson.fromJson(readBody(exchange), Map.class);
                    double value = (Double) body.get("value");
                    Player result = api.players.updateMarketValue(id, value);
                    respondJson(exchange, 200, result);
                    return;
                }

                respond(exchange, 404, "text/plain", "Route not found");

            } catch (Exception e) {
                respondJson(exchange, 400, Map.of("error", e.getMessage()));
            }
        }
    }

    // ── /api/teams ───────────────────────────────────────────
    static class TeamsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            String method = exchange.getRequestMethod();
            String[] parts = exchange.getRequestURI().getPath().split("/");

            try {
                if (method.equals("OPTIONS")) { respond(exchange, 204, "text/plain", ""); return; }

                if (method.equals("GET") && parts.length == 3) {
                    respondJson(exchange, 200, api.teams.getAll());
                    return;
                }
                if (method.equals("GET") && parts.length == 4) {
                    Long id = Long.parseLong(parts[3]);
                    respondJson(exchange, 200, api.teams.getById(id));
                    return;
                }
                respond(exchange, 404, "text/plain", "Route not found");

            } catch (Exception e) {
                respondJson(exchange, 400, Map.of("error", e.getMessage()));
            }
        }
    }

    // ── /api/leagues ─────────────────────────────────────────
    static class LeaguesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            String method = exchange.getRequestMethod();
            String[] parts = exchange.getRequestURI().getPath().split("/");

            try {
                if (method.equals("OPTIONS")) { respond(exchange, 204, "text/plain", ""); return; }

                if (method.equals("GET") && parts.length == 3) {
                    respondJson(exchange, 200, api.leagues.getAll());
                    return;
                }

                if (method.equals("PUT") && parts.length == 4) {
                    // PUT /api/leagues/{id}   body: {"name":"...","country":"...","season":"...","division":1}
                    Long id = Long.parseLong(parts[3]);
                    Map<?,?> body = gson.fromJson(readBody(exchange), Map.class);
                    String name     = (String) body.get("name");
                    String country  = (String) body.get("country");
                    String season   = (String) body.get("season");
                    int division    = body.get("division") != null
                                     ? ((Double) body.get("division")).intValue() : 0;
                    League result = api.leagues.update(id, name, country, season, division);
                    respondJson(exchange, 200, result);
                    return;
                }

                respond(exchange, 404, "text/plain", "Route not found");

            } catch (Exception e) {
                respondJson(exchange, 400, Map.of("error", e.getMessage()));
            }
        }
    }

    // ── Shared helpers ───────────────────────────────────────
    private static void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respondJson(HttpExchange exchange, int status, Object data) throws IOException {
        String json = gson.toJson(data);
        respond(exchange, status, "application/json", json);
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

}
