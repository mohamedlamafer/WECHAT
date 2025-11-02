package com.chatapp.demo.service

import org.apache.commons.validator.routines.EmailValidator
import com.google.i18n.phonenumbers.PhoneNumberUtil

class ValidationHelpers {

    object FieldHelper{
        val Emailvalidator = EmailValidator.getInstance()
        val Phonevalidator=PhoneNumberUtil.getInstance()

        fun validateEmail( email: String ,action: () -> Boolean ) {
            if (!Emailvalidator.isValid(email)) {
                throw IllegalArgumentException("Invalid email format. Example: example@gmail.com")
            }

            if (action()) {
                throw IllegalArgumentException("Email already registered")
            }
        }

        fun cleanPhoneNumber(phone: String): String {
            return phone.replace(Regex("[\\s\\-().]"), "")
        }

        fun validatePhone(phone: String ,action: () -> Boolean) {
            if (action()) {
                throw IllegalArgumentException("Phone already registered")
            }
            val cleanedPhone = cleanPhoneNumber(phone)
            try {
                val phoneNumber = Phonevalidator.parse(cleanedPhone, null)
                if (!Phonevalidator.isValidNumber(phoneNumber)) {
                    throw IllegalArgumentException("Invalid phone number")
                }
            } catch (e:Exception) {
                throw IllegalArgumentException("Invalid phone number format ")
            }
        }

        fun validatePassword(password: String) {
            val passwordRegex = Regex(
                "^(?=.*[0-9])" +
                        "(?=.*[a-z])" +
                        "(?=.*[A-Z])" +
                        "(?=.*[!@#\$%^&*()_+\\-={}\\[\\]|\\\\:;\"'<>,.?/~`])" +
                        ".{8,20}\$"
            )
            if (!password.matches(passwordRegex)) {
                throw IllegalArgumentException(
                    "Password must include uppercase, lowercase, digit, special character and at least 8 characters"
                )
            }
        }

    }

    object generalHelpers{
        fun validatename(name: String,
//                         validator:((String)->Boolean)?,
//                         error:((String)->Boolean)?
        ): String {
        val usern = name.trim()
        if (usern.isEmpty()) {
            throw IllegalArgumentException("Username must contain at least one character")
        }
//        if(validator!=null && validator(name)){
//            throw IllegalArgumentException(error)
//        }
        return usern
    }
    }
}