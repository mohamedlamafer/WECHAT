package com.chatapp.demo.controller

import com.chatapp.demo.dto.*
import com.chatapp.demo.service.ConversationService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
class MessageController(
    private val conversationService: ConversationService
) {

    @PostMapping
    fun sendMessage(
        servletReq: HttpServletRequest,
        @PathVariable conversationId: Int,
        @RequestBody request: CreateMessage
    ): ResponseEntity<out ApiResponse<out Any>?> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        val message = conversationService.addTextMessage(
            actorId = actorId,
            conversationId = conversationId,
            content = request.content,
            sentAt = LocalDateTime.now()
        )

        val response = MessageResponse(
            id = message.id!!,
            senderId = message.sender.id,
            content = message.content,
            createdAt = message.created_at
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(true, "Message sent successfully", response))
    }

    @GetMapping
    fun getConversationMessages(
        @PathVariable conversationId: Int
    ): ResponseEntity<ApiResponse<List<com.chatapp.demo.dto.MessageResponse>>?> {
        val messages = conversationService.getConversationMessages(conversationId).map { msg ->
            MessageResponse(
                id = msg.id!!,
                senderId = msg.sender?.id,
                content = msg.content,
                createdAt = msg.created_at,
            )
        }

        return ResponseEntity.ok(ApiResponse(true, "Messages retrieved successfully", messages))
    }

    @DeleteMapping("/{messageId}")
    fun deleteMessage(
        servletReq: HttpServletRequest,
        @PathVariable conversationId: Int,
        @PathVariable messageId: Int
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.deleteMessage(messageId, actorId)
        return ResponseEntity.ok(ApiResponse(true, "Message deleted successfully", null))
    }

    @DeleteMapping("/{messageId}/admin")
    fun adminDeleteMessage(
        servletReq: HttpServletRequest,
        @PathVariable conversationId: Int,
        @PathVariable messageId: Int
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.deleteMessageAsAdmin(actorId, messageId)
        return ResponseEntity.ok(ApiResponse(true, "Message deleted by admin successfully", null))
    }

    private fun getSessionUserId(servletReq: HttpServletRequest): Int? {
        val raw = servletReq.getSession(false)?.getAttribute("userId") ?: return null
        return when (raw) {
            is Int -> raw
            is Long -> raw.toInt()
            is String -> raw.toIntOrNull()
            else -> null
        }
    }
}
