package com.mungey.demo.model.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    public String gamePk;
    public Teams teams;
    public int scheduledInnings;
}
