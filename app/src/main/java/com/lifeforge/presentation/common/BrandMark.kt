package com.lifeforge.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifeforge.R

/**
 * Marca do LifeForge: as mesmas camadas do ícone adaptativo (fundo em degradê e
 * símbolo) recortadas numa forma do Material 3 Expressive — a tela de entrada
 * conversa com o ícone da tela inicial do aparelho. Decorativa para o TalkBack:
 * o nome do app vem logo abaixo, em texto.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 112.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Cookie9Sided.toShape()),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
