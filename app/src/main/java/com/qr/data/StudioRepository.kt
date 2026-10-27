package com.qr.data

import android.content.Context
import com.qr.domain.*
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

object DraftCodec {
    fun encode(d: QrDraft) = JSONObject().apply {
        put("title", d.title); put("type", d.type.name); put("fields", JSONObject(d.fields))
        put("ink", d.customInk); put("paper", d.customPaper); put("palette", d.palette); put("shape", d.shape.name); put("correction", d.correction.name)
        put("size", d.size); put("hidden", d.hiddenWifi); put("open", d.openWifi)
        put("poster", JSONObject().apply {
            val p = d.poster
            put("enabled", p.enabled); put("photo", p.photoUri); put("ratio", p.ratio.name)
            put("gradient", p.gradientEnabled); put("top", p.topColor); put("bottom", p.bottomColor)
            put("opacity", p.opacity.toDouble()); put("reversed", p.reversed); put("zoom", p.photoZoom.toDouble())
            put("scale", p.qrScale.toDouble()); put("x", p.x.toDouble()); put("y", p.y.toDouble())
            put("frame", p.frame.name); put("caption", p.caption)
        })
    }
    fun decode(o: JSONObject): QrDraft {
        val fields = o.getJSONObject("fields")
        return QrDraft(o.getString("title"), ContentType.valueOf(o.getString("type")),
            fields.keys().asSequence().associateWith { fields.getString(it) },
            o.optInt("palette").coerceIn(palettes.indices), ModuleShape.valueOf(o.getString("shape")),
            Correction.valueOf(o.getString("correction")), o.optInt("size", 1024).let { if (it in listOf(512, 1024, 2048)) it else 1024 },
            o.optBoolean("hidden"), o.optBoolean("open"), decodePoster(o.optJSONObject("poster")), if (o.isNull("ink")) null else o.getLong("ink"), if (o.isNull("paper")) null else o.getLong("paper"))
    }
    private fun decodePoster(o: JSONObject?): PosterDesign {
        if (o == null) return PosterDesign()
        return PosterDesign(
            enabled = o.optBoolean("enabled"), photoUri = o.optString("photo").takeIf { it.startsWith("content://") },
            ratio = runCatching { CanvasRatio.valueOf(o.getString("ratio")) }.getOrDefault(CanvasRatio.PORTRAIT),
            gradientEnabled = o.optBoolean("gradient", true), topColor = o.optLong("top", 0xFF203E37), bottomColor = o.optLong("bottom", 0xFFB2C794),
            opacity = o.optDouble("opacity", .65).toFloat(), reversed = o.optBoolean("reversed"), photoZoom = o.optDouble("zoom", 1.0).toFloat(),
            qrScale = o.optDouble("scale", .48).toFloat(), x = o.optDouble("x", .5).toFloat(), y = o.optDouble("y", .65).toFloat(),
            frame = runCatching { QrFrame.valueOf(o.getString("frame")) }.getOrDefault(QrFrame.CARD), caption = o.optString("caption")
        ).normalized()
    }
}
@Singleton
class StudioRepository @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("qr_library", Context.MODE_PRIVATE)
    private val settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun appearance(): Appearance = runCatching { Appearance.valueOf(settings.getString("theme_mode", "SYSTEM")!!) }.getOrDefault(Appearance.SYSTEM)
    fun setAppearance(value: Appearance) { check(settings.edit().putString("theme_mode", value.name).commit()) }
    fun load(): List<SavedQr> {
        val array = JSONArray(preferences.getString("items", "[]"))
        return (0 until array.length()).map { index ->
            val o = array.getJSONObject(index)
            SavedQr(o.getString("id"), DraftCodec.decode(o.getJSONObject("draft")), o.getLong("updated"))
        }
    }
    fun store(items: List<SavedQr>) {
        val array = JSONArray()
        items.forEach { item -> array.put(JSONObject().apply {
            put("id", item.id); put("draft", DraftCodec.encode(item.draft)); put("updated", item.updatedAt)
        }) }
        check(preferences.edit().putString("items", array.toString()).commit()) { "Could not save your library. Please try again." }
    }
}
