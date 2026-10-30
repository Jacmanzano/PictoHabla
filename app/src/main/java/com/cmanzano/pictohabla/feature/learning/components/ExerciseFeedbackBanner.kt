package com.cmanzano.pictohabla.feature.learning.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmanzano.pictohabla.core.designsystem.PictoColors
import com.cmanzano.pictohabla.core.designsystem.PictoDimens

/**
 * Estado del feedback en las actividades educativas.
 */
sealed class FeedbackState {
    data object None : FeedbackState()
    data class Success(val message: String = "¡Muy bien! ¡Lo has encontrado!") : FeedbackState()
    data class TryAgain(val message: String = "Vamos a intentarlo de nuevo.") : FeedbackState()
}

/**
 * Banner de feedback positivo y respetuoso para las actividades educativas.
 * Cumple con las pautas de accesibilidad y CAA:
 * - Sin penalizaciones ni alarmas sonoras agresivas.
 * - Iconografía clara complementaria al color para no depender únicamente del color.
 * - Botones táctiles amplios (min 48dp).
 * - Semántica completa para TalkBack.
 */
@Composable
fun ExerciseFeedbackBanner(
    state: FeedbackState,
    highContrast: Boolean = false,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state !is FeedbackState.None,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        when (state) {
            is FeedbackState.Success -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Respuesta correcta: ${state.message}"
                        },
                    shape = PictoDimens.CornerMedium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (highContrast) Color.Black else Color(0xFFE8F5E9)
                    ),
                    border = BorderStroke(
                        width = if (highContrast) PictoDimens.BorderWidthHighContrast else 2.dp,
                        color = if (highContrast) PictoColors.HighlightHighContrast else Color(0xFF2E7D32)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PictoDimens.SpaceMedium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (highContrast) PictoColors.HighlightHighContrast else Color(0xFF2E7D32),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(PictoDimens.SpaceSmall))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (highContrast) Color.White else Color(0xFF1B5E20)
                            )
                        }

                        Spacer(Modifier.height(PictoDimens.SpaceMedium))

                        Button(
                            onClick = onNext,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Pasar al siguiente ejercicio"
                                },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (highContrast) PictoColors.HighlightHighContrast else PictoColors.Primary,
                                contentColor = if (highContrast) Color.Black else Color.White
                            ),
                            shape = PictoDimens.CornerMedium
                        ) {
                            Text(
                                text = "Siguiente ejercicio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.width(PictoDimens.SpaceSmall))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }

            is FeedbackState.TryAgain -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Ánimo: ${state.message}"
                        },
                    shape = PictoDimens.CornerMedium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (highContrast) Color(0xFF1E1E1E) else Color(0xFFFFF8E1)
                    ),
                    border = BorderStroke(
                        width = if (highContrast) PictoDimens.BorderWidthHighContrast else 2.dp,
                        color = if (highContrast) Color.White else Color(0xFFFFA000)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PictoDimens.SpaceMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (highContrast) Color.White else Color(0xFFE65100),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(PictoDimens.SpaceSmall))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (highContrast) Color.White else Color(0xFF5D4037)
                            )
                        }

                        Spacer(Modifier.width(PictoDimens.SpaceSmall))

                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .height(46.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Intentar de nuevo"
                                },
                            shape = PictoDimens.CornerMedium
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reintentar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            FeedbackState.None -> { /* No se muestra nada */ }
        }
    }
}
