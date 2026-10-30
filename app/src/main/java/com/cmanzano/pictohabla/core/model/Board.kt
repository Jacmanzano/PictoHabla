package com.cmanzano.pictohabla.core.model

/**
 * Modelo de dominio que representa un tablero de comunicación personalizado.
 *
 * @property id Identificador único del tablero.
 * @property name Nombre descriptivo (ej: "Vocabulario Esencial", "Comida", "En el Cole").
 * @property description Descripción opcional para profesores o cuidadores.
 * @property columnsCount Número de columnas configurado para la cuadrícula (1..6).
 * @property isDefault Indica si es el tablero predeterminado del sistema.
 * @property pictograms Lista ordenada de pictogramas que contiene el tablero.
 * @property createdAt Marca temporal de creación.
 */
data class Board(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val columnsCount: Int = 3,
    val isDefault: Boolean = false,
    val pictograms: List<Pictogram> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
