package com.toolnews.bot.entity;

import com.toolnews.bot.entity.enumeration.TimeSettingOption;
import com.toolnews.bot.entity.enumeration.TimeSettingUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(schema = "tool-masters-bot", name = "t_site_setting")
public class SiteSettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @Column(name = "c_list_url")
    private String listUrl;

    @Column(name = "c_last_element_url")
    private String lastElementUrl;

    @Column(name = "c_element_wrapper")
    private String elementWrapper;



    @Column(name = "c_time_setting_option")
    private TimeSettingOption timeSettingOption;

    @Column(name = "c_news_check_time")
    private Time newsCheckTime;

    @Column(name = "c_every_time_unit")
    private Integer everyTimeUnit;

    @Column(name = "c_time_setting_unit")
    private TimeSettingUnit timeSettingUnit;

}
