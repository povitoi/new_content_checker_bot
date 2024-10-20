package com.toolnews.bot.entity;

import com.toolnews.bot.entity.enumeration.LinkType;
import com.toolnews.bot.entity.enumeration.TimeSettingOption;
import com.toolnews.bot.entity.enumeration.IntervalUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;
import java.sql.Timestamp;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(schema = "new-content-checker-bot", name = "t_site_setting")
public class SiteSettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "c_running")
    private boolean running;

    @Column(name = "c_setting_created")
    private Timestamp settingCreated;

    @Column(name = "c_last_check")
    private Timestamp lastCheck;

    @Column(name = "c_list_url")
    private String listUrl;

    @Column(name = "c_element_url")
    private String elementUrl;

    @Column(name = "c_element_wrapper")
    private String elementWrapper;

    @Column(name = "c_link_type")
    private LinkType linkType;



    @Column(name = "c_time_setting_option")
    private TimeSettingOption timeSettingOption;

    @Column(name = "c_news_check_time")
    private Time newsCheckTime;



    @Column(name = "c_every_time_value")
    private Integer everyTimeValue;

    @Column(name = "c_interval_unit")
    private IntervalUnit intervalUnit;

}
