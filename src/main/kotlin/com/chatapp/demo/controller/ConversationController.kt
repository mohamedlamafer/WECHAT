package com.chatapp.demo.controller

import com.chatapp.demo.dto.*
import com.chatapp.demo.service.ConversationService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/conversations")
class ConversationController(
    private val conversationService: ConversationService
) {

    @PostMapping("/private")
    fun createPrivateConversation(
        servletReq: HttpServletRequest,
        @RequestBody request: CreatePrivateConversationRequest
    ): ResponseEntity<ApiResponse<ConversationResponse>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        val conversation = conversationService.creatPrivateConversation(
            userId1 = actorId,
            userId2 = request.otherUserId,
            type = "private",
            custom_name1 = request.customName
        )

        val response = ConversationResponse(
            id = conversation.id!!,
            type = conversation.type,
            name = conversation.name,
            createdAt = conversation.created_at,
            participantCount = conversation.participants.size
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(true, "Private conversation created successfully", response))
    }

    @PostMapping("/group")
    fun createGroupConversation(
        servletReq: HttpServletRequest,
        @RequestBody request: CreateGroupConversationRequest
    ): ResponseEntity<ApiResponse<ConversationResponse>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        val conversation = conversationService.creatGroupConversation(
            name = request.name,
            creatorId = actorId,
            type = "group"
        )

        val response = ConversationResponse(
            id = conversation.id!!,
            type = conversation.type,
            name = conversation.name,
            createdAt = conversation.created_at,
            participantCount = conversation.participants.size
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(true, "Group conversation created successfully", response))
    }

    @GetMapping("/{id}")
    fun getConversationById(@PathVariable id: Int): ResponseEntity<ApiResponse<ConversationResponse>> {
        val conversation = conversationService.getConversationById(id)

        val response = ConversationResponse(
            id = conversation.id!!,
            type = conversation.type,
            name = conversation.name,
            createdAt = conversation.created_at,
            participantCount = conversation.participants.size
        )

        return ResponseEntity.ok(ApiResponse(true, "Conversation retrieved successfully", response))
    }

    @GetMapping("/search/users")
    fun searchUsers(@RequestParam query: String): ResponseEntity<ApiResponse<List<UserResponse>>> {
        val users = conversationService.searchUsers(query)
        val responses = users.map { UserResponse(it.id!!, it.username, it.email, it.phone) }
        return ResponseEntity.ok(ApiResponse(true, "Users found", responses))
    }

    @GetMapping("/search/groups")
    fun searchGroups(@RequestParam query: String): ResponseEntity<ApiResponse<List<ConversationResponse>>> {
        val groups = conversationService.searchGroups(query)
        val responses = groups.map { ConversationResponse(it.id!!, it.type, it.name, it.created_at, it.participants.size) }
        return ResponseEntity.ok(ApiResponse(true, "Groups found", responses))
    }

    @GetMapping("/user")
    fun getUserConversations(
        servletReq: HttpServletRequest
    ): ResponseEntity<ApiResponse<List<ConversationResponse>>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        val conversations = conversationService.getUserConversations(actorId).map { conv ->
            ConversationResponse(
                id = conv.id!!,
                type = conv.type,
                name = conv.name,
                createdAt = conv.created_at,
                participantCount = conv.participants.size
            )
        }

        return ResponseEntity.ok(ApiResponse(true, "Conversations retrieved successfully", conversations))
    }

    @GetMapping("/chats")
    fun getUserChats(
        servletReq: HttpServletRequest
    ): ResponseEntity<ApiResponse<List<ConversationResponse>>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        val conversations = conversationService.getUserChats(actorId).map { conv ->
            ConversationResponse(
                id = conv.id!!,
                type = conv.type,
                name = conv.name,
                createdAt = conv.created_at,
                participantCount = conv.participants.size
            )
        }
        return ResponseEntity.ok(ApiResponse(true, "Chats retrieved successfully", conversations))
    }

    @GetMapping("/contacts")
    fun getUserContacts(
        servletReq: HttpServletRequest
    ): ResponseEntity<ApiResponse<List<ConversationResponse>>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        val conversations = conversationService.getUserContacts(actorId).map { conv ->
            ConversationResponse(
                id = conv.id!!,
                type = conv.type,
                name = conv.name,
                createdAt = conv.created_at,
                participantCount = conv.participants.size
            )
        }

        return ResponseEntity.ok(ApiResponse(true, "Contacts retrieved successfully", conversations))
    }

    @PatchMapping("/participants/block")
    fun blockParticipant(
        servletReq: HttpServletRequest,
        @RequestBody request: BlockingOptions
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.blockUser(actorId = actorId, conversationId = request.conversationId, userId = request.targetUserId)
        return ResponseEntity.ok(ApiResponse(true, "User blocked successfully", null))
    }

    @PatchMapping("/participants/deblock")
    fun deblockParticipant(
        servletReq: HttpServletRequest,
        @RequestBody request: BlockingOptions
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.deblockUser(actorId, request.conversationId, request.targetUserId)
        return ResponseEntity.ok(ApiResponse(true, "User deblocked successfully", null))
    }

    @PutMapping("/{id}/name")
    fun updateGroupName(
        servletReq: HttpServletRequest,
        @RequestBody request: UpdateGroupNameRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.updateGroupConversationName(actorId, request.conversationId, request.name)
        return ResponseEntity.ok(ApiResponse(true, "Group name updated successfully", null))
    }

    @DeleteMapping("/participant")
    fun deleteConversation(
        servletReq: HttpServletRequest,
        @RequestBody request: DeleteParticipantConversation
    ): ResponseEntity<ApiResponse<Unit>> {
        val actorId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        conversationService.deleteUserConversation(actorId, request.conversationId, request.targetUserId)
        return ResponseEntity.ok(ApiResponse(true, "Conversation participant removed successfully", null))
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
