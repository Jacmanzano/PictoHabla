package com.cmanzano.pictohabla.feature.favorites

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import com.cmanzano.pictohabla.core.designsystem.PictoHaptics
import com.cmanzano.pictohabla.core.model.Pictogram
import com.cmanzano.pictohabla.feature.board.components.PictogramCard
import com.cmanzano.pictohabla.navigation.PictoBottomNavBar
import com.cmanzano.pictohabla.navigation.PictoScreen

/**
 * Pantalla de Pictogramas Favoritos.
 * Muestra una cuadrícula con acceso rápido a los pictogramas guardados por el usuario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoritePictos: List<Pictogram>,
    languageCode: String,
    highContrast: Boolean,
    hapticsEnabled: Boolean,
    largeGrid: Boolean,
    onPictoClick: (Pictogram) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onBack: () -> Unit,
    onNavigateTablero: () -> Unit = onBack,
    onNavigateHistory: () -> Unit = {},
    onNavigateLearn: () -> Unit = {}
) {
    val hapticFeedback = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val gridMinCardWidth = if (isLandscape) {
        if (largeGrid) PictoDimens.CardMinSizeLandscapeLarge else PictoDimens.CardMinSizeLandscapeStandard
    } else {
        if (largeGrid) PictoDimens.CardMinSizePortraitLarge else PictoDimens.CardMinSizePortraitStandard
    }

    val gridCardHeight = if (isLandscape) {
        if (largeGrid) PictoDimens.CardHeightLandscapeLarge else PictoDimens.CardHeightLandscapeStandard
    } else {
        if (largeGrid) PictoDimens.CardHeightPortraitLarge else PictoDimens.CardHeightPortraitStandard
    }

    val isEn = languageCode.equals("EN", ignoreCase = true)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isEn) "Favorite Pictograms" else "Pictogramas Favoritos",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                            onBack()
                        },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = if (isEn) "Back to main board" else "Volver al tablero principal"
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            PictoBottomNavBar(
                currentScreen = PictoScreen.FAVORITES,
                onNavigateTablero = onNavigateTablero,
                onNavigateFavorites = { /* ya en favoritos */ },
                onNavigateHistory = onNavigateHistory,
                onNavigateLearn = onNavigateLearn,
                highContrast = highContrast,
                hapticsEnabled = hapticsEnabled
            )
        }
    ) { padding ->
        if (favoritePictos.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(PictoDimens.SpaceExtraLarge),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFFFB300)
                    )
                    Spacer(Modifier.height(PictoDimens.SpaceMedium))
                    Text(
                        text = if (isEn) "No favorites saved yet" else "Aún no tienes favoritos guardados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(PictoDimens.SpaceSmall))
                    Text(
                        text = if (isEn) {
                            "Tap the star on any pictogram on the board to add it to quick access."
                        } else {
                            "Toca la estrella en la esquina de cualquier pictograma del tablero para añadirlo a esta sección de acceso rápido."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = gridMinCardWidth),
                contentPadding = PaddingValues(PictoDimens.SpaceMedium),
                horizontalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium),
                verticalArrangement = Arrangement.spacedBy(PictoDimens.SpaceMedium),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                items(favoritePictos, key = { it.id }) { picto ->
                    PictogramCard(
                        picto = picto,
                        languageCode = languageCode,
                        isFavorite = true,
                        highContrast = highContrast,
                        hapticsEnabled = hapticsEnabled,
                        onToggleFavorite = { onToggleFavorite(picto.id) },
                        modifier = Modifier.height(gridCardHeight)
                    ) {
                        onPictoClick(picto)
                    }
                }
            }
        }
    }
}
