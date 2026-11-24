package com.example.security

import java.security.SecureRandom

object SessionSecurity {
  private val random = SecureRandom()
  private const val TOKEN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
  const val SUPPORT_LINK_TTL_MILLIS = 60 * 60 * 1000L // 1 hour

  fun generateJoinToken(length: Int = 16): String {
    val sb = StringBuilder(length)
    repeat(length) {
      sb.append(TOKEN_CHARS[random.nextInt(TOKEN_CHARS.length)])
    }
    return sb.toString()
  }

  fun buildSupportLink(sessionId: String, joinToken: String): String {
    return "https://sadiconnect.app/connect/$sessionId?token=$joinToken"
  }

  fun isSessionExpired(expiresAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
    return expiresAtMillis <= nowMillis
  }
}
