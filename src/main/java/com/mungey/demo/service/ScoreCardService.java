package com.mungey.demo.service;

import com.google.gson.Gson;
import com.mungey.demo.model.context.BattersFaced;
import com.mungey.demo.model.context.BoxscoreContext;
import com.mungey.demo.model.plays.HalfInning;
import com.mungey.demo.model.plays.PlayResponse;
import com.mungey.demo.model.plays.ScoreCardResponse;
import com.mungey.demo.model.plays.AllPlays;
import com.mungey.demo.model.plays.Plays;
import com.mungey.demo.model.scoreboard.OfficalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScoreCardService {

    @Autowired
    private RestTemplate restTemplate;

    public ScoreCardResponse buildScoreCard(String gameId) {
        ScoreCardResponse scoreCardResponse = new ScoreCardResponse();

//        List<Plays> playsList = getPlays(gameId).getAllPlays();
//
//        int finalInning = getLastInning(playsList);
//        scoreCardResponse.setInnings(buildPlays(finalInning, playsList));

        BoxscoreContext boxscoreContext = buildGameContext(gameId);
        buildOfficals(scoreCardResponse, boxscoreContext);
        buildExtraContext(scoreCardResponse, boxscoreContext);

        return scoreCardResponse;
    }

    public List<BattersFaced> boxscoreTest(String gameId) {

        return buildBattersFaced("Abbott, A 25; Rogers, Ta 3; Richardson 1; Pagán 3; Mikolas 20; Leahy 7; Graceffo 6.");
    }

    private AllPlays getPlays(String gameId) {

        String apiUrl = "https://statsapi.mlb.com/api/v1/game/" + gameId + "/playByPlay";

        Gson gson = new Gson();

        return gson.fromJson(restTemplate.getForObject(apiUrl, String.class), AllPlays.class);
    }

    private List<HalfInning> buildInning(List<Plays> allPlays, int inning) {
        List<HalfInning> innings = new ArrayList<>();

        HalfInning topfirst = new HalfInning();
        topfirst.setInning(inning);

        HalfInning bottomfirst = new HalfInning();
        bottomfirst.setInning(inning+ .5);

        List<PlayResponse> topPlays = new ArrayList<>();
        List<PlayResponse> bottomPlays = new ArrayList<>();

        allPlays.forEach(play -> {
            PlayResponse inningPlay = new PlayResponse();
            inningPlay.setDescription(play.getResult().getDescription());

            if(play.getAbout().isTopInning() && play.getAbout().getInning() == inning) {
                topPlays.add(inningPlay);
            }
            else if(!play.getAbout().isTopInning() && play.getAbout().getInning() == inning){
                bottomPlays.add(inningPlay);
            }
        });

        topfirst.setPlays(topPlays);
        bottomfirst.setPlays(bottomPlays);

        innings.add(topfirst);
        innings.add(bottomfirst);

        return innings;
    }

    private int getLastInning(List<Plays> plays) {
        return plays.getLast().getAbout().getInning();
    }

    private List<HalfInning> buildPlays(int finalInning, List<Plays> playsList) {
        List<HalfInning> innings = new ArrayList<>();

        for(int i = 1; i <= finalInning; i++){
            innings.addAll(buildInning(playsList, i));
        }

        return innings;
    }

    private BoxscoreContext buildGameContext(String gameId) {

        String apiUrl = "https://statsapi.mlb.com/api/v1/game/" + gameId + "/boxscore";

        Gson gson = new Gson();

        return gson.fromJson(restTemplate.getForObject(apiUrl, String.class), BoxscoreContext.class);
    }

    private ScoreCardResponse buildOfficals(ScoreCardResponse scoreCard, BoxscoreContext boxscoreContext) {
        List<OfficalResponse> officals = new ArrayList<>();

        boxscoreContext.getOfficials().forEach(contextOffical -> {
            OfficalResponse officalResponse = new OfficalResponse();

            officalResponse.setName(contextOffical.getOfficial().getFullName());
            officalResponse.setPosition(contextOffical.getOfficialType());

            officals.add(officalResponse);
        });

        scoreCard.setOfficals(officals);
        return scoreCard;
    }

    private ScoreCardResponse buildExtraContext(ScoreCardResponse scoreCard, BoxscoreContext boxscoreContext) {
        boxscoreContext.getInfo().forEach(contextInfo -> {
            switch(contextInfo.getLabel()) {
                case "Pitches-strikes":
                    scoreCard.setPitchesStrikes(contextInfo.getValue());
                    break;
                case "Batters faced":
                    scoreCard.setBattersFaced(buildBattersFaced(contextInfo.getValue()));
                    break;
                case "Weather":
                    scoreCard.setWeather(contextInfo.getValue());
                    break;
                case "First pitch":
                    scoreCard.setFirstPitch(contextInfo.getValue());
                    break;
                case "T":
                    scoreCard.setGameTime(contextInfo.getValue());
                    break;
                case "Att":
                    scoreCard.setAttendance(contextInfo.getValue());
                    break;
            }
        });

        return scoreCard;
    }

    private List<BattersFaced> buildBattersFaced(String battersString) {
        List<BattersFaced> battersFaced = new ArrayList<>();

        battersString = StringUtils.substring(battersString, 0, battersString.length() - 1);

        String[] full = battersString.split(";");

        for(int i=0;i< full.length; i++) {
            BattersFaced faced = new BattersFaced();
            String facedString = full[i];

            int numBatters = Integer.parseInt(facedString.replaceAll("[^0-9]", ""));
            faced.setBatters(numBatters);

            String pitcher = facedString.substring(0, facedString.indexOf(String.valueOf(numBatters)));

            faced.setPitcher(pitcher.trim());

            battersFaced.add(faced);
        }

        return battersFaced;
    }
}
