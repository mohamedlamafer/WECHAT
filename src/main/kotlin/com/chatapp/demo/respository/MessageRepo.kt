package com.chatapp.demo.respository

import com.chatapp.demo.model.message
import com.chatapp.demo.model.message_text
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MessageRepo: JpaRepository<message,Int> {
    fun findByConversationId(conversationId:Int):List<message>
    fun findBySenderId(senderId:Int):List<message>
    fun findByConversationName(conversationName:String):List<message>
    fun findBySenderUsername(senderName:String):List<message>
    fun deleteByConversationId(id:Int)
    @Query("""
    SELECT m FROM message m 
    WHERE m.conversation.id IN (
        SELECT p.conversation.id FROM ConversationParticipant p WHERE p.user.id = :userId
    )
""")
    fun findAllUserMessages(@Param("userId") userId: Int): List<message>

    @Query("SELECT m FROM message_text m LEFT JOIN FETCH m.sender WHERE m.conversation.id = :conversationId ORDER BY m.created_at")
    fun findByConversationIdWithSender(conversationId: Int): List<message_text>
}

