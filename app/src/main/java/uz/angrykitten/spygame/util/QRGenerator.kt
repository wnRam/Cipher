package uz.angrykitten.spygame.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRGenerator @Inject constructor() {

    fun generateQRCode(content: String, size: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        // Build the pixel array in one pass and copy it into the Bitmap in a
        // single setPixels call. setPixel() per-pixel on a 512×512 image
        // crosses the JNI boundary 262 144 times; this version crosses it
        // once and is roughly 50–100× faster.
        val pixels = IntArray(size * size)
        var index = 0
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[index++] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    }

    fun encodeRoomInfo(ip: String, port: Int, roomCode: String): String =
        "cipher://join?ip=$ip&port=$port&code=$roomCode"

    data class RoomConnectionInfo(
        val ip: String,
        val port: Int,
        val roomCode: String
    )

    fun decodeRoomInfo(data: String): RoomConnectionInfo? {
        return try {
            // Preferred: cipher://join?ip=…&port=…&code=…
            if (data.startsWith("cipher://join")) {
                val query = data.substringAfter("?", "")
                val params = query.split("&").mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }.toMap()
                val ip = params["ip"] ?: return null
                val port = params["port"]?.toIntOrNull() ?: return null
                val code = params["code"] ?: return null
                return RoomConnectionInfo(ip, port, code)
            }
            // Legacy fallback: ip:port:code
            val parts = data.split(":")
            if (parts.size == 3) {
                RoomConnectionInfo(
                    ip = parts[0],
                    port = parts[1].toInt(),
                    roomCode = parts[2]
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
