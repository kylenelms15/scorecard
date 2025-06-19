package com.mungey.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

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
