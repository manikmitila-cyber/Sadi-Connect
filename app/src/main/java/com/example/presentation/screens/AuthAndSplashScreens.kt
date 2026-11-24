package com.example.presentation.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.domain.model.AppLanguage
import com.example.firebase.FirebaseAuthHelper
import com.example.presentation.components.LanguageTogglePill
import com.example.presentation.components.SadiBrandLogo
import com.example.presentation.components.SadiCard
import com.example.presentation.components.SadiWaveBackground
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.OnlineGreen
import com.example.utils.AppStrings
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  language: AppLanguage,
  onFinished: () -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "splash_ring")
  val sweepAngle by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
      label = "ring_rotation",
    )

  LaunchedEffect(Unit) {
    delay(1100L)
    onFinished()
  }

  SadiWaveBackground(darkTheme = true) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .safeDrawingPadding()
          .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      SadiBrandLogo(size = 112.dp)
      Spacer(Modifier.height(26.dp))
      Text(
        text =
          buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
              append("Sadi ")
            }
            withStyle(SpanStyle(color = ElectricBlueBright, fontWeight = FontWeight.Bold)) {
              append("Connect")
            }
          },
        style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp),
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = AppStrings.tagline.get(language).uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
        color = Color(0xFFCBD5E1),
      )
      Spacer(Modifier.height(48.dp))
      Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          drawArc(
            color = ElectricBlue.copy(alpha = 0.25f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx()),
          )
          drawArc(
            brush = Brush.sweepGradient(listOf(Color.Transparent, ElectricBlueBright, CyanGlow)),
            startAngle = sweepAngle,
            sweepAngle = 220f,
            useCenter = false,
            style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round),
          )
        }
      }
      Spacer(Modifier.height(28.dp))
      Text(
        text = AppStrings.splashSubtitle.get(language),
        style = MaterialTheme.typography.bodyLarge,
        color = Color(0xFF94A3B8),
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
fun AuthScreen(
  language: AppLanguage,
  onLanguageChange: (AppLanguage) -> Unit,
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    FirebaseAuthHelper.attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  SadiWaveBackground(darkTheme = true) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .safeDrawingPadding()
          .padding(24.dp),
    ) {
      LanguageTogglePill(
        current = language,
        onSelect = onLanguageChange,
        modifier = Modifier.align(Alignment.TopEnd),
      )

      Column(
        modifier =
          Modifier
            .align(Alignment.Center)
            .widthIn(max = 480.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        SadiBrandLogo(size = 96.dp)
        Spacer(Modifier.height(20.dp))
        Text(
          text =
            buildAnnotatedString {
              withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                append("Sadi ")
              }
              withStyle(SpanStyle(color = ElectricBlueBright, fontWeight = FontWeight.Bold)) {
                append("Connect")
              }
            },
          style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text = AppStrings.tagline.get(language),
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFF94A3B8),
        )
        Spacer(Modifier.height(28.dp))

        SadiCard {
          Text(
            text = AppStrings.signInTitle.get(language),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = AppStrings.signInSubtitle.get(language),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
          )
          Spacer(Modifier.height(20.dp))

          Button(
            onClick = {
              isLoading = true
              errorMessage = null
              FirebaseAuthHelper.onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                scope = scope,
                onAuthCancelled = { isLoading = false },
              )
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier =
              Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("google_sign_in_button"),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp,
              )
            } else {
              Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = Color.White)
              Spacer(Modifier.width(10.dp))
              Text(
                text = AppStrings.signInWithGoogle.get(language),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
              )
            }
          }

          if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.errorContainer,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
            ) {
              Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
        }

        Spacer(Modifier.height(18.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = OnlineGreen,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = AppStrings.securityBadgeNote.get(language),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8),
          )
        }
      }
    }
  }
}
