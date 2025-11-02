package com.chatapp.demo

import com.chatapp.demo.model.User
import com.chatapp.demo.service.ConversationService
import com.chatapp.demo.respository.UserRepo
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootApplication
class DemoApplication {

}

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}
