package com.news.content.checker.repository;

import com.news.content.checker.entity.SiteSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteSettingRepository extends JpaRepository<SiteSettingEntity, Long> {
}
