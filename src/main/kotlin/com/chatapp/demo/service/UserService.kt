package com.chatapp.demo.service

import com.chatapp.demo.model.User
import com.chatapp.demo.respository.UserRepo
import org.springframework.stereotype.Service
import com.chatapp.demo.service.ValidationHelpers
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.io.File
import java.io.FileNotFoundException

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateResourceException(message: String) : RuntimeException(message)

@Service
class UserService(private val usereRepo: UserRepo, private val passwordencoder:PasswordEncoder){

    fun logIn(email:String?,phone:String?,password:String):User {
        if(email==null && phone==null){
            throw IllegalArgumentException("You should log in using either your phone number or your email")
        }
        val user=usereRepo.findByEmailOrPhone(email,phone)
            .orElseThrow{ IllegalArgumentException("No user found with this Phone number or Email")}

        if(!passwordencoder.matches(password, user.password)){
            throw IllegalArgumentException("Incorrect password")
        }
        return user

    }


    fun signUp(username:String=".",email:String,phone:String,password:String):User{
        var username= ValidationHelpers.generalHelpers.validatename(username)
        ValidationHelpers.FieldHelper.validateEmail(email){usereRepo.existsByEmail(email)}
        ValidationHelpers.FieldHelper.validatePhone(phone){usereRepo.existsByPhone(phone)}
        ValidationHelpers.FieldHelper.validatePassword(password)
        val user= User(username=username,email=email,phone=phone,password=passwordencoder.encode(password))

        try {
            return usereRepo.save(user)
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateResourceException("Phone number Or email already exists ")
        }
    }

    fun finduserById(id:Int):User{
        return usereRepo.findById(id).orElseThrow{
            IllegalArgumentException("no user found")
        }
    }

    fun findUserByEmailOrPhone(email: String,phone:String?): User {
        return usereRepo.findByEmailOrPhone(email,phone).orElseThrow(

        )
    }

    fun findByUsername(username:String):User{
        return usereRepo.findByUsername(username).orElseThrow(

        )
    }

    fun getAllusers():MutableList<User>{
        return usereRepo.findAll()
    }


    fun updateUsername(userId:Int,newusername:String):User{
        ValidationHelpers.generalHelpers.validatename(newusername)
        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        user.username=newusername
        return usereRepo.save(user)
    }

    fun updateEmail(userId:Int,newemail:String):User{

        ValidationHelpers.FieldHelper.validateEmail(newemail){usereRepo.existsByEmail(newemail)}
        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        user.email=newemail
        return usereRepo.save(user)
    }

    fun updatePhone(userId:Int,newPhoneNumber:String):User{
        ValidationHelpers.FieldHelper.validatePhone(newPhoneNumber){usereRepo.existsByPhone(newPhoneNumber)}

        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        user.phone=newPhoneNumber
        return usereRepo.save(user)
    }

    fun updatePassword(userId:Int,newPassword:String):User{
        ValidationHelpers.FieldHelper.validatePassword(newPassword)
        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        user.password= passwordencoder.encode(newPassword)
        return usereRepo.save(user)
    }

    fun updateProfileimage(userId:Int,imagePath:String):User{
        val file = File(imagePath)
        if(!file.exists() || !file.isFile){
            throw FileNotFoundException("File not found at path: $imagePath")
        }
        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        user.profile_image=imagePath
        return usereRepo.save(user)
    }

    fun deleteUser(userId:Int):Boolean{
        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        usereRepo.delete(user)
        return true
    }

    fun deleteProfileimage(userId:Int):User{
        val user=usereRepo.findById(userId).orElseThrow{IllegalArgumentException("User not found with id: $userId")}
        user.profile_image=null
        return usereRepo.save(user)
    }
}