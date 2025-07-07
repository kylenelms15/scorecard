package com.mungey.demo.model.context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Stat {
    String inningsPitched;
    int hits;
    int runs;
    int earnedRuns;
    int baseOnBalls;
    int strikeOuts;
    int battersFaced;
    int pitchesThrown;
    int strikes;
}
