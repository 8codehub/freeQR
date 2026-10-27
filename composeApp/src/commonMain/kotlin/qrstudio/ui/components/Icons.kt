// Lucide icon geometry. Copyright Lucide contributors; ISC license in third_party/LUCIDE-LICENSE.
package qrstudio.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp
import qrstudio.theme.LocalTokens

enum class StudioIcon { Link, QrCode, Image, Blend, Type, Share2, Download, Sun, Moon, Plus, Trash2 }
private val vectors=mutableMapOf<StudioIcon,ImageVector>()
private fun vector(icon: StudioIcon): ImageVector=vectors.getOrPut(icon) {
    val data=when(icon) {
        StudioIcon.Link -> "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71 M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"
        StudioIcon.QrCode -> "M4.0 3.0H7.0Q8.0 3.0 8.0 4.0V7.0Q8.0 8.0 7.0 8.0H4.0Q3.0 8.0 3.0 7.0V4.0Q3.0 3.0 4.0 3.0Z M17.0 3.0H20.0Q21.0 3.0 21.0 4.0V7.0Q21.0 8.0 20.0 8.0H17.0Q16.0 8.0 16.0 7.0V4.0Q16.0 3.0 17.0 3.0Z M4.0 16.0H7.0Q8.0 16.0 8.0 17.0V20.0Q8.0 21.0 7.0 21.0H4.0Q3.0 21.0 3.0 20.0V17.0Q3.0 16.0 4.0 16.0Z M21 16h-3a2 2 0 0 0-2 2v3 M21 21v.01 M12 7v3a2 2 0 0 1-2 2H7 M3 12h.01 M12 3h.01 M12 16v.01 M16 12h1 M21 12v.01 M12 21v-1"
        StudioIcon.Image -> "M5.0 3.0H19.0Q21.0 3.0 21.0 5.0V19.0Q21.0 21.0 19.0 21.0H5.0Q3.0 21.0 3.0 19.0V5.0Q3.0 3.0 5.0 3.0Z M7.0 9.0a2.0 2.0 0 1 0 4.0 0a2.0 2.0 0 1 0 -4.0 0 m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"
        StudioIcon.Blend -> "M8.0 9.0a7.0 7.0 0 1 0 14.0 0a7.0 7.0 0 1 0 -14.0 0 M2.0 15.0a7.0 7.0 0 1 0 14.0 0a7.0 7.0 0 1 0 -14.0 0"
        StudioIcon.Type -> "M12 4v16 M4 7V5a1 1 0 0 1 1-1h14a1 1 0 0 1 1 1v2 M9 20h6"
        StudioIcon.Share2 -> "M15.0 5.0a3.0 3.0 0 1 0 6.0 0a3.0 3.0 0 1 0 -6.0 0 M3.0 12.0a3.0 3.0 0 1 0 6.0 0a3.0 3.0 0 1 0 -6.0 0 M15.0 19.0a3.0 3.0 0 1 0 6.0 0a3.0 3.0 0 1 0 -6.0 0 M8.59 13.51L15.42 17.49 M15.41 6.51L8.59 10.49"
        StudioIcon.Download -> "M12 15V3 M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 m7 10 5 5 5-5"
        StudioIcon.Sun -> "M8.0 12.0a4.0 4.0 0 1 0 8.0 0a4.0 4.0 0 1 0 -8.0 0 M12 2v2 M12 20v2 m4.93 4.93 1.41 1.41 m17.66 17.66 1.41 1.41 M2 12h2 M20 12h2 m6.34 17.66-1.41 1.41 m19.07 4.93-1.41 1.41"
        StudioIcon.Moon -> "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401"
        StudioIcon.Plus -> "M5 12h14 M12 5v14"
        StudioIcon.Trash2 -> "M10 11v6 M14 11v6 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6 M3 6h18 M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
    }
    ImageVector.Builder(icon.name,24.dp,24.dp,24f,24f).addPath(PathParser().parsePathString(data).toNodes(),stroke=SolidColor(Color.Black),strokeLineWidth=2f,strokeLineCap=StrokeCap.Round,strokeLineJoin=StrokeJoin.Round,fill=null).build()
}
@Composable fun Glyph(icon: StudioIcon,description: String?=null,modifier: Modifier=Modifier,color: Color=LocalTokens.current.text) {
    Icon(vector(icon),description,modifier,tint=color)
}
