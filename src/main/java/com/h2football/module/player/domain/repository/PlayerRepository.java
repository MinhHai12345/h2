package com.h2football.module.player.domain.repository;

import com.h2football.module.player.domain.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {}
