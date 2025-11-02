package com.chatapp.demo.controller

import com.chatapp.demo.dto.*
import com.chatapp.demo.service.ConversationService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/conversations/{conversationId}/participants")
class ParticipantController(
    private val conversationService: ConversationService
) {

    @PostMapping
    fun addParticipant(
        servletReq: HttpServletRequest,
        @PathVariable conversationId: Int,
        @RequestBody request: AddParticipantRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.addParticipant(actorId, conversationId, request.userId, request.role)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(true, "Participant added successfully", null))
    }


    @GetMapping
    fun getConversationParticipants(
        @PathVariable conversationId: Int
    ): ResponseEntity<ApiResponse<List<ParticipantResponse>>> {
        val participants = conversationService.getConversationParticipants(conversationId).map { p ->
            ParticipantResponse(
                id = p.id!!,
                userId = p.user?.id,
                username = p.user?.username,
                role = p.role,
                status = p.status,
                joinedAt = p.joinat
            )
        }

        return ResponseEntity.ok(ApiResponse(true, "Participants retrieved successfully", participants))
    }

    @DeleteMapping("/{targetUserId}")
    fun removeParticipant(
        servletReq: HttpServletRequest,
        @PathVariable conversationId: Int,
        @PathVariable targetUserId: Int
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.deleteParticipant(actorId, conversationId, targetUserId)
        return ResponseEntity.ok(ApiResponse(true, "Participant removed successfully", null))
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