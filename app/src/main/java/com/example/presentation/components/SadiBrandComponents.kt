package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.domain.model.RemoteSession
import com.example.ui.theme.CardBorderNavy
import com.example.ui.theme.CardNavy
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DeepBlackBg
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.DisconnectRed
import com.example.ui.theme.DisconnectRedBg
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueBright
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.OnlineGreenBg
import com.example.utils.AppStrings

@Composable
fun SadiWaveBackground(
  darkTheme: Boolean = true,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (darkTheme) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
          brush =
            Brush.verticalGradient(
              colors = listOf(DeepBlackBg, DeepNavyBg, DeepBlackBg)
            )
        )
        // Top & bottom subtle glowing electric-blue wave arcs
        val w = size.width
        val h = size.height
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(ElectricBlue.copy(alpha = 0.18f), Color.Transparent),
              center = Offset(w * 0.5f, h * 0.16f),
              radius = w * 0.7f,
            ),
          radius = w * 0.7f,
          center = Offset(w * 0.5f, h * 0.16f),
        )
        for (i in 0..3) {
          val yOffset = h * (0.78f + i * 0.035f)
          val path =
            Path().apply {
              moveTo(0f, yOffset)
              cubicTo(
                w * 0.3f,
                yOffset - 60f - i * 12f,
                w * 0.7f,
                yOffset + 55f + i * 10f,
                w,
                yOffset - 20f,
              )
            }
          drawPath(
            path = path,
            color = CyanGlow.copy(alpha = 0.16f - i * 0.03f),
            style = Stroke(width = 2.5f),
          )
        }
      }
    } else {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
      )
    }
    content()
  }
}

@Composable
fun SadiBrandLogo(
  size: Dp = 64.dp,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .size(size)
        .clip(RoundedCornerShape(size * 0.28f))
        .border(
          width = 1.5.dp,
          brush = Brush.linearGradient(listOf(ElectricBlueBright, ElectricBlue)),
          shape = RoundedCornerShape(size * 0.28f),
        ),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = R.drawable.sadi_connect_icon_1795454668335),
      contentDescription = "Sadi Connect Logo",
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
    )
  }
}

@Composable
fun SadiCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = RoundedCornerShape(18.dp)
  val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
  val bg = MaterialTheme.colorScheme.surfaceVariant
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier),
    shape = shape,
    color = bg,
    border = BorderStroke(1.dp, borderColor),
    tonalElevation = 2.dp,
  ) {
    Column(modifier = Modifier.padding(14.dp), content = content)
  }
}

@Composable
fun LanguageTogglePill(
  current: AppLanguage,
  onSelect: (AppLanguage) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.testTag("language_toggle_pill"),
    shape = RoundedCornerShape(50),
    color = CardNavy,
    border = BorderStroke(1.dp, CardBorderNavy),
  ) {
    Row(
      modifier = Modifier.padding(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf(AppLanguage.BN to "বাং", AppLanguage.EN to "Eng").forEach { (lang, label) ->
        val selected = current == lang
        Box(
          modifier =
            Modifier
              .clip(RoundedCornerShape(50))
              .background(if (selected) ElectricBlue else Color.Transparent)
              .clickable { onSelect(lang) }
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag("lang_btn_${lang.code}"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
fun StatusPillBadge(
  text: String,
  positive: Boolean,
  modifier: Modifier = Modifier,
) {
  val bg = if (positive) OnlineGreenBg else DisconnectRedBg
  val fg = if (positive) OnlineGreen else DisconnectRed
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = bg,
    border = BorderStroke(1.dp, fg.copy(alpha = 0.45f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(fg))
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
fun IncomingSessionApprovalDialog(
  session: RemoteSession,
  language: AppLanguage,
  onAccept: (allowScreen: Boolean, allowTouch: Boolean) -> Unit,
  onReject: () -> Unit,
) {
  var allowScreen by remember(session.id) { mutableStateOf(true) }
  var allowTouch by remember(session.id) { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = {},
    containerColor = MaterialTheme.colorScheme.surface,
    icon = {
      Icon(
        imageVector = Icons.Rounded.Security,
        contentDescription = null,
        tint = ElectricBlueBright,
        modifier = Modifier.size(36.dp),
      )
    },
    title = {
      Text(
        text = AppStrings.incomingRequestTitle.get(language),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text =
            if (language == AppLanguage.BN) {
              "${session.controllerName} (${session.controllerDeviceName}) আপনার ডিভাইসের সাথে সংযুক্ত হতে চাইছে।"
            } else {
              "${session.controllerName} (${session.controllerDeviceName}) wants to connect to your device."
            },
          style = MaterialTheme.typography.bodyLarge,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable { allowScreen = !allowScreen }
              .padding(4.dp),
        ) {
          Checkbox(
            checked = allowScreen,
            onCheckedChange = { allowScreen = it },
            colors = CheckboxDefaults.colors(checkedColor = ElectricBlue),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = if (language == AppLanguage.BN) "লাইভ স্ক্রিন দেখার অনুমতি" else "Allow Live Screen Viewing",
            style = MaterialTheme.typography.bodyMedium,
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable { allowTouch = !allowTouch }
              .padding(4.dp),
        ) {
          Checkbox(
            checked = allowTouch,
            onCheckedChange = { allowTouch = it },
            colors = CheckboxDefaults.colors(checkedColor = ElectricBlue),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = AppStrings.allowTouchControl.get(language),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
        Text(
          text = AppStrings.securityBadgeNote.get(language),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onAccept(allowScreen, allowTouch) },
        colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
        modifier = Modifier.testTag("approve_session_button"),
      ) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(AppStrings.accept.get(language), color = Color.White)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onReject,
        border = BorderStroke(1.dp, DisconnectRed),
        modifier = Modifier.testTag("reject_session_button"),
      ) {
        Icon(Icons.Rounded.Close, contentDescription = null, tint = DisconnectRed, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(AppStrings.reject.get(language), color = DisconnectRed)
      }
    },
  )
}
