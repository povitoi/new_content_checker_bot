package com.toolnews.bot.repository;

import com.toolnews.bot.model.SiteSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteSettingRepository extends JpaRepository<SiteSettingEntity, Long> {
}
