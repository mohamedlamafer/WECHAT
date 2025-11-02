package com.chatapp.demo.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name="users")
class User(
           @Id
           @GeneratedValue(strategy = GenerationType.IDENTITY)
           var id: Int?=null,
           @Column(name="username", nullable = false)
           var username:String,
           @Column(name="email", nullable = false)
           var email:String,
           @Column(name="phone", nullable = false)
           var phone:String,
           @Column(name="created_at", nullable = false)
           val created_at:LocalDateTime=LocalDateTime.now(),
           @Column(name="password", nullable = false)
           var password:String,
           @Column(name="profile_image", nullable = true)
           var profile_image:String?=null,

           @OneToMany(mappedBy="sender")
           var messages:MutableList<message> =mutableListOf()
)


