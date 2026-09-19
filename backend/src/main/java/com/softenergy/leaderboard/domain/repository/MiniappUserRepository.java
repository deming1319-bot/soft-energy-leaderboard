package com.softenergy.leaderboard.domain.repository;

import com.softenergy.leaderboard.domain.model.MiniappUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MiniappUserRepository extends JpaRepository<MiniappUser, String> {
    Optional<MiniappUser> findByOpenid(String openid);
}

