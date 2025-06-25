package com.mungey.demo.model.plays;

import com.mungey.demo.model.context.MVP;
import com.mungey.demo.model.context.Pitcher;
import com.mungey.demo.model.scoreboard.OfficalResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreCardResponse {
    List<HalfInning> innings;
    List<OfficalResponse> officials;
    List<Pitcher> pitchers;
    String weather;
    String firstPitch;
    String gameTime;
    String Attendance;
    MVP homeMVP;
    MVP awayMVP;
}
