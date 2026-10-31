package com.cmanzano.pictohabla.navigation

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.core.designsystem.PictoColors
import com.cmanzano.pictohabla.core.designsystem.PictoDimens
import com.cmanzano.pictohabla.core.designsystem.PictoHaptics

/**
 * Barra de navegación principal inferior accesible para PictoHabla.
 * Permite a niños y cuidadores alternar fácilmente entre:
 * - Tablero principal
 * - Favoritos
 * - Historial
 * - Aprender
 * Todos los elementos incluyen icono y texto siempre visible.
 */
@Composable
fun PictoBottomNavBar(
    currentScreen: PictoScreen,
    onNavigateTablero: () -> Unit,
    onNavigateFavorites: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateLearn: () -> Unit,
    modifier: Modifier = Modifier,
    highContrast: Boolean = false,
    hapticsEnabled: Boolean = true
) {
    val hapticFeedback = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isPhoneLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && configuration.screenHeightDp < 500
    val navHeight = if (isPhoneLandscape) 50.dp else PictoDimens.BottomNavHeight
    val iconSize = if (isPhoneLandscape) 20.dp else 26.dp
    val labelSize = if (isPhoneLandscape) 10.sp else 12.sp

    val navBarColors = if (highContrast) {
        NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = PictoColors.HighlightHighContrast,
            indicatorColor = PictoColors.HighlightHighContrast,
            unselectedIconColor = Color.White,
            unselectedTextColor = Color.White
        )
    } else {
        NavigationBarItemDefaults.colors(
            selectedIconColor = PictoColors.OnPrimary,
            selectedTextColor = PictoColors.Primary,
            indicatorColor = PictoColors.Primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = if (highContrast) Color.Black else MaterialTheme.colorScheme.surface,
        shadowElevation = PictoDimens.ElevationStandard,
        border = if (highContrast) BorderStroke(PictoDimens.BorderWidthThin, Color.White) else null
    ) {
        NavigationBar(
            modifier = Modifier.height(navHeight),
            containerColor = Color.Transparent,
            tonalElevation = PictoDimens.ElevationNone
        ) {
            // 1. Tablero
            val isBoardSelected = currentScreen == PictoScreen.BOARD
            NavigationBarItem(
                selected = isBoardSelected,
                onClick = {
                    if (!isBoardSelected) {
                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                        onNavigateTablero()
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )
                },
                label = {
                    Text(
                        text = "Tablero",
                        fontSize = labelSize,
                        fontWeight = if (isBoardSelected) FontWeight.Black else FontWeight.Bold
                    )
                },
                alwaysShowLabel = true,
                colors = navBarColors,
                modifier = Modifier.semantics {
                    role = Role.Tab
                    contentDescription = "Pestaña Tablero de comunicación" + if (isBoardSelected) ", seleccionada" else ""
                }
            )

            // 2. Favoritos
            val isFavSelected = currentScreen == PictoScreen.FAVORITES
            NavigationBarItem(
                selected = isFavSelected,
                onClick = {
                    if (!isFavSelected) {
                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                        onNavigateFavorites()
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )
                },
                label = {
                    Text(
                        text = "Favoritos",
                        fontSize = labelSize,
                        fontWeight = if (isFavSelected) FontWeight.Black else FontWeight.Bold
                    )
                },
                alwaysShowLabel = true,
                colors = navBarColors,
                modifier = Modifier.semantics {
                    role = Role.Tab
                    contentDescription = "Pestaña Pictogramas Favoritos" + if (isFavSelected) ", seleccionada" else ""
                }
            )

            // 3. Historial
            val isHistorySelected = currentScreen == PictoScreen.HISTORY
            NavigationBarItem(
                selected = isHistorySelected,
                onClick = {
                    if (!isHistorySelected) {
                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                        onNavigateHistory()
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )
                },
                label = {
                    Text(
                        text = "Historial",
                        fontSize = labelSize,
                        fontWeight = if (isHistorySelected) FontWeight.Black else FontWeight.Bold
                    )
                },
                alwaysShowLabel = true,
                colors = navBarColors,
                modifier = Modifier.semantics {
                    role = Role.Tab
                    contentDescription = "Pestaña Historial de expresiones" + if (isHistorySelected) ", seleccionada" else ""
                }
            )

            // 4. Aprender
            val isLearnSelected = currentScreen == PictoScreen.LEARN
            NavigationBarItem(
                selected = isLearnSelected,
                onClick = {
                    if (!isLearnSelected) {
                        PictoHaptics.performClick(hapticFeedback, hapticsEnabled)
                        onNavigateLearn()
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )
                },
                label = {
                    Text(
                        text = "Aprender",
                        fontSize = labelSize,
                        fontWeight = if (isLearnSelected) FontWeight.Black else FontWeight.Bold
                    )
                },
                alwaysShowLabel = true,
                colors = navBarColors,
                modifier = Modifier.semantics {
                    role = Role.Tab
                    contentDescription = "Pestaña Aprender frases guiadas" + if (isLearnSelected) ", seleccionada" else ""
                }
            )
        }
    }
}
