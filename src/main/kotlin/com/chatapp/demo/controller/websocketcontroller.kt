package com.chatapp.demo.controller

import com.chatapp.demo.service.ConversationService
import com.chatapp.demo.service.UserService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.time.LocalDateTime


data class ChatMessage(
    val conversationId: Int,
    val senderId: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class MessageResponse(
    val id: Int,
    val conversationId: Int,
    val senderId: Int,
    val senderName: String,
    val content: String,
    val timestamp: LocalDateTime
)

@Controller
class WebSocketController(
    private val messagingTemplate: SimpMessagingTemplate,
    private val messageService: ConversationService,  // Inject MessageService
    private val userService: UserService  // Inject UserService to get username
) {

    @MessageMapping("/chat.send")
    fun sendMessage(
        @Payload message: ChatMessage,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        println("=== Received message ===")
        println("Message: $message")
        println("User principal: ${headerAccessor.user}")
        println("User principal name: ${headerAccessor.user?.name}")

        val userIdString = headerAccessor.user?.name

        if (userIdString == null) {
            println("❌ ERROR: User not authenticated")
            throw IllegalStateException("User not authenticated")
        }

        val authenticatedUserId = try {
            userIdString.toInt()
        } catch (e: NumberFormatException) {
            println("❌ ERROR: Invalid user ID format: $userIdString")
            throw IllegalStateException("Invalid user ID")
        }

        println("Authenticated user ID: $authenticatedUserId")
        println("Message senderId: ${message.senderId}")

        if (message.senderId != authenticatedUserId) {
            println("❌ ERROR: Sender mismatch! Authenticated: $authenticatedUserId, Claimed: ${message.senderId}")
            throw IllegalArgumentException(
                "Cannot send message as different user. You are user $authenticatedUserId but tried to send as ${message.senderId}"
            )
        }

        println("✓ Sender verified: $authenticatedUserId")

        try {
            val savedMessage = messageService.addTextMessage(
                actorId = authenticatedUserId,
                content = message.content,
                conversationId = message.conversationId,
                sentAt = LocalDateTime.now()
            )

            println("✓ Message saved to database with ID: ${savedMessage.id}")

            val sender = userService.finduserById(authenticatedUserId)
            val senderName = sender.username

            val response = MessageResponse(
                id = savedMessage.id!!,
                conversationId = savedMessage.conversation.id!!,
                senderId = authenticatedUserId,
                senderName = senderName,
                content = savedMessage.content!!,
                timestamp = savedMessage.created_at
            )

            println("✓ Broadcasting message to /topic/conversation/${message.conversationId}")

            messagingTemplate.convertAndSend(
                "/topic/conversation/${message.conversationId}",
                response
            )

            println("=== Message sent successfully ===")

        } catch (e: Exception) {
            println("❌ ERROR saving or broadcasting message: ${e.message}")
            e.printStackTrace()
            throw IllegalStateException("Failed to save message: ${e.message}")
        }
    }

    fun sendPrivateMessage(userId: Int, message: Any) {
        messagingTemplate.convertAndSend("/queue/user/$userId", message)
    }
}