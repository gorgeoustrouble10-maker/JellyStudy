package com.jellystudy.coach.repository;

import com.jellystudy.coach.document.WeeklySnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WeeklySnapshotRepository extends MongoRepository<WeeklySnapshot, String> {

    Optional<WeeklySnapshot> findByUserIdAndWeekKey(String userId, String weekKey);

    List<WeeklySnapshot> findTop12ByUserIdOrderByCreatedAtDesc(String userId);
}
