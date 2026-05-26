package com.jellystudy.qa.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jellystudy.qa.config.JellystudyRedisProperties;
import com.jellystudy.qa.entity.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testcontainers + Redis 集成测试（无需本机 Redis，JUnit 会自动起 Docker 容器）。
 *
 * <p>学习要点：
 * <ol>
 *   <li>{@code @Testcontainers} + {@code @Container}：测试类启动前拉镜像、起容器</li>
 *   <li>用 {@link StringRedisTemplate} 连容器映射端口，测真实 Redis 命令</li>
 *   <li>不启完整 Spring Boot（无 MySQL/Nacos），只测 Redis 层 —— 快且稳</li>
 * </ol>
 *
 * <p>运行：{@code mvn test -pl jellystudy-qa -Dtest=QuestionRedisServiceTestcontainersTest}
 * （需本机 Docker Desktop 已启动）
 */
@Testcontainers(disabledWithoutDocker = true)
class QuestionRedisServiceTestcontainersTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379);

    private QuestionRedisService questionRedisService;
    private StringRedisTemplate redisTemplate;
    private JellystudyRedisProperties properties;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
                redis.getHost(), redis.getMappedPort(6379));
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(factory);
        redisTemplate.setKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setValueSerializer(StringRedisSerializer.UTF_8);
        redisTemplate.afterPropertiesSet();

        properties = new JellystudyRedisProperties();
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        questionRedisService = new QuestionRedisService(redisTemplate, properties, objectMapper);
    }

    @Test
    void onQuestionCreated_writesHotZsetAndDetailCache() {
        Question q = sampleQuestion("q-hot-1", 3, 2, 10);

        questionRedisService.onQuestionCreated(q);

        Double hotScore = redisTemplate.opsForZSet()
                .score(properties.getHotKey(), q.getId());
        assertNotNull(hotScore);
        assertTrue(hotScore > 0);

        String cached = redisTemplate.opsForValue().get(properties.getQuestionCachePrefix() + q.getId());
        assertNotNull(cached);
        assertTrue(cached.contains("Redis 集成测试"));
    }

    @Test
    void onQuestionUpdated_refreshesDetailCache() {
        Question q = sampleQuestion("q-update-1", 1, 0, 5);
        questionRedisService.onQuestionCreated(q);

        q.setTitle("更新后的标题");
        q.setContent("更新后的正文");
        q.setUpdatedAt(new Date());
        questionRedisService.onQuestionUpdated(q);

        String cached = redisTemplate.opsForValue().get(properties.getQuestionCachePrefix() + q.getId());
        assertNotNull(cached);
        assertTrue(cached.contains("更新后的标题"));
        assertFalse(cached.contains("Redis 集成测试"));
    }

    @Test
    void onQuestionViewed_updatesViewRank() {
        Question q = sampleQuestion("q-view-1", 0, 0, 20);
        questionRedisService.onQuestionCreated(q);

        q.setViewCount(21);
        questionRedisService.onQuestionViewed(q);

        Double viewScore = redisTemplate.opsForZSet()
                .score(properties.getViewRankKey(), q.getId());
        assertNotNull(viewScore);
        assertTrue(viewScore > 0);
    }

    @Test
    void evictQuestion_removesZsetAndCache() {
        Question q = sampleQuestion("q-evict-1", 2, 1, 8);
        questionRedisService.onQuestionCreated(q);

        questionRedisService.evictQuestion(q.getId());

        assertNull(redisTemplate.opsForZSet().score(properties.getHotKey(), q.getId()));
        assertNull(redisTemplate.opsForZSet().score(properties.getViewRankKey(), q.getId()));
        assertNull(redisTemplate.opsForValue().get(properties.getQuestionCachePrefix() + q.getId()));
    }

    @Test
    void getHotTop_returnsCachedDtoFirst() {
        Question q = sampleQuestion("q-top-1", 5, 3, 50);
        questionRedisService.onQuestionCreated(q);

        var list = questionRedisService.getHotTop(5, id -> fail("不应回源 MySQL: " + id));

        assertEquals(1, list.size());
        assertEquals("q-top-1", list.get(0).getId());
        assertEquals("Redis 集成测试", list.get(0).getTitle());
    }

    private static Question sampleQuestion(String id, int likes, int answers, int views) {
        return Question.builder()
                .id(id)
                .title("Redis 集成测试")
                .content("Testcontainers 自动起的 Redis")
                .viewCount(views)
                .likeCount(likes)
                .answerCount(answers)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
    }
}
