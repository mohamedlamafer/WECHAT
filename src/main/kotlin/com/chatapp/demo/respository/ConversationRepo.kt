package com.chatapp.demo.respository

import com.chatapp.demo.model.conversation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ConversationRepo: JpaRepository<conversation,Int>{
    fun findByName(name:String):List<conversation>

    @Query(
        """
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END 
        FROM conversation c 
        JOIN c.participants cp1 
        JOIN c.participants cp2 
        WHERE c.type = 'private' 
        AND cp1.user.id = :userId1 
        AND cp2.user.id = :userId2 
        AND cp1.user.id != cp2.user.id
        """
    )
    fun existsPrivateConversationBetweenUsers(
        @Param("userId1") userId1: Int?,
        @Param("userId2") userId2: Int?
    ): Boolean

    @Query("SELECT c FROM conversation c JOIN c.participants p WHERE p.user.id = :userId")
    fun findByParticipantsUserId(@Param("userId") userId: Int): List<conversation>


    @Query("""
    SELECT c FROM conversation c 
    WHERE EXISTS (
        SELECT m FROM message m 
        WHERE m.conversation.id = c.id 
        AND EXISTS (
            SELECT cp FROM ConversationParticipant cp 
            WHERE cp.conversation.id = c.id 
            AND cp.id.userId = :userId
        )
    )
""")
    fun findConversationsWithMessages(@Param("userId") userId: Int): List<conversation>

    @Query("""
        SELECT c FROM conversation c 
        WHERE NOT EXISTS (
            SELECT 1 FROM message m 
            WHERE m.conversation.id = c.id
        )
        AND EXISTS (
            SELECT 1 FROM ConversationParticipant cp 
            WHERE cp.conversation.id = c.id 
            AND cp.id.userId = :userId
        )
        ORDER BY c.created_at DESC
    """)
    fun findConversationsWithoutMessages(@Param("userId") userId: Int): List<conversation>

    @Query("SELECT c FROM conversation c WHERE c.type = :type AND LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    fun searchByTypeAndName(@Param("type") type: String, @Param("query") query: String,  pageable: Pageable): List<conversation>
}

