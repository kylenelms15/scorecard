package com.mungey.demo.model.context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pitcher {
    int jerseyNumber;
    String name;
    String inningsPitched;
    int hits;
    int runs;
    int earnedRuns;
    int walks;
    int strikeOuts;
    int battersFaced;
    int pitches;
    String team;
    int pitcherId;
}
