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
public class BoxscoreContext {
    List<ContextOffical> officials;
    List<ContextInfo> info;
    List<ContextTopPerformer> topPerformers;
}
