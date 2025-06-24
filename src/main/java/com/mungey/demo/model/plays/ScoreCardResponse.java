package com.mungey.demo.model.plays;

import com.mungey.demo.model.context.BattersFaced;
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
    List<OfficalResponse> officals;
    String pitchesStrikes;
    List<BattersFaced> battersFaced;
    String weather;
    String firstPitch;
    String gameTime;
    String Attendance;
}
