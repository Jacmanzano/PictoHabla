package com.cmanzano.pictohabla.data.repository

import com.cmanzano.pictohabla.core.database.dao.BoardDao
import com.cmanzano.pictohabla.core.database.dao.PictogramDao
import com.cmanzano.pictohabla.core.database.entity.BoardEntity
import com.cmanzano.pictohabla.core.database.entity.BoardPictogramCrossRef
import com.cmanzano.pictohabla.core.model.Board
import com.cmanzano.pictohabla.data.local.DefaultCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repositorio para la gestión de tableros de comunicación personalizados en Room SQLite.
 */
class BoardRepository(
    private val boardDao: BoardDao,
    private val pictogramDao: PictogramDao? = null
) {

    /**
     * Inicializa el tablero principal predeterminado con vocabulario esencial
     * si la base de datos de tableros está vacía.
     */
    suspend fun initializeDefaultBoardIfNeeded() = withContext(Dispatchers.IO) {
        val count = boardDao.getBoardCount()
        if (count == 0) {
            val defaultBoard = BoardEntity(
                name = "Vocabulario Esencial",
                description = "Tablero principal con expresiones clave y necesidades cotidianas",
                columnsCount = 3,
                isDefault = true,
                createdAt = System.currentTimeMillis()
            )
            val boardId = boardDao.insertBoard(defaultBoard)
            val coreIds = DefaultCatalog.getDefaultBoardPictoIds()
            val refs = coreIds.mapIndexed { index, pictoId ->
                BoardPictogramCrossRef(boardId = boardId, pictoId = pictoId, position = index)
            }
            boardDao.insertCrossRefs(refs)
        }
    }

    /**
     * Emite la lista de todos los tableros con sus metadatos.
     */
    fun getAllBoards(): Flow<List<Board>> {
        return boardDao.getAllBoardsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Emite un tablero específico con sus pictogramas ordenados en tiempo real.
     */
    fun getBoardWithPictograms(boardId: Long): Flow<Board?> {
        val boardFlow = boardDao.getAllBoardsFlow().map { list ->
            list.find { it.id == boardId }?.toDomain()
        }
        val pictosFlow = boardDao.getPictogramsForBoardFlow(boardId).map { entities ->
            entities.map { it.toDomain() }
        }

        return combine(boardFlow, pictosFlow) { board, pictos ->
            board?.copy(pictograms = pictos)
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Crea un nuevo tablero personalizado.
     */
    suspend fun createBoard(
        name: String,
        description: String = "",
        columnsCount: Int = 3,
        initialPictoIds: List<Int> = emptyList()
    ): Long = withContext(Dispatchers.IO) {
        val entity = BoardEntity(
            name = name.trim(),
            description = description.trim(),
            columnsCount = columnsCount.coerceIn(1, 6),
            isDefault = false,
            createdAt = System.currentTimeMillis()
        )
        val newId = boardDao.insertBoard(entity)
        if (initialPictoIds.isNotEmpty()) {
            val refs = initialPictoIds.mapIndexed { index, pictoId ->
                BoardPictogramCrossRef(boardId = newId, pictoId = pictoId, position = index)
            }
            boardDao.insertCrossRefs(refs)
        }
        newId
    }

    /**
     * Actualiza el nombre, descripción y número de columnas de un tablero existente.
     */
    suspend fun updateBoard(
        boardId: Long,
        name: String,
        description: String = "",
        columnsCount: Int = 3
    ) = withContext(Dispatchers.IO) {
        val existing = boardDao.getBoardById(boardId)
        if (existing != null) {
            boardDao.updateBoard(
                existing.copy(
                    name = name.trim(),
                    description = description.trim(),
                    columnsCount = columnsCount.coerceIn(1, 6)
                )
            )
        }
    }

    /**
     * Duplica un tablero existente con todos sus pictogramas y orden.
     */
    suspend fun duplicateBoard(boardId: Long, newName: String): Long = withContext(Dispatchers.IO) {
        val original = boardDao.getBoardById(boardId) ?: return@withContext 0L
        val pictos = boardDao.getPictogramsForBoard(boardId)

        val duplicateEntity = BoardEntity(
            name = newName.trim().ifEmpty { "${original.name} (Copia)" },
            description = original.description,
            columnsCount = original.columnsCount,
            isDefault = false,
            createdAt = System.currentTimeMillis()
        )
        val newBoardId = boardDao.insertBoard(duplicateEntity)
        val refs = pictos.mapIndexed { index, picto ->
            BoardPictogramCrossRef(boardId = newBoardId, pictoId = picto.id, position = index)
        }
        boardDao.insertCrossRefs(refs)
        newBoardId
    }

    /**
     * Elimina un tablero si no es el predeterminado del sistema.
     */
    suspend fun deleteBoard(boardId: Long) = withContext(Dispatchers.IO) {
        boardDao.deleteBoard(boardId)
    }

    /**
     * Añade un pictograma al final del tablero.
     */
    suspend fun addPictogramToBoard(boardId: Long, pictoId: Int) = withContext(Dispatchers.IO) {
        val maxPos = boardDao.getMaxPosition(boardId) ?: -1
        boardDao.insertCrossRef(
            BoardPictogramCrossRef(boardId = boardId, pictoId = pictoId, position = maxPos + 1)
        )
    }

    /**
     * Retira un pictograma del tablero.
     */
    suspend fun removePictogramFromBoard(boardId: Long, pictoId: Int) = withContext(Dispatchers.IO) {
        boardDao.removePictoFromBoard(boardId, pictoId)
    }

    /**
     * Mueve un pictograma a la izquierda/arriba (step = -1) o derecha/abajo (step = +1)
     * de forma accesible mediante botones, sin depender exclusivamente de drag-and-drop.
     */
    suspend fun movePictogram(boardId: Long, pictoId: Int, moveLeft: Boolean) = withContext(Dispatchers.IO) {
        val currentPictos = boardDao.getPictogramsForBoard(boardId)
        val currentIndex = currentPictos.indexOfFirst { it.id == pictoId }
        if (currentIndex == -1) return@withContext

        val targetIndex = if (moveLeft) currentIndex - 1 else currentIndex + 1
        if (targetIndex in currentPictos.indices) {
            val list = currentPictos.map { it.id }.toMutableList()
            list.removeAt(currentIndex)
            list.add(targetIndex, pictoId)
            boardDao.reorderPictograms(boardId, list)
        }
    }

    /**
     * Reordena la lista completa de pictogramas de un tablero.
     */
    suspend fun reorderPictograms(boardId: Long, orderedPictoIds: List<Int>) = withContext(Dispatchers.IO) {
        boardDao.reorderPictograms(boardId, orderedPictoIds)
    }
}
