package com.mungey.demo.controller;

import com.mungey.demo.model.plays.AllPlays;
import com.mungey.demo.model.plays.Plays;
import com.mungey.demo.service.ScoreCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path="/scorecard")
public class ScoreCardController {

    @Autowired
    private ScoreCardService scoreCardService;

    @GetMapping(path="/{gameId}")
    public @ResponseBody ResponseEntity<AllPlays> getScoreCard(@PathVariable String gameId) {

        return ResponseEntity.ok(scoreCardService.buildScoreCard(gameId));
    }
}
