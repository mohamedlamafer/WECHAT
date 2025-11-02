package com.chatapp.demo.controller

import com.chatapp.demo.dto.*
import com.chatapp.demo.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    @PostMapping("/signUp")
    fun createUser(@RequestBody request: CreateUserRequest, servletReq: HttpServletRequest): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.signUp(request.username, request.email, request.phone, request.password)

        // rotate session after sign-up
        servletReq.getSession(false)?.invalidate()
        val session = servletReq.getSession(true)
        session.setAttribute("userId", user.id)
        session.maxInactiveInterval = 30 * 60

        println("=== User Signed Up ===")
        println("Session ID: ${session.id}")
        println("User ID stored: ${user.id}")

        val response = UserResponse(
            id = user.id!!,
            username = user.username,
            email = user.email,
            phone = user.phone
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(true, "User created successfully", response))
    }


    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest, servletReq: HttpServletRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        val user = userService.logIn(request.email, request.phone, request.password)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse(false, "Invalid credentials", null))

        // rotate session after login
        servletReq.getSession(false)?.invalidate()
        val session = servletReq.getSession(true)
        session.setAttribute("userId", user.id)
        session.maxInactiveInterval = 30 * 60

        println("=== User Logged In ===")
        println("Session ID: ${session.id}")
        println("User ID stored: ${user.id} (type: ${user.id!!.javaClass.simpleName})")
        println("Session userId retrieved: ${session.getAttribute("userId")}")

        val response = LoginResponse(
            id = user.id!!,
            username = user.username,
            email = user.email,
            phone = user.phone,
            message = "Login successful"
        )

        return ResponseEntity.ok(ApiResponse(true, "Login successful", response))
    }

    @GetMapping("/current")
    fun getCurrentUser(servletReq: HttpServletRequest): ResponseEntity<ApiResponse<UserResponse>> {
        val session = servletReq.getSession(false)

        println("=== Get Current User ===")
        println("Session: ${session?.id ?: "No session"}")

        if (session == null) {
            println("❌ No active session")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse(false, "Not authenticated", null))
        }

        val userId = getSessionUserId(servletReq)
        println("userId from session: $userId")

        if (userId == null) {
            println("❌ No userId in session - user not authenticated")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse(false, "Not authenticated", null))
        }

        val user = userService.finduserById(userId)
        println("✓ Current user: ${user.username} (ID: ${user.id})")

        val response = UserResponse(
            id = user.id!!,
            username = user.username,
            email = user.email,
            phone = user.phone
        )

        return ResponseEntity.ok(ApiResponse(true, "User retrieved successfully", response))
    }

    @PostMapping("/logout")
    fun logout(servletReq: HttpServletRequest): ResponseEntity<ApiResponse<Unit>> {
        // optional: close websocket sessions for this user if you have a ConnectionManager
        servletReq.getSession(false)?.invalidate()
        return ResponseEntity.ok(ApiResponse(true, "Logged out", null))
    }

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Int): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.finduserById(id)
        val response = UserResponse(
            id = user.id!!,
            username = user.username,
            email = user.email,
            phone = user.phone
        )
        return ResponseEntity.ok(ApiResponse(true, "User retrieved successfully", response))
    }

    @PutMapping("/username")
    fun updateUsername(
        servletReq: HttpServletRequest,
        @RequestBody request: UpdateUsername
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        userService.updateUsername(userId, request.username)
        return ResponseEntity.ok(ApiResponse(true, "Username updated successfully", null))
    }

    @PutMapping("/email")
    fun updateEmail(
        servletReq: HttpServletRequest,
        @RequestBody request: UpdateEmail
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        userService.updateEmail(userId, request.email)
        return ResponseEntity.ok(ApiResponse(true, "Email updated successfully", null))
    }

    @PutMapping("/phone")
    fun updatePhone(
        servletReq: HttpServletRequest,
        @RequestBody request: UpdatePhone
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        userService.updatePhone(userId, request.phone)
        return ResponseEntity.ok(ApiResponse(true, "Phone updated successfully", null))
    }

    @PutMapping("/password")
    fun updatePassword(
        servletReq: HttpServletRequest,
        @RequestBody request: UpdatePassword
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = getSessionUserId(servletReq)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(false, "Not authenticated", null))

        if (request.password != request.confirmedPassword) {
            throw IllegalArgumentException("Passwords don't match")
        }

        userService.updatePassword(userId, request.password)
        return ResponseEntity.ok(ApiResponse(true, "Password updated successfully", null))
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Int): ResponseEntity<ApiResponse<Unit>> {
        userService.deleteUser(id)
        return ResponseEntity.ok(ApiResponse(true, "User deleted successfully", null))
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