package com.qr.domain

import android.content.res.Configuration
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.free.qr.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocalizationTest {
    @Test fun allSixLocalesResolveDisplayTextAndNestedValidation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        mapOf("en" to "Studio", "ru" to "Студия", "es" to "Estudio", "hi" to "स्टूडियो", "ar" to "الاستوديو", "tr" to "Stüdyo").forEach { (language, expected) ->
            val locale = Locale.forLanguageTag(language)
            val configuration = Configuration(context.resources.configuration).apply { setLocale(locale); setLayoutDirection(locale) }
            val localized = context.createConfigurationContext(configuration)
            assertEquals(expected, localized.getString(R.string.nav_studio))
            val message = LocalizedText(R.string.error_required, listOf(LocalizedText(R.string.field_url))).resolve(localized)
            assertTrue(message.contains(localized.getString(R.string.field_url)))
            assertEquals(if (language == "ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR, configuration.layoutDirection)
            R.string::class.java.fields.forEach { field ->
                val id = field.getInt(null)
                val template = localized.getString(id)
                assertTrue(template.isNotBlank())
                if (template.contains(Regex("%[12]\\$"))) {
                    assertTrue(localized.getString(id, 7, "sample").isNotBlank())
                }
            }
        }
    }
}
