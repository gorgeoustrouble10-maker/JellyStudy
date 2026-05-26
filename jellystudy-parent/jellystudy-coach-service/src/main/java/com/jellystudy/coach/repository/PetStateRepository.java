package com.jellystudy.coach.repository;

import com.jellystudy.coach.document.PetState;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PetStateRepository extends MongoRepository<PetState, String> {

    Optional<PetState> findFirstByUserId(String userId);

    List<PetState> findAllByUserId(String userId);
}
