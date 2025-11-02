package com.chatapp.demo.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Embeddable
data class ConversationParticipantId(
    @Column(name = "conversation_id")
    var conversationId: Int = 0,

    @Column(name = "user_id")
    var userId: Int = 0
) : java.io.Serializable


@Entity
@Table(name = "conversation_participants")
open class ConversationParticipant(
    @EmbeddedId
    open var id: ConversationParticipantId = ConversationParticipantId(),
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversationId")
    @JoinColumn(name = "conversation_id")
open var conversation: conversation? = null,

    // Maps id.userId -> user_id FK
     @ManyToOne(fetch = FetchType.LAZY)
     @MapsId("userId")
     @JoinColumn(name = "user_id")
open var user: User? = null,
    @Column(name="role")
    var role:String?,

    @Column(name="status")
    var status:String,

    @Column(name="join_at")
    var joinat: LocalDateTime= LocalDateTime.now(),

    @Column(name = "custom_name")
    var customName: String? = null
)