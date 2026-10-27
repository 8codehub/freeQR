package qrstudio.qr

import qrcode.raw.QRCodeProcessor
import qrcode.raw.QRCodeDataType
import qrcode.raw.ErrorCorrectionLevel
import qrstudio.model.Ecl

class QrMatrix(val n: Int, private val cells: BooleanArray) {
    fun isDark(row: Int,col: Int) = row in 0 until n && col in 0 until n && cells[row*n+col]
}
/** One-entry memoization: design-only edits never invoke the byte-mode encoder. */
class QrEncoder {
    private var lastKey: Pair<String,Ecl>? = null
    private var lastResult: QrMatrix? = null
    var encodingCount: Int = 0; private set
    fun encode(payload: String,ecl: Ecl): QrMatrix? {
        val key=payload to ecl
        if(lastKey==key) return lastResult
        lastKey=key;encodingCount++
        lastResult=runCatching {
            require(payload.encodeToByteArray().size <= 2953)
            val level=when(ecl) { Ecl.L -> ErrorCorrectionLevel.LOW; Ecl.M -> ErrorCorrectionLevel.MEDIUM; Ecl.Q -> ErrorCorrectionLevel.HIGH; Ecl.H -> ErrorCorrectionLevel.VERY_HIGH }
            val version=QRCodeProcessor.infoDensityForDataAndECL(payload,level,QRCodeDataType.DEFAULT)
            val processor=QRCodeProcessor(payload,level,QRCodeDataType.DEFAULT)
            // The upstream shortcut jumps to v40 beyond its small-version lookup. Search the
            // remaining versions so UTF-8 byte mode always gets the smallest fitting matrix.
            val raw=if(version<40) processor.encode(type=version) else
                (level.maxTypeNum..40).firstNotNullOfOrNull { candidate -> runCatching { processor.encode(type=candidate) }.getOrNull() }
                    ?: error("Payload exceeds QR capacity")
            QrMatrix(raw.size,BooleanArray(raw.size*raw.size) { raw[it/raw.size][it%raw.size].dark })
        }.getOrNull()
        return lastResult
    }
}
