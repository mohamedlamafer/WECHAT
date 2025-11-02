package com.chatapp.demo.model
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name="conversations")
class conversation(
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    var id:Int?=null,
    @Column(name="type")
    val type:String,
    @Column(name="name")
    var name:String?,
    @Column(name="created_at")
    val created_at:LocalDateTime=LocalDateTime.now(),
    @Column(name="lastMessageAt")
    var lastMessageAt: LocalDateTime? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    val created_by: User,
    @OneToMany(mappedBy = "conversation",cascade = [CascadeType.PERSIST, CascadeType.MERGE], orphanRemoval = true)
    val participants: MutableList<ConversationParticipant> = mutableListOf(),

    @OneToMany(mappedBy = "conversation", cascade = [CascadeType.PERSIST, CascadeType.MERGE], orphanRemoval = true)
    var messages: MutableList<message> = mutableListOf()

)
