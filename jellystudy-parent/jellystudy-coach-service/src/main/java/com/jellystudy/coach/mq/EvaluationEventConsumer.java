package com.jellystudy.coach.mq;

import com.jellystudy.common.entity.EvaluationCompletedEvent;
import com.jellystudy.coach.service.CoachServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationEventConsumer {

    private final CoachServiceImpl coachService;

    @RabbitListener(queues = "${coach.rabbit.evaluation-queue}")
    public void onEvaluationCompleted(EvaluationCompletedEvent event) {
        log.info("收到评估完成事件 answerId={}", event.getAnswerId());
        coachService.onEvaluationCompleted(
                event.getAnswerId(),
                event.getQuestionId(),
                event.getScore(),
                event.getGrade(),
                event.getUserId());
    }
}
