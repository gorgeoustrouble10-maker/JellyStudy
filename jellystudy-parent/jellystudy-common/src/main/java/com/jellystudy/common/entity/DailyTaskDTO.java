package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTaskDTO implements Serializable {

    private String taskId;
    private String title;
    private String weakPoint;
    private int rewardPoints;
    private boolean completed;
}
