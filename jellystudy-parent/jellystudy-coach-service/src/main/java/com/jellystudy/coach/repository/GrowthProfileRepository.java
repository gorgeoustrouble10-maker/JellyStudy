package com.jellystudy.coach.repository;

import com.jellystudy.coach.document.GrowthProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GrowthProfileRepository extends MongoRepository<GrowthProfile, String> {

    Optional<GrowthProfile> findFirstByUserIdOrderByUpdatedAtDesc(String userId);

    List<GrowthProfile> findAllByUserIdOrderByUpdatedAtDesc(String userId);
}
