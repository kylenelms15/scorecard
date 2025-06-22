package com.mungey.demo.model.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Scoreboard {
    public String gameId;
    public String homeTeam;
    public String awayTeam;
    public int homeScore;
    public int awayScore;
}
