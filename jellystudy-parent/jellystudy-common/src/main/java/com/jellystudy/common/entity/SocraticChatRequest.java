package com.jellystudy.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocraticChatRequest implements Serializable {
    private String message;
    @Builder.Default
    private List<SocraticMessageDTO> history = new ArrayList<>();
    /** socratic=追问引导（默认）；popularize=纯科普，先讲透再轻问 */
    @Builder.Default
    private String teachMode = "socratic";
}
