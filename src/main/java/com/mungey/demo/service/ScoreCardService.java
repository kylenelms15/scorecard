package com.mungey.demo.service;

import com.google.gson.Gson;
import com.mungey.demo.model.context.Pitcher;
import com.mungey.demo.model.context.BoxscoreContext;
import com.mungey.demo.model.context.PlayerStats;
import com.mungey.demo.model.context.Stat;
import com.mungey.demo.model.player.Lineup;
import com.mungey.demo.model.player.PlayerLookup;
import com.mungey.demo.model.plays.*;
import com.mungey.demo.model.scoreboard.OfficalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ScoreCardService {

    @Autowired
    private RestTemplate restTemplate;

    private String battersFaced;

    public ScoreCardResponse buildScoreCard(String gameId) {
        ScoreCardResponse scoreCardResponse = new ScoreCardResponse();

        BoxscoreContext boxscoreContext = buildGameContext(gameId);

        buildOfficals(scoreCardResponse, boxscoreContext);
        buildExtraContext(scoreCardResponse, boxscoreContext);
        scoreCardResponse.setPitchers(fillPitcherStats(getPitcherJerseyNumbers(boxscoreContext, scoreCardResponse.getPitchers()), gameId));
        scoreCardResponse.setLineup(buildLineUp(boxscoreContext));

        List<Plays> playsList = getPlays(gameId).getAllPlays();
        int finalInning = getLastInning(playsList);
        scoreCardResponse.setInnings(buildPlays(finalInning, playsList));

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

            BSCount count = new BSCount();
            count.setBalls(play.getCount().getBalls());
            count.setStrikes(play.getCount().getStrikes());
            count.setOuts(play.getCount().getOuts());
            inningPlay.setCount(count);
            inningPlay.setRbi(play.getResult().getRbi());

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

        scoreCard.setOfficials(officals);
        return scoreCard;
    }

    private ScoreCardResponse buildExtraContext(ScoreCardResponse scoreCard, BoxscoreContext boxscoreContext) {
        boxscoreContext.getInfo().forEach(contextInfo -> {
            switch(contextInfo.getLabel()) {
                case "Pitches-strikes":
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

        scoreCard.setPitchers(buildPitchers(battersFaced));

        return scoreCard;
    }

    private List<Pitcher> buildPitchers(String battersString) {
        List<Pitcher> battersFaced = new ArrayList<>();

        battersString = StringUtils.substring(battersString, 0, battersString.length() - 1);

        String[] facedArray = battersString.split(";");

        for(int i=0;i< facedArray.length; i++) {
            Pitcher pitcher = new Pitcher();

            String facedString = facedArray[i];

            int numBatters = Integer.parseInt(facedString.replaceAll("[^0-9]", ""));

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
                        o.setPitcherId(pitcherId);
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
        ArrayList<String> playStringList = new ArrayList<>();

        for(int i = 0; i<splitString.length; i++){
            if(!splitString[i].equals("sharply") && !splitString[i].equals("softly")) {
                playStringList.add(splitString[i]);
            }
        }

        if(playStringList.contains("upheld")) {
            playStringList.subList(0, playStringList.indexOf("upheld") + 1).clear();
        }
        if(playStringList.contains("overturned")) {
            playStringList.subList(0, playStringList.indexOf("overturned") + 1).clear();
        }

        return getPlayShortform(playStringList);
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

    private String getPlayShortform(ArrayList<String> playStringList) {

        if(playStringList.contains("walks")) {
            if(playStringList.indexOf("intentionally") >=0) {
                return "IBB";
            }
            return "BB";
        }

        if(playStringList.contains("singles")) {
            return "1B";
        }

        if(playStringList.contains("doubles")) {
            return "2B";
        }

        if(playStringList.contains("triples")) {
            return "3B";
        }

        if(playStringList.contains("homers")) {
            return "HR";
        }

        if(playStringList.contains("hits")) {
            if(playStringList.contains("ground-rule")) {
                return "2B GR";
            }
            if(playStringList.contains("grand")) {
                return "GSHR";
            }
        }

        if(playStringList.contains("hit")) {
            return "HBP";
        }

        if(playStringList.contains("out")) {
            if(playStringList.contains("sacrifice")) {
                if(playStringList.contains("fly")) {
                    return "SF " + getFielder(playStringList.get(playStringList.indexOf("to")+1));
                }

                if(playStringList.contains("bunt")) {
                    return "SB " + getFielder(playStringList.get(5)) + "-" + getFielder(playStringList.get(playStringList.indexOf("to")+1));
                }
            }
            if(playStringList.contains("called")) {
                return "Backwards K";
            }
            if(playStringList.contains("strikes")) {
                return "K";
            }
            if(playStringList.contains("flies")) {
                return "F" + getFielder(playStringList.get(3));
            }
            if(playStringList.contains("lines")) {
                return "L" + getFielder(playStringList.get(3));
            }
            if(playStringList.contains("pops")) {
                return "P" + getFielder(playStringList.get(3));
            }
        }

        if(playStringList.contains("grounds") || playStringList.contains("pops")) {
            String playResult;

            if(playStringList.contains("force")) {
                if(playStringList.contains("fielded")) {
                    playResult = getFielder(playStringList.get(playStringList.indexOf("by")+1));
                } else {
                    playResult = getFielder(playStringList.get(5)) + "-" + getFielder(playStringList.get(playStringList.indexOf("to")+1));
                }
            } else if(playStringList.contains("double")){
                return buildDoublePlay(playStringList);
            }
            else {
                playResult = getFielder(playStringList.get(2)) + "-" + getFielder(playStringList.get(playStringList.indexOf("to")+1));
            }

            if(playResult.length() == 2) {
                return playResult.charAt(1) + "U";
            }

            if(playResult.length() == 1) {
                return playResult.charAt(0) + "U";
            }

            return playResult;
        }

        if(playStringList.contains("lines")) {
            if(playStringList.contains("double")){
                return buildDoublePlay(playStringList);
            }
            if(playStringList.contains("unassisted") && playStringList.contains("double")){
                return "DP " + getFielder(playStringList.get(6)) + "U";
            }
        }

        if(playStringList.contains("fielder's")) {
            String playResult = "FC ";

            if(playStringList.contains("by")){
                return playResult + getFielder(playStringList.get(playStringList.indexOf("by") + 1));
            }

            return playResult + getFielder(playStringList.get(6)) + "-" + getFielder(playStringList.get(playStringList.indexOf("to")+1));
        }

        if(playStringList.contains("error")) {
            return "E" + getFielder(playStringList.get(playStringList.indexOf("by")+1));
        }

        return playStringList.toString();
        //return "No At Bat";
    }

    private List<Pitcher> fillPitcherStats(List<Pitcher> pitchersList, String gameId) {
        List<Pitcher> pitchers = new ArrayList<>();

        pitchersList.forEach(incomingPitcher -> {
            Pitcher pitcher = new Pitcher();
            String apiUrl = "https://statsapi.mlb.com/api/v1/people/" + incomingPitcher.getPitcherId() + "/stats/game/" + gameId;

            Gson gson = new Gson();

            AtomicReference<Stat> pitcherStat = new AtomicReference<>();
            PlayerStats playerStats = gson.fromJson(restTemplate.getForObject(apiUrl, String.class), PlayerStats.class);

            playerStats.getStats().get(0).getSplits().forEach(split -> {
                if(split.getGroup().equals("pitching")) {
                    pitcherStat.set(split.getStat());
                }
            });
            pitcher.setJerseyNumber(incomingPitcher.getJerseyNumber());
            pitcher.setName(incomingPitcher.getName());
            pitcher.setInningsPitched(pitcherStat.get().getInningsPitched());
            pitcher.setHits(pitcherStat.get().getHits());
            pitcher.setRuns(pitcherStat.get().getRuns());
            pitcher.setEarnedRuns(pitcherStat.get().getEarnedRuns());
            pitcher.setWalks(pitcherStat.get().getBaseOnBalls());
            pitcher.setStrikeOuts(pitcherStat.get().getStrikeOuts());
            pitcher.setBattersFaced(pitcherStat.get().getBattersFaced());
            pitcher.setPitches(pitcherStat.get().getPitchesThrown());
            pitcher.setTeam(incomingPitcher.getTeam());
            pitcher.setPitcherId(incomingPitcher.getPitcherId());

            pitchers.add(pitcher);
        });

        return pitchers;
    }
}
