package com.toolnews.bot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(schema = "tool-masters-bot", name = "t_bot_user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "c_user_id",nullable = false)
    private Long userId;

    @Column(name = "c_user_name")
    private String userName;

    @Column(name = "c_first_name")
    private String firstName;

    @Column(name = "c_last_name")
    private String lastName;

    @Column(name = "c_added")
    private Timestamp added;

}
