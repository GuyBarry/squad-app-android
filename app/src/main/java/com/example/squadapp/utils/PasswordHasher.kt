package com.example.squadapp.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    /**
     * Hashes a password using PBKDF2 with a randomly generated salt
     * @param password The plain text password to hash
     * @return A string containing the salt and hash separated by ":"
     */
    fun hashPassword(password: String): String {
        // Generate random salt
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)

        // Hash the password
        val hash = hashPBKDF2(password, salt)

        // Encode salt and hash to Base64
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        // Return salt:hash format
        return "$saltBase64:$hashBase64"
    }

    /**
     * Verifies a password against a stored hash
     * @param password The plain text password to verify
     * @param storedHash The stored hash in format "salt:hash"
     * @return true if the password matches, false otherwise
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        return try {
            // Split stored hash into salt and hash
            val parts = storedHash.split(":")
            if (parts.size != 2) {
                return false
            }

            val saltBase64 = parts[0]
            val hashBase64 = parts[1]

            // Decode from Base64
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val originalHash = Base64.decode(hashBase64, Base64.NO_WRAP)

            // Hash the provided password with the same salt
            val testHash = hashPBKDF2(password, salt)

            // Compare hashes using constant-time comparison
            MessageDigest.isEqual(originalHash, testHash)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Hashes a password using PBKDF2
     * @param password The password to hash
     * @param salt The salt to use
     * @return The hashed password as a byte array
     */
    private fun hashPBKDF2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
}


