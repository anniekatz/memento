package com.annie.memento.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.annie.memento.di.LocalAppSettings
import com.annie.memento.model.CardFont
import com.annie.memento.model.CardFontWeight
import memento.shared.generated.resources.Res
import memento.shared.generated.resources.noto_sans_jp
import memento.shared.generated.resources.noto_serif_jp
import memento.shared.generated.resources.zen_maru_gothic_bold
import memento.shared.generated.resources.zen_maru_gothic_light
import memento.shared.generated.resources.zen_maru_gothic_medium
import memento.shared.generated.resources.zen_maru_gothic_regular
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

@Composable
fun TextStyle.withCardFont(main: Boolean): TextStyle {
    val settings = LocalAppSettings.current
    return withCardFont(settings.cardFont, if (main) settings.cardFontWeight else null)
}

@Composable
fun TextStyle.withCardFont(font: CardFont, weight: CardFontWeight?): TextStyle =
    copy(
        fontFamily = cardFontFamily(font) ?: fontFamily,
        fontWeight = weight?.toFontWeight() ?: fontWeight,
    )

fun CardFontWeight.toFontWeight(): FontWeight = when (this) {
    CardFontWeight.LIGHT -> FontWeight.Light
    CardFontWeight.REGULAR -> FontWeight.Normal
    CardFontWeight.MEDIUM -> FontWeight.Medium
    CardFontWeight.BOLD -> FontWeight.Bold
}

@Composable
fun cardFontFamily(font: CardFont): FontFamily? = when (font) {
    CardFont.SYSTEM -> null
    CardFont.NOTO_SANS -> variableFamily(Res.font.noto_sans_jp)
    CardFont.NOTO_SERIF -> variableFamily(Res.font.noto_serif_jp)
    CardFont.ZEN_MARU -> FontFamily(
        Font(Res.font.zen_maru_gothic_light, FontWeight.Light),
        Font(Res.font.zen_maru_gothic_regular, FontWeight.Normal),
        Font(Res.font.zen_maru_gothic_medium, FontWeight.Medium),
        Font(Res.font.zen_maru_gothic_bold, FontWeight.Bold),
    )
}

@Composable
private fun variableFamily(resource: FontResource): FontFamily = FontFamily(
    variableFont(resource, FontWeight.Light),
    variableFont(resource, FontWeight.Normal),
    variableFont(resource, FontWeight.Medium),
    variableFont(resource, FontWeight.SemiBold),
    variableFont(resource, FontWeight.Bold),
)

@Composable
private fun variableFont(resource: FontResource, weight: FontWeight) =
    Font(resource, weight, FontStyle.Normal, FontVariation.Settings(FontVariation.weight(weight.weight)))
