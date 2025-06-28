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
    String name;
    int battersFaced;
    int pitches;
    int strikes;
    int jerseyNumber;
    String team;
}
