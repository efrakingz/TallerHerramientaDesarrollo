package com.sansaweigh.repository;

import com.sansaweigh.domain.RegistroPesaje;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RegistroPesajeRepository extends MongoRepository<RegistroPesaje, String> {

    @Query("{ 'createdAt': { $gte: ?0, $lt: ?1 } }")
    List<RegistroPesaje> findByCreatedAtBetween(Instant start, Instant end);
}
