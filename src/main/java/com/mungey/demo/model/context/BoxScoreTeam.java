package com.mungey.demo.model.context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoxScoreTeam {
    TeamInfo team;
    List<Integer> battingOrder;
    List<Integer> pitchers;
    List<Integer> bullpen;
}
