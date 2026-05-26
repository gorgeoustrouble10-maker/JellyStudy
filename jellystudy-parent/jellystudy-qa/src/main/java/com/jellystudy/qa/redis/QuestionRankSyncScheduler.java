package com.jellystudy.qa.redis;

import com.jellystudy.qa.entity.Question;
import com.jellystudy.qa.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时将 MySQL 中的问题热度同步到 Redis（缓存/数据内容同步策略）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionRankSyncScheduler {

    private final QuestionRepository questionRepository;
    private final QuestionRedisService questionRedisService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpOnStartup() {
        log.info("应用启动：预热 Redis 排行榜...");
        syncRankings();
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    public void syncRankingsScheduled() {
        syncRankings();
    }

    public void syncRankings() {
        List<Question> all = questionRepository.findAll();
        questionRedisService.rebuildRankings(all);
    }
}
