package com.expensetracker.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordHasher @Inject constructor() {
    private val random = SecureRandom()
    private val iterations = 120_000
    private val keyLength = 256

    fun generateSalt(): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        return hash(password, salt) == expectedHash
    }
}

@Singleton
class OtpGenerator @Inject constructor(private val passwordHasher: PasswordHasher) {
    private val random = SecureRandom()

    fun generateCode(): String = (100_000 + random.nextInt(900_000)).toString()

    fun hashCode(code: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.encodeToString(digest.digest(code.toByteArray()), Base64.NO_WRAP)
    }

    fun verifyCode(code: String, hash: String): Boolean = hashCode(code) == hash
}

object InputValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val MOBILE_REGEX = Regex("^\\+?[0-9]{8,15}$")

    fun isValidEmail(email: String): Boolean = email.matches(EMAIL_REGEX)
    fun isValidMobile(mobile: String): Boolean = mobile.matches(MOBILE_REGEX)
    fun isValidPassword(password: String): Boolean = password.length >= 8
    fun isValidAmount(amount: Double): Boolean = amount > 0 && amount < 1_000_000_000
    fun sanitize(input: String): String = input.trim().replace(Regex("[<>\"';]"), "")
}
