package com.jellystudy.coach.repository;

import com.jellystudy.coach.document.SocraticSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SocraticSessionRepository extends MongoRepository<SocraticSession, String> {

    List<SocraticSession> findTop5ByUserIdOrderByCreatedAtDesc(String userId);

    List<SocraticSession> findByUserIdOrderByCreatedAtDesc(String userId);
}
