package com.mungey.demo.controller;

import com.mungey.demo.model.context.Pitcher;
import com.mungey.demo.model.plays.ScoreCardResponse;
import com.mungey.demo.service.ScoreCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path="/scorecard")
public class ScoreCardController {

    @Autowired
    private ScoreCardService scoreCardService;

    @GetMapping(path="/{gameId}")
    public @ResponseBody ResponseEntity<ScoreCardResponse> getScoreCard(@PathVariable String gameId) {

        return ResponseEntity.ok(scoreCardService.buildScoreCard(gameId));
    }

    @GetMapping(path="/test/{gameId}")
    public @ResponseBody ResponseEntity<List<Pitcher>> boxscoreTest(@PathVariable String gameId) {

        return ResponseEntity.ok(scoreCardService.boxscoreTest(gameId));
    }
}
