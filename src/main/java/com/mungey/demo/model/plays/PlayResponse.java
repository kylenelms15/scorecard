package com.mungey.demo.model.plays;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayResponse {
    String player;
    String atBatResult;
    BSCount count;
    int rbi;
    String description;
}
