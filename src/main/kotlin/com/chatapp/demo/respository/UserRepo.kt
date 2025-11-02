package com.chatapp.demo.respository

import com.chatapp.demo.model.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepo:JpaRepository<User,Int?> {
    fun findByEmailOrPhone(email: String?, phone: String?):Optional<User>
    fun findByUsername(username: String): Optional<User>
    fun existsByPhone(phone:String):Boolean
    fun existsByEmail(email:String):Boolean

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :query, '%'))")
    fun searchByUsernameEmailOrPhone(@Param("query") query: String,  pageable: Pageable): List<User>
}

