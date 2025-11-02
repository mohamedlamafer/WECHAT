
package com.chatapp.demo.dto

import com.chatapp.demo.model.ConversationParticipantId
import java.time.LocalDateTime

// User DTOs
data class CreateUserRequest(
    val username: String,
    val email: String,
    val phone: String,
    val password: String
)

data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val phone: String
)


data class UpdateUsername(
    val username: String
)

data class UpdateEmail(
    val email: String
)

data class UpdatePhone(
    val phone: String
)

data class UpdatePassword(
    val password: String,
    val confirmedPassword: String
)



data class CreatePrivateConversationRequest(
    val otherUserId: Int,
    val customName: String? = null
)


data class CreateGroupConversationRequest(
    val name: String
)

data class ConversationResponse(
    val id: Int,
    val type: String,
    val name: String?,
    val createdAt: LocalDateTime?,
    val participantCount: Int
)

data class GetUserConversations(
    val page: Int = 0,
    val size: Int = 50
)

data class BlockingOptions(
    val conversationId: Int,
    val targetUserId: Int
)

data class CreateMessage(
    val conversationId: Int,
    val content: String
)

data class MessageResponse(
    val id: Int?,
    val senderId: Int?,
    val content: String,
    val createdAt: LocalDateTime?
)


data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

data class UpdateGroupNameRequest(
    val conversationId: Int,
    val name: String
)

data class AddParticipantRequest(
    val userId: Int,
    val role: String = "Member"
)

data class ParticipantResponse(
    val id: ConversationParticipantId,
    val userId: Int?,
    val username: String?,
    val role: String?,
    val status: String,
    val joinedAt: LocalDateTime?
)

data class DeleteParticipantConversation(
    val conversationId: Int,
    val targetUserId: Int
)

data class DeleteMessage(
    val messageId: Int
)

data class LoginRequest(
    val email: String?,
    val phone: String?,
    val password: String
)

data class LoginResponse(
    val id: Int,
    val username: String,
    val email: String,
    val phone: String,
    val message: String
)
