package com.toolnews.bot.repository;

import com.toolnews.bot.entity.BotSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BotSettingsRepository extends JpaRepository<BotSettingsEntity, Long> {

    Optional<BotSettingsEntity> findFirstByOrderByIdAsc();

}
