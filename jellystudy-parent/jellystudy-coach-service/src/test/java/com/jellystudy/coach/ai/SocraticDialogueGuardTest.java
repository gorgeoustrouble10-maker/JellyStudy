package com.jellystudy.coach.ai;

import com.jellystudy.common.entity.SocraticMessageDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocraticDialogueGuardTest {

    private static SocraticMessageDTO assistant(String content) {
        return SocraticMessageDTO.builder().role("assistant").content(content).build();
    }

    @Test
    void detectsBlockedReadyResolution() {
        List<SocraticMessageDTO> history = List.of(
                assistant("如果进程因等待调度而进入阻塞态，那是否意味着它无法被调度器选中？这与就绪态的定义是否有冲突？")
        );
        assertTrue(SocraticDialogueGuard.misconceptionResolved(
                "不冲突。阻塞态是资源等待，就绪态是等待CPU调度", history));
    }

    @Test
    void detectsScaffoldNeed() {
        assertTrue(SocraticDialogueGuard.needsScaffoldHelp("我不清楚具体怎么组织语言"));
    }

    @Test
    void scaffoldStaysOnSchedulingTopic() {
        List<SocraticMessageDTO> history = List.of(
                assistant("短作业优先和先来先服务对平均等待时间会有什么不同？各举一个直觉例子。")
        );
        var reply = SocraticDialogueGuard.buildScaffoldReply("操作系统", history, 3);
        assertTrue(reply.isScaffoldMode());
        assertTrue(reply.getReply().contains("FCFS") || reply.getReply().contains("先来先服务"));
        assertFalse(reply.getReply().contains("阻塞态与就绪态各自在等什么"));
    }

    @Test
    void contextualFallbackDoesNotJumpToBlockedReady() {
        List<SocraticMessageDTO> history = List.of(
                assistant("短作业优先和先来先服务对平均等待时间会有什么不同？")
        );
        var reply = SocraticDialogueGuard.buildContextualFallback(
                "操作系统", history, 3, "短作业的时间会比较短，但我不清楚具体怎么组织语言");
        assertTrue(reply.getReply().contains("短作业") || reply.getReply().contains("FCFS")
                || reply.getReply().contains("平均等待"));
        assertFalse(reply.getReply().contains("阻塞态与就绪态各自在等什么"));
    }

    @Test
    void detectsDuplicateReply() {
        String q = "如果进程因等待调度而进入阻塞态，那是否意味着它无法被调度器选中？";
        List<SocraticMessageDTO> history = List.of(assistant(q));
        assertTrue(SocraticDialogueGuard.isNearDuplicateReply(q, history));
    }
}
