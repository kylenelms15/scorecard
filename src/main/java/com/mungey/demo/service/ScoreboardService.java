package com.mungey.demo.service;

import com.google.gson.Gson;
import com.mungey.demo.model.Game;
import com.mungey.demo.model.GameId;
import com.mungey.demo.model.GameResult;
import com.mungey.demo.model.Scoreboard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScoreboardService {

    @Autowired
    private RestTemplate restTemplate;

    public List<Scoreboard> getScoreboard(LocalDate date) {
        GameId gameId = getGameIds(date);

        List<String> gameIds = new ArrayList<>();
        gameId.dates.getFirst().games.forEach(game -> {
            gameIds.add(game.gamePk);
        });

        List<Scoreboard> scoreboards = new ArrayList<>();

        gameIds.forEach(game -> {
            scoreboards.add(getGameResults(game));
        });

        return scoreboards;
    }

    private GameId getGameIds(LocalDate date) {

        String apiUrl = "https://statsapi.mlb.com/api/v1/schedule?sportId=1&startDate=" +
                date + "&endDate=" + date + "&fields=dates,games,gamePk";

        Gson gson = new Gson();

        return gson.fromJson(restTemplate.getForObject(apiUrl, String.class), GameId.class);
    }

    private Scoreboard getGameResults(String gameId) {
        String apiUrl = "https://statsapi.mlb.com/api/v1/game/" + gameId + "/contextMetrics";
        Gson gson = new Gson();
        GameResult result = gson.fromJson(restTemplate.getForObject(apiUrl, String.class), GameResult.class);

        Scoreboard scoreboard = new Scoreboard();

        scoreboard.gameId = result.getGame().getGamePk();
        scoreboard.homeTeam = result.getGame().getTeams().getHome().getTeam().getName();
        scoreboard.homeScore = result.getGame().getTeams().getHome().getScore();
        scoreboard.awayTeam = result.getGame().getTeams().getAway().getTeam().getName();
        scoreboard.awayScore = result.getGame().getTeams().getAway().getScore();

        return scoreboard;
    }
}
