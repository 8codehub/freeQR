package qrstudio

import kotlin.test.*
import qrstudio.model.*
import qrstudio.qr.*
class EncoderTest {
    @Test fun everyCorrectionLevelEncodesUtf8Bytes() {
        Ecl.entries.forEach { level ->
            val matrix=assertNotNull(QrEncoder().encode("Hello café 世界 مرحبا",level))
            assertEquals(0,(matrix.n-17)%4)
            assertTrue(matrix.isDark(0,0));assertFalse(matrix.isDark(-1,0));assertFalse(matrix.isDark(matrix.n,0))
        }
    }
    @Test fun memoizationUsesOnlyPayloadAndCorrection() {
        val encoder=QrEncoder();val initial=EditorState()
        val matrix=encoder.encode(buildPayload(initial),initial.ecl)
        val styled=initial.copy(fg=0xFFFFFFFF,qrSize=.8f,bg=0xFFEC3013)
        assertSame(matrix,encoder.encode(buildPayload(styled),styled.ecl));assertEquals(1,encoder.encodingCount)
        encoder.encode(buildPayload(styled),Ecl.H);assertEquals(2,encoder.encodingCount)
    }
    @Test fun overCapacityReturnsNullAndTheFailureIsMemoized() {
        val encoder=QrEncoder();val payload="a".repeat(5000)
        assertNull(encoder.encode(payload,Ecl.L));assertNull(encoder.encode(payload,Ecl.L));assertEquals(1,encoder.encodingCount)
    }
    @Test fun numericInputStillUsesByteModeAndAutoVersions() {
        assertNotNull(QrEncoder().encode("1234567890".repeat(30),Ecl.Q))
        assertNotNull(QrEncoder().encode("x".repeat(2000),Ecl.L))
        assertNull(QrEncoder().encode("x".repeat(2000),Ecl.H))
    }
}
