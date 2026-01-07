package com.zg.ai.repository;

import com.zg.ai.entity.po.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends R2dbcRepository<User, String> {
    Mono<User> findByUsername(String username);
}