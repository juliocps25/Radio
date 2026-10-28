@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.julio.radio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.julio.radio.R
import com.julio.radio.data.model.Station

@Composable
fun StationLogo(
    station: Station,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    val context = LocalContext.current
    val shape = MaterialShapes.Cookie6Sided.toShape()
    val placeholder = painterResource(R.drawable.ic_station_placeholder)
    val sizePx = with(LocalDensity.current) { size.roundToPx() }

    val request = remember(station.favicon, sizePx) {
        ImageRequest.Builder(context)
            .data(station.favicon.takeIf { it.isNotBlank() })
            .size(sizePx)
            .build()
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().padding(6.dp),
            contentScale = ContentScale.Fit,
            placeholder = placeholder,
            error = placeholder,
            fallback = placeholder
        )
    }
}