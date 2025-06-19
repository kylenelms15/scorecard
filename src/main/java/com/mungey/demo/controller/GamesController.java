package com.mungey.demo.controller;

import com.mungey.demo.model.GameId;
import com.mungey.demo.model.Scoreboard;
import com.mungey.demo.service.ScoreboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(path="/games")
public class GamesController {

    @Autowired
    private ScoreboardService scoreboardService;

    @GetMapping(path="/{date}")
    public @ResponseBody ResponseEntity<List<Scoreboard>> getGames(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        return ResponseEntity.ok(scoreboardService.getScoreboard(date));
    }
}
