package cl.duoc.museo.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Security {
    /** Hash simple para el MVP académico (no usar así en producción). */
    fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    fun isEmail(s: String) = emailRegex.matches(s.trim())
    fun isStrongPassword(s: String) = s.length >= 6 && s.any { it.isDigit() } && s.any { it.isLetter() }
    fun isUrl(s: String) = s.startsWith("http://") || s.startsWith("https://")
}

object Format {
    fun money(v: Double): String = "US$ " + String.format(Locale.US, "%.0f", v)
    fun money2(v: Double): String = "US$ " + String.format(Locale.US, "%.2f", v)
    fun date(millis: Long): String =
        SimpleDateFormat("d MMM yyyy", Locale("es", "CL")).format(Date(millis))

    fun relative(millis: Long): String {
        val diff = (System.currentTimeMillis() - millis) / 1000
        return when {
            diff < 60 -> "ahora"
            diff < 3600 -> "${diff / 60} min"
            diff < 86400 -> "${diff / 3600} h"
            else -> "${diff / 86400} d"
        }
    }
}

/**
 * Reglas FICTICIAS de cálculo de envío (no se conecta a ningún courier real).
 * Las mismas reglas existen en el backend (ShippingService).
 */
object ShippingCalculator {
    val countries = listOf(
        "Chile", "Argentina", "Perú", "Colombia", "México", "Brasil",
        "Uruguay", "Ecuador", "Costa Rica", "España", "Portugal", "Otro"
    )

    enum class ShippingType(val label: String, val days: String) {
        ESTANDAR("Estándar", "7-14 días"),
        EXPRESS("Express", "2-5 días"),
        RETIRO("Retiro en sede", "Gratis, solo Chile")
    }

    private val latam = setOf("Argentina", "Perú", "Colombia", "México", "Brasil", "Uruguay", "Ecuador", "Costa Rica")

    /** @param physicalUnits unidades de productos físicos (los cursos no se envían). */
    fun quote(country: String, type: ShippingType, physicalUnits: Int): Double {
        if (physicalUnits <= 0) return 0.0
        if (type == ShippingType.RETIRO) return if (country == "Chile") 0.0 else -1.0
        val (base, perItem, expressFactor) = when {
            country == "Chile" -> Triple(4.0, 1.0, 1.8)
            country in latam -> Triple(12.0, 2.0, 1.6)
            else -> Triple(20.0, 3.0, 1.5)
        }
        val standard = base + perItem * (physicalUnits - 1)
        return if (type == ShippingType.EXPRESS) standard * expressFactor else standard
    }

    fun isNational(country: String) = country == "Chile"
}

object QrGenerator {
    fun bitmap(content: String, size: Int = 512): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size) {
            bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
        return bmp
    }
}
