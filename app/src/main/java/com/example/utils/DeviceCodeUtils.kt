package com.example.utils

import java.security.SecureRandom

object DeviceCodeUtils {
  private val secureRandom = SecureRandom()

  fun generateNineDigitCode(): String {
    val num = 100_000_000 + secureRandom.nextInt(900_000_000)
    return num.toString()
  }

  fun formatCode(rawCode: String): String {
    val digits = rawCode.filter { it.isDigit() }
    return if (digits.length == 9) {
      "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6, 9)}"
    } else {
      rawCode
    }
  }

  fun normalizeDigits(input: String): String = input.filter { it.isDigit() }

  data class ParsedConnectInput(
    val deviceCode: String? = null,
    val sessionId: String? = null,
    val token: String? = null,
  )

  fun parseConnectInput(rawInput: String): ParsedConnectInput? {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank()) return null

    // Support Link formats:
    // https://sadiconnect.app/connect/SESSION_ID?token=TOKEN
    // sadiconnect://connect/SESSION_ID?token=TOKEN
    if (trimmed.contains("/connect/") || trimmed.startsWith("sadiconnect://")) {
      val afterConnect = trimmed.substringAfter("/connect/", "")
        .ifBlank { trimmed.substringAfter("sadiconnect://connect/", "") }
      val sessionPart = afterConnect.substringBefore("?").trim('/')
      val tokenPart = afterConnect.substringAfter("token=", "").substringBefore("&").ifBlank { null }
      if (sessionPart.isNotBlank()) {
        return ParsedConnectInput(sessionId = sessionPart, token = tokenPart)
      }
    }

    // Direct 9-digit device code (with or without dashes/spaces)
    val digits = normalizeDigits(trimmed)
    if (digits.length == 9) {
      return ParsedConnectInput(deviceCode = digits)
    }
    return null
  }
}
