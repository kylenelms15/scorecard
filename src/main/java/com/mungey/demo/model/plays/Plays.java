package com.mungey.demo.model.plays;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Plays {
    PlayResult result;
    PlayAbout about;
    BSCount count;
    PlayMatchup matchup;
    List<PlayRunner> runners;
}
