package com.qr.domain

import com.free.qr.R

import java.net.URI
import java.net.URLEncoder

enum class ContentType(val titleRes: Int, val hintRes: Int) {
    URL(R.string.kind_website, R.string.hint_website), TEXT(R.string.kind_text, R.string.hint_text),
    WIFI(R.string.kind_wifi, R.string.hint_wifi), EMAIL(R.string.kind_email, R.string.hint_email),
    PHONE(R.string.kind_phone, R.string.hint_phone), SMS(R.string.kind_sms, R.string.hint_sms),
    CONTACT(R.string.kind_contact, R.string.hint_contact)
}
enum class Appearance { SYSTEM, LIGHT, DARK }
enum class ModuleShape { SQUARE, ROUNDED, DOTS }
enum class Correction { M, Q, H }
data class Palette(val titleRes: Int, val ink: Long, val paper: Long)
val palettes = listOf(
    Palette(R.string.palette_ink, 0xFF000000, 0xFFFFFFFF),
    Palette(R.string.palette_forest, 0xFF245E49, 0xFFF5F7EB),
    Palette(R.string.palette_cobalt, 0xFF2546A5, 0xFFF1F4FF),
    Palette(R.string.palette_plum, 0xFF642F60, 0xFFFFF4FA),
    Palette(R.string.palette_terracotta, 0xFF873E28, 0xFFFFF5EA)
)
data class QrDraft(
    val title: String = "", val type: ContentType = ContentType.URL,
    val fields: Map<String, String> = emptyMap(), val palette: Int = 0,
    val shape: ModuleShape = ModuleShape.SQUARE, val correction: Correction = Correction.M,
    val size: Int = 1024, val hiddenWifi: Boolean = false, val openWifi: Boolean = false,
    val poster: PosterDesign = PosterDesign(), val customInk: Long? = null, val customPaper: Long? = null
)
data class SavedQr(val id: String, val draft: QrDraft, val updatedAt: Long)
data class FieldSpec(val key: String, val labelRes: Int, val placeholderRes: Int, val required: Boolean = false)
fun ContentType.fields(): List<FieldSpec> = when (this) {
    ContentType.URL -> listOf(FieldSpec("url", R.string.field_url, R.string.sample_url, true))
    ContentType.TEXT -> listOf(FieldSpec("text", R.string.field_text, R.string.placeholder_text, true))
    ContentType.WIFI -> listOf(FieldSpec("ssid", R.string.field_ssid, R.string.placeholder_ssid, true), FieldSpec("password", R.string.field_password, R.string.placeholder_password))
    ContentType.EMAIL -> listOf(FieldSpec("email", R.string.field_email, R.string.sample_email, true), FieldSpec("subject", R.string.field_subject, R.string.placeholder_subject), FieldSpec("body", R.string.field_message, R.string.placeholder_message))
    ContentType.PHONE -> listOf(FieldSpec("phone", R.string.field_phone_number, R.string.sample_phone, true))
    ContentType.SMS -> listOf(FieldSpec("phone", R.string.field_phone_number, R.string.sample_phone, true), FieldSpec("body", R.string.field_message, R.string.placeholder_message))
    ContentType.CONTACT -> listOf(FieldSpec("name", R.string.field_name, R.string.sample_name, true), FieldSpec("organization", R.string.field_company, R.string.placeholder_company), FieldSpec("phone", R.string.kind_phone, R.string.sample_phone), FieldSpec("email", R.string.kind_email, R.string.sample_contact_email), FieldSpec("url", R.string.kind_website, R.string.sample_contact_url))
}
class InvalidContent(val text: LocalizedText) : IllegalArgumentException() {
    constructor(id: Int, vararg args: Any) : this(LocalizedText(id, args.toList()))
}
object QrPayload {
    fun encode(draft: QrDraft): String {
        fun field(key: String) = draft.fields[key].orEmpty().trim()
        draft.type.fields().filter { it.required }.firstOrNull { field(it.key).isBlank() }?.let {
            throw InvalidContent(R.string.error_required, LocalizedText(it.labelRes))
        }
        if (draft.fields.values.any { it.length > 2000 }) throw InvalidContent(R.string.error_length)
        fun email(value: String) {
            if (value.isNotEmpty() && !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value)) throw InvalidContent(R.string.error_email)
        }
        fun phone(): String {
            val value = field("phone")
            if (!Regex("^\\+?[0-9 ()-]{3,30}$").matches(value)) throw InvalidContent(R.string.error_phone)
            return value.replace(Regex("[ ()-]"), "")
        }
        fun url(): String {
            val value = field("url")
            val uri = runCatching { URI(value) }.getOrNull()
            if (uri == null || uri.scheme?.lowercase() !in listOf("http", "https") || uri.host.isNullOrBlank()) throw InvalidContent(R.string.error_url)
            return value
        }
        return when (draft.type) {
            ContentType.URL -> url()
            ContentType.TEXT -> draft.fields["text"].orEmpty()
            ContentType.WIFI -> {
                if (!draft.openWifi && field("password").isBlank()) throw InvalidContent(R.string.error_wifi_password)
                "WIFI:T:${if (draft.openWifi) "nopass" else "WPA"};S:${wifi(draft.fields["ssid"].orEmpty())};P:${if (draft.openWifi) "" else wifi(draft.fields["password"].orEmpty())};H:${draft.hiddenWifi};;"
            }
            ContentType.EMAIL -> { email(field("email")); "mailto:${field("email")}?subject=${percent(field("subject"))}&body=${percent(field("body"))}" }
            ContentType.PHONE -> "tel:${phone()}"
            ContentType.SMS -> "smsto:${phone()}:${draft.fields["body"].orEmpty()}"
            ContentType.CONTACT -> {
                email(field("email"))
                if (field("url").isNotEmpty()) url()
                "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:${vcard(field("name"))}\r\nORG:${vcard(field("organization"))}\r\nTEL:${vcard(field("phone"))}\r\nEMAIL:${vcard(field("email"))}\r\nURL:${vcard(field("url"))}\r\nEND:VCARD"
            }
        }
    }
    private fun percent(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    private fun wifi(value: String) = value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace(":", "\\:").replace("\"", "\\\"")
    private fun vcard(value: String) = value.replace("\\", "\\\\").replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")
}

val QrDraft.colors: Palette get() = palettes[palette].copy(ink = customInk ?: palettes[palette].ink, paper = customPaper ?: palettes[palette].paper)
