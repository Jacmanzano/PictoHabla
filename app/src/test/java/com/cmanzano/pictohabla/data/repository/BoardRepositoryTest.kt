package com.cmanzano.pictohabla.data.repository

import com.cmanzano.pictohabla.core.database.dao.BoardDao
import com.cmanzano.pictohabla.core.database.entity.BoardEntity
import com.cmanzano.pictohabla.core.database.entity.BoardPictogramCrossRef
import com.cmanzano.pictohabla.core.database.entity.PictogramEntity
import com.cmanzano.pictohabla.core.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeBoardDao : BoardDao {
    private val boards = mutableMapOf<Long, BoardEntity>()
    private val crossRefs = mutableListOf<BoardPictogramCrossRef>()
    private val pictograms = mutableMapOf<Int, PictogramEntity>()

    private val boardsFlow = MutableStateFlow<List<BoardEntity>>(emptyList())
    private var nextBoardId = 1L

    fun populatePictograms(items: List<PictogramEntity>) {
        items.forEach { pictograms[it.id] = it }
    }

    override fun getAllBoardsFlow(): Flow<List<BoardEntity>> = boardsFlow

    override suspend fun getBoardById(id: Long): BoardEntity? = boards[id]

    override suspend fun getDefaultBoard(): BoardEntity? = boards.values.find { it.isDefault }

    override suspend fun insertBoard(board: BoardEntity): Long {
        val id = if (board.id != 0L) board.id else nextBoardId++
        val saved = board.copy(id = id)
        boards[id] = saved
        boardsFlow.value = boards.values.toList()
        return id
    }

    override suspend fun updateBoard(board: BoardEntity) {
        boards[board.id] = board
        boardsFlow.value = boards.values.toList()
    }

    override suspend fun deleteBoard(id: Long) {
        boards.remove(id)
        crossRefs.removeAll { it.boardId == id }
        boardsFlow.value = boards.values.toList()
    }

    override suspend fun getBoardCount(): Int = boards.size

    override suspend fun getPictogramsForBoard(boardId: Long): List<PictogramEntity> {
        val refs = crossRefs.filter { it.boardId == boardId }.sortedBy { it.position }
        return refs.mapNotNull { pictograms[it.pictoId] }
    }

    override fun getPictogramsForBoardFlow(boardId: Long): Flow<List<PictogramEntity>> {
        return boardsFlow.map {
            getPictogramsForBoard(boardId)
        }
    }

    override suspend fun insertCrossRef(ref: BoardPictogramCrossRef) {
        crossRefs.removeIf { it.boardId == ref.boardId && it.pictoId == ref.pictoId }
        crossRefs.add(ref)
        boardsFlow.value = boards.values.toList()
    }

    override suspend fun insertCrossRefs(refs: List<BoardPictogramCrossRef>) {
        refs.forEach { insertCrossRef(it) }
    }

    override suspend fun deleteCrossRefsForBoard(boardId: Long) {
        crossRefs.removeAll { it.boardId == boardId }
        boardsFlow.value = boards.values.toList()
    }

    override suspend fun removePictoFromBoard(boardId: Long, pictoId: Int) {
        crossRefs.removeAll { it.boardId == boardId && it.pictoId == pictoId }
        boardsFlow.value = boards.values.toList()
    }

    override suspend fun getMaxPosition(boardId: Long): Int? {
        return crossRefs.filter { it.boardId == boardId }.maxOfOrNull { it.position }
    }
}

class BoardRepositoryTest {

    private lateinit var fakeBoardDao: FakeBoardDao
    private lateinit var fakePictoDao: FakePictogramDao
    private lateinit var repository: BoardRepository

    @Before
    fun setUp() {
        fakeBoardDao = FakeBoardDao()
        fakePictoDao = FakePictogramDao()

        // Poblamos algunos pictogramas base para las pruebas
        val basePictos = listOf(
            PictogramEntity(id = 20, nameEs = "Yo", categoryName = Category.PEOPLE.name, orderIndex = 1),
            PictogramEntity(id = 30, nameEs = "Quiero", categoryName = Category.ACTIONS.name, orderIndex = 2),
            PictogramEntity(id = 40, nameEs = "Manzana", categoryName = Category.FOOD.name, orderIndex = 3),
            PictogramEntity(id = 50, nameEs = "Feliz", categoryName = Category.FEELINGS.name, orderIndex = 4)
        )
        fakeBoardDao.populatePictograms(basePictos)
        repository = BoardRepository(fakeBoardDao, fakePictoDao)
    }

    @Test
    fun initializeDefaultBoardIfNeeded_createsInitialBoardWhenEmpty() = runTest {
        repository.initializeDefaultBoardIfNeeded()

        val allBoards = repository.getAllBoards().first()
        assertEquals(1, allBoards.size)
        val defaultBoard = allBoards.first()
        assertEquals("Vocabulario Esencial", defaultBoard.name)
        assertTrue(defaultBoard.isDefault)

        // Idempotencia: segunda llamada no debe duplicar
        repository.initializeDefaultBoardIfNeeded()
        val allBoardsSecond = repository.getAllBoards().first()
        assertEquals(1, allBoardsSecond.size)
    }

    @Test
    fun createBoard_createsBoardWithCustomSettingsAndPictograms() = runTest {
        val boardId = repository.createBoard(
            name = "Tablero del Colegio",
            description = "Para usar en clase",
            columnsCount = 4,
            initialPictoIds = listOf(20, 30, 40)
        )

        val board = repository.getBoardWithPictograms(boardId).first()
        assertNotNull(board)
        assertEquals("Tablero del Colegio", board!!.name)
        assertEquals(4, board.columnsCount)
        assertFalse(board.isDefault)
        assertEquals(3, board.pictograms.size)
        assertEquals(20, board.pictograms[0].id)
        assertEquals(30, board.pictograms[1].id)
        assertEquals(40, board.pictograms[2].id)
    }

    @Test
    fun updateBoard_updatesMetadataAndClampsColumns() = runTest {
        val boardId = repository.createBoard(name = "Original", columnsCount = 3)

        repository.updateBoard(boardId, name = "Modificado", description = "Nueva desc", columnsCount = 8)

        val updated = repository.getBoardWithPictograms(boardId).first()
        assertNotNull(updated)
        assertEquals("Modificado", updated!!.name)
        assertEquals("Nueva desc", updated.description)
        assertEquals(6, updated.columnsCount) // Máximo 6 columnas
    }

    @Test
    fun duplicateBoard_createsExactCopy() = runTest {
        val originalId = repository.createBoard(
            name = "Casa",
            description = "Rutinas de casa",
            columnsCount = 2,
            initialPictoIds = listOf(20, 40)
        )

        val duplicateId = repository.duplicateBoard(originalId, "Casa Copia")
        val duplicate = repository.getBoardWithPictograms(duplicateId).first()

        assertNotNull(duplicate)
        assertEquals("Casa Copia", duplicate!!.name)
        assertEquals(2, duplicate.columnsCount)
        assertEquals(2, duplicate.pictograms.size)
        assertEquals(20, duplicate.pictograms[0].id)
        assertEquals(40, duplicate.pictograms[1].id)
    }

    @Test
    fun movePictogram_reordersItemsCorrectly() = runTest {
        val boardId = repository.createBoard(
            name = "Prueba Reordenar",
            initialPictoIds = listOf(20, 30, 40) // [0: 20, 1: 30, 2: 40]
        )

        // Mover 40 a la izquierda (antes) -> nuevo orden [20, 40, 30]
        repository.movePictogram(boardId, pictoId = 40, moveLeft = true)

        val board = repository.getBoardWithPictograms(boardId).first()
        assertNotNull(board)
        assertEquals(listOf(20, 40, 30), board!!.pictograms.map { it.id })

        // Mover 20 a la derecha (después) -> nuevo orden [40, 20, 30]
        repository.movePictogram(boardId, pictoId = 20, moveLeft = false)

        val boardAfterSecond = repository.getBoardWithPictograms(boardId).first()
        assertNotNull(boardAfterSecond)
        assertEquals(listOf(40, 20, 30), boardAfterSecond!!.pictograms.map { it.id })
    }

    @Test
    fun removePictogramFromBoard_deletesOnlyTargetItem() = runTest {
        val boardId = repository.createBoard(
            name = "Prueba Eliminar",
            initialPictoIds = listOf(20, 30, 40)
        )

        repository.removePictogramFromBoard(boardId, pictoId = 30)

        val board = repository.getBoardWithPictograms(boardId).first()
        assertNotNull(board)
        assertEquals(2, board!!.pictograms.size)
        assertEquals(listOf(20, 40), board.pictograms.map { it.id })
    }

    @Test
    fun deleteBoard_removesBoardAndAssociations() = runTest {
        val boardId = repository.createBoard(
            name = "A Borrar",
            initialPictoIds = listOf(20, 30)
        )
        assertNotNull(repository.getBoardWithPictograms(boardId).first())

        repository.deleteBoard(boardId)
        val afterDelete = repository.getBoardWithPictograms(boardId).first()
        assertNull(afterDelete)
    }
}
