package com.chatapp.demo.respository

import com.chatapp.demo.model.ConversationParticipant
import com.chatapp.demo.model.ConversationParticipantId
import com.chatapp.demo.model.conversation
import java.util.Optional
import org.springframework.data.domain.Limit
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ConversationParticipantRepo: JpaRepository<ConversationParticipant,ConversationParticipantId> {


    fun findBystatus(status:String):List<ConversationParticipant>
    fun findByrole(role:String):List<ConversationParticipant>
    fun findByConversation(conversation: conversation, sort: Sort, limit: Limit): MutableList<ConversationParticipant>

    @Query("SELECT p FROM ConversationParticipant p LEFT JOIN FETCH p.user WHERE p.conversation.id = :conversationId")
    fun findByConversationIdWithUser(conversationId: Int): List<ConversationParticipant>

    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.id.userId = :userId AND cp.id.conversationId = :conversationId")
    fun findByUserIdAndConversationId(@Param("userId") userId: Int, @Param("conversationId") conversationId: Int): Optional<ConversationParticipant>

    @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId OR cp.id.userId = :userId")
    fun findByConversationIdOrUserId(@Param("conversationId") conversationId: Int, @Param("userId") userId: Int): List<ConversationParticipant>
}