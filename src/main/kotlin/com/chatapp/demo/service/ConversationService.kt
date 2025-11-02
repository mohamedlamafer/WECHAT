package com.chatapp.demo.service

import com.chatapp.demo.model.ConversationParticipant
import com.chatapp.demo.model.User
import com.chatapp.demo.respository.MessageRepo
import com.chatapp.demo.model.message_text
import com.chatapp.demo.model.conversation
import com.chatapp.demo.respository.ConversationRepo
import com.chatapp.demo.respository.ConversationParticipantRepo
import com.chatapp.demo.respository.UserRepo
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.ResponseStatus
import java.time.LocalDateTime

@ResponseStatus(HttpStatus.CONFLICT)
class ConversationAlreadyExistsException(message: String) : RuntimeException(message)

@Service
@Transactional
class ConversationService(
    private val conversationRepo: ConversationRepo,
    private val messageRepo: MessageRepo,
    private val conversationParticipantRepo: ConversationParticipantRepo,
    private val userRepo: UserRepo
) {

    fun hasPrivateConversation(userId1: Int?, userId2: Int?): Boolean {
        return conversationRepo.existsPrivateConversationBetweenUsers(userId1, userId2)
    }


    fun creatPrivateConversation(userId1: Int?, userId2: Int?, type: String, custom_name1: String?): conversation {
        if (userId1 == null || userId2 == null) {
            throw IllegalArgumentException("User IDs cannot be null")
        }

        if (userId1 == userId2) {
            throw IllegalArgumentException("Cannot create conversation with yourself")
        }

        val possibleconversation = hasPrivateConversation(userId1, userId2)
        if (possibleconversation) {
            throw ConversationAlreadyExistsException("Private conversation already exists")
        }

        val user1 = userRepo.findById(userId1).orElseThrow { IllegalArgumentException("User1 not found") }
        val user2 = userRepo.findById(userId2).orElseThrow { IllegalArgumentException("User2 not found") }

        val conversation = conversation(type = type, created_by = user1, name = null)

        val participant1 = if (custom_name1 == null) {
            ConversationParticipant(conversation = conversation, user = user1, role = null, status = "ACTIVE", customName = user2.phone)
        } else {
            ConversationParticipant(conversation = conversation, user = user1, role = null, status = "ACTIVE", customName = custom_name1)
        }

        val participant2 = ConversationParticipant(conversation = conversation, user = user2, role = null, status = "ACTIVE", customName = user1.phone)

        conversation.participants.add(participant1)
        conversation.participants.add(participant2)

        return conversationRepo.save(conversation)
    }


    fun creatGroupConversation(name: String, creatorId: Int, type: String): conversation {
        val validatedname = ValidationHelpers.generalHelpers.validatename(name)
        val creator = userRepo.findById(creatorId).orElseThrow { IllegalArgumentException("User not found") }

        val conversation = conversation(type = type, name = validatedname, created_by = creator)
        val admin = ConversationParticipant(conversation = conversation, user = creator, role = "Admin", status = "ACTIVE")

        conversation.participants.add(admin)
        return conversationRepo.save(conversation)
    }

    fun getConversationById(conversationId: Int): conversation {
        return conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found") }
    }


    fun getUserConversations(userId: Int): List<conversation> {
        return conversationRepo.findByParticipantsUserId(userId)
    }

    fun searchUsers(query: String): List<User> {
        if (query.isBlank()) return emptyList()
        val pageable = PageRequest.of(0, 10)
        return userRepo.searchByUsernameEmailOrPhone(query, pageable)
    }

    fun searchGroups(query: String): List<conversation> {
        if (query.isBlank()) return emptyList()
        val pageable = PageRequest.of(0, 10)
        return conversationRepo.searchByTypeAndName("group", query, pageable)
    }

    fun getConversationsByname(name: String): List<conversation> {
        return conversationRepo.findByName(name)
    }


    fun getUserChats(userId: Int): List<conversation> {
        return conversationRepo.findConversationsWithMessages(userId)
    }


    fun getUserContacts(userId: Int): List<conversation> {
        return conversationRepo.findConversationsWithoutMessages(userId)
    }

    @Transactional(readOnly = true)
    fun getConversationParticipants(conversationId: Int): List<ConversationParticipant> {
        return conversationParticipantRepo.findByConversationIdWithUser(conversationId)
    }

    @Transactional(readOnly = true)
    fun getConversationMessages(conversationId: Int): List<message_text> {
        return messageRepo.findByConversationIdWithSender(conversationId)
    }


    fun addTextMessage(actorId: Int, conversationId: Int, content: String, sentAt: LocalDateTime): message_text {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found") }

        val sender = userRepo.findById(actorId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val isParticipant = conversation.participants.any { it.user?.id == actorId }
        if (!isParticipant) {
            throw IllegalArgumentException("Sender is not a participant in this conversation")
        }

        val message = message_text(content = content, conversation = conversation, sender = sender)
        updateLastMessage(conversationId, sentAt)
        return messageRepo.save(message)
    }


    @Transactional
    fun addParticipant(actorId: Int, conversationId: Int, userId: Int, role: String) {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found") }

        val actorParticipant = conversation.participants.find { it.user?.id == actorId }
            ?: throw IllegalArgumentException("Actor is not a participant in conversation $conversationId")

        if (actorParticipant.role != "Admin") {
            throw IllegalStateException("Only admins can add participants")
        }

        val user = userRepo.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        if (conversation.participants.any { it.user?.id == userId }) {
            throw IllegalStateException("User $userId is already a participant")
        }

        val participant = ConversationParticipant(conversation = conversation, user = user, status = "ACTIVE", role = role)
        conversation.participants.add(participant)
        conversationRepo.save(conversation)
    }

    @Transactional
    fun updateLastMessage(conversationId: Int, sentAt: LocalDateTime) {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found") }

        conversation.lastMessageAt = sentAt
        conversationRepo.save(conversation)
    }


    @Transactional
    fun blockUser(actorId: Int, conversationId: Int, userId: Int): Boolean {
        // ensure actor is participant and has rights
        val actorParticipant = conversationParticipantRepo.findByUserIdAndConversationId(actorId, conversationId)
            .orElseThrow { IllegalArgumentException("Acting participant not found") }

        if (actorParticipant.role != "Admin") {
            throw IllegalStateException("Only admins can block users")
        }

        val participantToBlock = conversationParticipantRepo.findByUserIdAndConversationId(userId, conversationId)
            .orElseThrow { IllegalArgumentException("Participant not found") }

        if (participantToBlock.status == "BLOCKED") {
            throw IllegalStateException("User $userId is already blocked in conversation $conversationId")
        }

        participantToBlock.status = "BLOCKED"
        conversationParticipantRepo.save(participantToBlock)
        return true
    }


    @Transactional
    fun deblockUser(actorId: Int, conversationId: Int, userId: Int) {
        val actorParticipant = conversationParticipantRepo.findByUserIdAndConversationId(actorId, conversationId)
            .orElseThrow { IllegalArgumentException("Acting participant not found") }

        if (actorParticipant.role != "Admin") {
            throw IllegalStateException("Only admins can unblock users")
        }

        val participant = conversationParticipantRepo.findByUserIdAndConversationId(userId, conversationId)
            .orElseThrow { IllegalArgumentException("Participant not found") }

        if (participant.status == "BLOCKED") {
            participant.status = "ACTIVE"
            conversationParticipantRepo.save(participant)
        }
    }


    fun updateGroupConversationName(actorId: Int, conversationId: Int, newName: String?) {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found") }


        val actorParticipant = conversation.participants.find { it.user?.id == actorId }
            ?: throw IllegalArgumentException("Actor is not a participant in this conversation")


        if (newName != null) conversation.name = newName
        conversationRepo.save(conversation)
    }


    @Transactional
    fun deleteUserConversation(actorId: Int, conversationId: Int, targetUserId: Int) {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found: $conversationId") }

        val actorIsParticipant = conversation.participants.any { it.user?.id == actorId }
        if (!actorIsParticipant) throw IllegalArgumentException("Actor is not a participant in conversation $conversationId")

        val participantToRemove = conversation.participants.find { it.user?.id == targetUserId }
            ?: throw IllegalArgumentException("User $targetUserId is not a participant in conversation $conversationId")

        if (actorId != targetUserId) {
            val actorParticipant = conversation.participants.find { it.user?.id == actorId }
                ?: throw IllegalArgumentException("Actor participant record not found")
            if (actorParticipant.role != "Admin") {
                throw IllegalStateException("Only admins can remove other participants")
            }
        }

        conversation.participants.remove(participantToRemove)
        conversationRepo.save(conversation)
    }

    @Transactional
    fun deleteParticipant(actorId: Int, conversationId: Int, targetUserId: Int) {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found: $conversationId") }

        val actorParticipant = conversation.participants.find { it.user?.id == actorId }
            ?: throw IllegalArgumentException("Actor is not a participant in conversation $conversationId")

        val targetParticipant = conversation.participants.find { it.user?.id == targetUserId }
            ?: throw IllegalArgumentException("User $targetUserId is not a participant in conversation $conversationId")

        if (actorId == targetUserId) {
            conversation.participants.remove(targetParticipant)
            conversationRepo.save(conversation)
            return
        }

        if (actorParticipant.role != "Admin") {
            throw IllegalStateException("Only admins can remove other participants")
        }

        if (targetParticipant.role == "Admin") {
            throw IllegalStateException("Cannot remove another admin")
        }

        conversation.participants.remove(targetParticipant)
        conversationRepo.save(conversation)
    }

    @Transactional
    fun promoteToAdmin(actorId: Int, conversationId: Int, targetUserId: Int) {
        val conversation = conversationRepo.findById(conversationId)
            .orElseThrow { IllegalArgumentException("Conversation not found: $conversationId") }

        val actorParticipant = conversation.participants.find { it.user?.id == actorId }
            ?: throw IllegalArgumentException("Actor is not a participant in conversation $conversationId")

        if (actorParticipant.role != "Admin") {
            throw IllegalStateException("Only admins can promote participants")
        }

        val targetParticipant = conversation.participants.find { it.user?.id == targetUserId }
            ?: throw IllegalArgumentException("Target user is not a participant in conversation $conversationId")

        targetParticipant.role = "Admin"
        conversationParticipantRepo.save(targetParticipant)
    }

    @Transactional
    fun deleteMessageAsAdmin(actorId: Int, messageId: Int) {
        val message = messageRepo.findById(messageId)
            .orElseThrow { IllegalArgumentException("Message not found with id $messageId") }

        val conversation = message.conversation ?: throw IllegalArgumentException("Message has no conversation")
        val conversationId = conversation.id ?: throw IllegalArgumentException("Conversation id missing")

        val actorParticipant = conversation.participants.find { it.user?.id == actorId }
            ?: throw IllegalArgumentException("Actor is not a participant in conversation $conversationId")

        if (actorParticipant.role != "Admin") {
            throw IllegalStateException("Only admins can delete others' messages")
        }

        messageRepo.delete(message)
    }

    fun deleteMessage(messageId: Int, userId: Int) {
        val message = messageRepo.findById(messageId)
            .orElseThrow { IllegalArgumentException("Message not found with id $messageId") }

        if (message.sender.id != userId) {
            throw IllegalArgumentException("User is not allowed to delete this message")
        }

        messageRepo.delete(message)
    }
}
