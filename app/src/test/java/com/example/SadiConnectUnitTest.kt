package com.example

import com.example.domain.model.AppLanguage
import com.example.security.SessionSecurity
import com.example.utils.AppStrings
import com.example.utils.DeviceCodeUtils
import com.example.utils.QrCodeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SadiConnectUnitTest {

  @Test
  fun deviceCodeGenerationAndFormatting() {
    val code = DeviceCodeUtils.generateNineDigitCode()
    assertEquals(9, code.length)
    assertTrue(code.all { it.isDigit() })

    val formatted = DeviceCodeUtils.formatCode("469752988")
    assertEquals("469-752-988", formatted)
  }

  @Test
  fun supportLinkAndCodeParsing() {
    val fromCode = DeviceCodeUtils.parseConnectInput("469-752-988")
    assertNotNull(fromCode)
    assertEquals("469752988", fromCode?.deviceCode)
    assertNull(fromCode?.sessionId)

    val fromUrl = DeviceCodeUtils.parseConnectInput("https://sadiconnect.app/connect/sess_abc123?token=tok_xyz")
    assertNotNull(fromUrl)
    assertEquals("sess_abc123", fromUrl?.sessionId)
    assertEquals("tok_xyz", fromUrl?.token)

    val fromCustomScheme = DeviceCodeUtils.parseConnectInput("sadiconnect://connect/sess_999?token=t123")
    assertNotNull(fromCustomScheme)
    assertEquals("sess_999", fromCustomScheme?.sessionId)
    assertEquals("t123", fromCustomScheme?.token)

    assertNull(DeviceCodeUtils.parseConnectInput("12345"))
  }

  @Test
  fun sessionSecurityTokensAndExpiry() {
    val token1 = SessionSecurity.generateJoinToken()
    val token2 = SessionSecurity.generateJoinToken()
    assertEquals(16, token1.length)
    assertTrue(token1 != token2)

    val now = 1_700_000_000_000L
    assertFalse(SessionSecurity.isSessionExpired(now + 60_000L, now))
    assertTrue(SessionSecurity.isSessionExpired(now - 1_000L, now))
  }

  @Test
  fun bilingualStringsArePopulated() {
    assertTrue(AppStrings.generateSupportLink.get(AppLanguage.BN).isNotBlank())
    assertTrue(AppStrings.generateSupportLink.get(AppLanguage.EN).isNotBlank())
    assertTrue(AppStrings.remoteControl.get(AppLanguage.BN) != AppStrings.remoteControl.get(AppLanguage.EN))
  }

  @Test
  fun qrCodeMatrixEncodesWithoutCrash() {
    // Pure JVM check on invalid empty input returns null safely
    assertNull(QrCodeUtils.generateQrBitmap(""))
  }
}
