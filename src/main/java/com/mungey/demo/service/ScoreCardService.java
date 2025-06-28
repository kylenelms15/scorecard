package com.mungey.demo.service;

import com.google.gson.Gson;
import com.mungey.demo.model.context.Pitcher;
import com.mungey.demo.model.context.BoxscoreContext;
import com.mungey.demo.model.player.Lineup;
import com.mungey.demo.model.player.PlayerLookup;
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

    private String battersFaced;
    private String pitchesStrikes;

    public ScoreCardResponse buildScoreCard(String gameId) {
        ScoreCardResponse scoreCardResponse = new ScoreCardResponse();

        BoxscoreContext boxscoreContext = buildGameContext(gameId);

        buildOfficals(scoreCardResponse, boxscoreContext);
        buildExtraContext(scoreCardResponse, boxscoreContext);
        scoreCardResponse.setPitchers(getPitcherJerseyNumbers(boxscoreContext, scoreCardResponse.getPitchers()));
        scoreCardResponse.setLineup(buildLineUp(boxscoreContext));

        List<Plays> playsList = getPlays(gameId).getAllPlays();
        int finalInning = getLastInning(playsList);
        scoreCardResponse.setInnings(buildPlays(finalInning, playsList, scoreCardResponse.getLineup()));

        return scoreCardResponse;
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

            String playerName = play.getMatchup().getBatter().getFullName();
            inningPlay.setPlayer(playerName);

            String playDescription = play.getResult().getDescription();
            inningPlay.setResult(getPlayResult(playDescription.replace(playerName + " ", "")));

            inningPlay.setDescription(playDescription);

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

    private List<HalfInning> buildPlays(int finalInning, List<Plays> playsList, Lineup lineup) {
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

        scoreCard.setOfficials(officals);
        return scoreCard;
    }

    private ScoreCardResponse buildExtraContext(ScoreCardResponse scoreCard, BoxscoreContext boxscoreContext) {
        boxscoreContext.getInfo().forEach(contextInfo -> {
            switch(contextInfo.getLabel()) {
                case "Pitches-strikes":
                    pitchesStrikes = contextInfo.getValue();
                    break;
                case "Batters faced":
                    battersFaced = contextInfo.getValue();
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

        scoreCard.setPitchers(buildPitchers(battersFaced, pitchesStrikes));

        return scoreCard;
    }

    private List<Pitcher> buildPitchers(String battersString, String strikesString) {
        List<Pitcher> battersFaced = new ArrayList<>();

        battersString = StringUtils.substring(battersString, 0, battersString.length() - 1);
        strikesString = StringUtils.substring(strikesString, 0, strikesString.length() - 1);

        String[] facedArray = battersString.split(";");
        String[] strikesArray = strikesString.split(";");

        for(int i=0;i< facedArray.length; i++) {
            Pitcher pitcher = new Pitcher();

            String facedString = facedArray[i];
            String pitchesString = strikesArray[i];

            String[] pitchesArray = pitchesString.split("-");

            int numBatters = Integer.parseInt(facedString.replaceAll("[^0-9]", ""));
            int numPitches = Integer.parseInt(pitchesArray[0].replaceAll("[^0-9]", ""));
            int numStrikes = Integer.parseInt(pitchesArray[1]);

            pitcher.setBattersFaced(numBatters);
            pitcher.setPitches(numPitches);
            pitcher.setStrikes(numStrikes);
            pitcher.setName(facedString.substring(0, facedString.indexOf(String.valueOf(numBatters))).trim());

            battersFaced.add(pitcher);
        }

        return battersFaced;
    }

    private List<Pitcher> getPitcherJerseyNumbers(BoxscoreContext context, List<Pitcher> pitchers) {

        List<Integer> awayPitcherIds = new ArrayList<>();
        awayPitcherIds.addAll(context.getTeams().getAway().getPitchers());
        awayPitcherIds.addAll(context.getTeams().getAway().getBullpen());

        List<Integer> homePitcherIds = new ArrayList<>();
        homePitcherIds.addAll(context.getTeams().getHome().getPitchers());
        homePitcherIds.addAll(context.getTeams().getHome().getBullpen());

        pitchers = getPitcherJerseyNumbersByTeam(homePitcherIds, pitchers, "home");
        pitchers = getPitcherJerseyNumbersByTeam(awayPitcherIds, pitchers, "away");

        return pitchers;
    }

    private List<Pitcher> getPitcherJerseyNumbersByTeam(List<Integer> pitcherIds, List<Pitcher> pitchers, String team) {

        pitcherIds.forEach(pitcherId -> {
            String apiUrl = "https://statsapi.mlb.com/api/v1/people/" + pitcherId;

            Gson gson = new Gson();

            PlayerLookup player = gson.fromJson(restTemplate.getForObject(apiUrl, String.class), PlayerLookup.class);
            String boxscoreName = player.getPeople().get(0).getBoxscoreName();

            pitchers.stream().filter(o -> o.getName().equals(boxscoreName)).forEach(
                    o -> {
                        o.setJerseyNumber(player.getPeople().get(0).getPrimaryNumber());
                        o.setTeam(team);
                    }
            );
        });

        return pitchers;
    }

    private Lineup buildLineUp(BoxscoreContext context) {
        Lineup lineup = new Lineup();

        List<Integer> awayLineupIds = context.getTeams().getAway().getBattingOrder();
        List<Integer> homeLineupIds = context.getTeams().getHome().getBattingOrder();

        lineup.setAwayLineup(buildLineupByTeam(awayLineupIds));
        lineup.setHomeLineup(buildLineupByTeam(homeLineupIds));

        return lineup;
    }

    private List<String> buildLineupByTeam(List<Integer> lineupIds) {
        List<String> lineup = new ArrayList<>();

        lineupIds.forEach(pitcherId -> {
            String apiUrl = "https://statsapi.mlb.com/api/v1/people/" + pitcherId;

            Gson gson = new Gson();

            PlayerLookup player = gson.fromJson(restTemplate.getForObject(apiUrl, String.class), PlayerLookup.class);
            String boxscoreName = player.getPeople().get(0).getBoxscoreName();
            lineup.add(boxscoreName);
        });

        return lineup;
    }

    private String getPlayResult(String playDescription) {
        playDescription = playDescription.replace(",","");
        playDescription = playDescription.replace(".","");
        playDescription = playDescription.replace("sharply","");

        String[] splitString = playDescription.split(" ");

        if(splitString.length <= 1 && splitString[0].equals("walks")) {
            return "BB";
        }

        if(splitString[1].equals("out")) {
            if(splitString[0].equals("called")) {
                return "Backwards K";
            }
            if(splitString[0].equals("strikes")) {
                return "K";
            }
        }

        return playDescription;
    }
}
