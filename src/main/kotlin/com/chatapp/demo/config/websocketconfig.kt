package com.chatapp.demo.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.HandshakeInterceptor

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val webSocketAuthInterceptor: WebSocketAuthInterceptor,
    private val httpHandshakeInterceptor: HttpHandshakeInterceptor
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue")
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .addInterceptors(httpHandshakeInterceptor)
            .withSockJS()
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(webSocketAuthInterceptor)
    }
}

@Component
class HttpHandshakeInterceptor : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        if (request is ServletServerHttpRequest) {
            val session = request.servletRequest.session
            val userId = session.getAttribute("userId")

            println("=== WebSocket Handshake ===")
            println("userId from HTTP session: $userId (type: ${userId?.javaClass?.simpleName})")

            if (userId != null) {
                val userIdInt = when (userId) {
                    is Long -> userId.toInt()
                    is Int -> userId
                    is String -> userId.toIntOrNull()
                    else -> null
                }

                if (userIdInt != null) {
                    attributes["userId"] = userIdInt
                    println("✓ userId transferred to WebSocket session: $userIdInt")
                } else {
                    println("⚠ Could not convert userId to Int: $userId")
                }
            } else {
                println("⚠ No userId found in HTTP session during handshake")
            }
        }

        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
    }
}

@Component
class WebSocketAuthInterceptor : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (accessor != null && StompCommand.CONNECT == accessor.command) {
            val sessionAttributes = accessor.sessionAttributes

            println("=== STOMP CONNECT ===")
            println("Session attributes: $sessionAttributes")

            // Get userId - it should be Int from our handshake interceptor
            val userId = sessionAttributes?.get("userId")

            println("userId from WebSocket session: $userId (type: ${userId?.javaClass?.simpleName})")

            val userIdInt = when (userId) {
                is Int -> userId
                is Long -> userId.toInt()
                is String -> userId.toIntOrNull()
                else -> null
            }

            if (userIdInt != null) {
                val authToken = UsernamePasswordAuthenticationToken(
                    userIdInt.toString(),
                    null,
                    emptyList()
                )

                accessor.user = authToken
                SecurityContextHolder.getContext().authentication = authToken

                println("✓ User authenticated for WebSocket: $userIdInt")
            } else {
                println("⚠ WARNING: No valid userId found - connection will be rejected")
                println("⚠ This means the user is not logged in or session expired")

                return null
            }
        }

        return message
    }
}