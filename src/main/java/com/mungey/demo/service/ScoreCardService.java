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
            inningPlay.setAtBatResult(getPlayResult(playDescription.replace(playerName + " ", "")));

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
        playDescription = playDescription.replace(":","");

        String[] splitString = playDescription.split(" ");
        ArrayList<String> newList = new ArrayList<>();

        for(int i = 0; i<splitString.length; i++){
            if(!splitString[i].equals("sharply") && !splitString[i].equals("softly")) {
                newList.add(splitString[i]);
            }
        }

        if(newList.indexOf("upheld") > 0) {
            newList.subList(0, newList.indexOf("upheld") + 1).clear();
        }

        if(newList.indexOf("overturned") > 0) {
            newList.subList(0, newList.indexOf("overturned") + 1).clear();
        }

        if(newList.get(0).equals("walks")) {
            return "BB";
        }

        if(newList.get(0).equals("singles")) {
            return "1B";
        }

        if(newList.get(0).equals("doubles")) {
            return "2B";
        }

        if(newList.get(0).equals("triples")) {
            return "3B";
        }

        if(newList.get(0).equals("homers")) {
            return "HR";
        }

        if(newList.get(0).equals("hits")) {
            if(newList.indexOf("ground-rule")>0) {
                return "2B GR";
            }
            if(newList.indexOf("grand")>0) {
                return "GSHR";
            }
        }

        if(newList.get(0).equals("hit")) {
            return "HBP";
        }

        if(newList.get(0).equals("out")) {
            int sac = newList.indexOf("sacrifice");
            if(sac > 0 && newList.get(sac + 1).equals("fly")) {
                return "SF " + getFielder(newList.get(newList.indexOf("to")+1));
            }
            if(sac > 0 && newList.get(sac + 1).equals("bunt")) {
                return "SB " + getFielder(newList.get(5)) + "-" + getFielder(newList.get(newList.indexOf("to")+1));
            }
        }

        if(newList.get(1).equals("out")) {
            if(newList.get(0).equals("called")) {
                return "Backwards K";
            }
            if(newList.get(0).equals("strikes")) {
                return "K";
            }
            if(newList.get(0).equals("flies")) {
                return "F" + getFielder(newList.get(3));
            }
            if(newList.get(0).equals("lines")) {
                return "L" + getFielder(newList.get(3));
            }
            if(newList.get(0).equals("pops")) {
                return "P" + getFielder(newList.get(3));
            }
        }

        if(newList.get(0).equals("grounds") || newList.get(0).equals("pops")) {
            String playResult;

            if(newList.get(3).equals("force")) {
                if(newList.indexOf("fielded") > 0) {
                    playResult = getFielder(newList.get(newList.indexOf("by")+1));
                } else {
                    playResult = getFielder(newList.get(5)) + "-" + getFielder(newList.get(newList.indexOf("to")+1));
                }
            } else if(newList.get(3).equals("double")){
                return buildDoublePlay(newList);
            }
            else {
                playResult = getFielder(newList.get(2)) + "-" + getFielder(newList.get(newList.indexOf("to")+1));
            }

            if(playResult.length() == 2) {
                return playResult.charAt(1) + "U";
            }

            if(playResult.length() == 1) {
                return playResult.charAt(0) + "U";
            }

            return playResult;
        }

        if(newList.get(0).equals("lines")) {
            if(newList.get(3).equals("double")){
                return buildDoublePlay(newList);
            }
            if(newList.get(3).equals("unassisted") && newList.get(4).equals("double") ){
                return "DP " + getFielder(newList.get(6)) + "U";
            }
        }

        if(newList.get(3).equals("fielder's")) {
            String playResult = "FC ";
            if(newList.indexOf("by") > 0){
                return playResult + getFielder(newList.get(newList.indexOf("by") + 1));
            }
            return playResult + getFielder(newList.get(6)) + "-" + getFielder(newList.get(newList.indexOf("to")+1));
        }

        if(newList.get(4).equals("error")) {
            return "E" + getFielder(newList.get(newList.indexOf("by")+1));
        }

        return "No At Bat";
    }

    private String getFielder(String player) {
        if(player.equals("pitcher")) {
            return "1";
        }
        if(player.equals("catcher")) {
            return "2";
        }
        if(player.equals("first")) {
            return "3";
        }
        if(player.equals("second")) {
            return "4";
        }
        if(player.equals("third")) {
            return "5";
        }
        if(player.equals("shortstop")) {
            return "6";
        }
        if(player.equals("left")) {
            return "7";
        }
        if(player.equals("center")) {
            return "8";
        }
        if(player.equals("right")) {
            return "9";
        }
        return "";
    }

    private String buildDoublePlay(ArrayList<String> newList){
        String playResult = "DP " + getFielder(newList.get(5));

        for (int i = 0; i< newList.size(); i++) {
            if (newList.get(i).equals("to")){
                playResult = playResult + "-" +getFielder(newList.get(i+1));
            }
        }

        return playResult;
    }
}
