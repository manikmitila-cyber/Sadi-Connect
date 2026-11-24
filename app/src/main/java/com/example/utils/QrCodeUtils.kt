package com.example.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeUtils {
  fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
    if (content.isBlank()) return null
    return try {
      val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
      val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
      for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
          bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
      }
      bitmap
    } catch (_: Exception) {
      null
    }
  }

  fun decodeYuvQrCode(yuvBytes: ByteArray, width: Int, height: Int): String? {
    return try {
      val source = PlanarYUVLuminanceSource(yuvBytes, width, height, 0, 0, width, height, false)
      val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
      MultiFormatReader().decode(binaryBitmap).text
    } catch (_: Exception) {
      null
    }
  }
}
