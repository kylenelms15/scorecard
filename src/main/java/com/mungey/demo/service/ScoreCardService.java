package com.mungey.demo.service;

import com.google.gson.Gson;
import com.mungey.demo.model.GameId;
import com.mungey.demo.model.plays.AllPlays;
import com.mungey.demo.model.plays.Plays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ScoreCardService {

    @Autowired
    private RestTemplate restTemplate;

    public AllPlays buildScoreCard(String gameId) {


        return getPlays(gameId);
    }

    private AllPlays getPlays(String gameId) {

        String apiUrl = "https://statsapi.mlb.com/api/v1/game/" + gameId + "/playByPlay";

        Gson gson = new Gson();

        return gson.fromJson(restTemplate.getForObject(apiUrl, String.class), AllPlays.class);
    }
}
