package com.chatapp.demo.model
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name="message")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class message(
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    var id:Int?=null,
    @Column(name="created_at")
    var created_at:LocalDateTime=LocalDateTime.now(),
    @Column(name="type")
    var type:String,

    @ManyToOne
    @JoinColumn(name="conversation_id")
    var conversation:conversation,

    @ManyToOne
    @JoinColumn(name="sender_id")
    var sender:User
)