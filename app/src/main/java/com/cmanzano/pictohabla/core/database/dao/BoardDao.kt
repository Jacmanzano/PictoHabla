package com.cmanzano.pictohabla.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cmanzano.pictohabla.core.database.entity.BoardEntity
import com.cmanzano.pictohabla.core.database.entity.BoardPictogramCrossRef
import com.cmanzano.pictohabla.core.database.entity.PictogramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {

    @Query("SELECT * FROM boards ORDER BY isDefault DESC, createdAt ASC")
    fun getAllBoardsFlow(): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards WHERE id = :id LIMIT 1")
    suspend fun getBoardById(id: Long): BoardEntity?

    @Query("SELECT * FROM boards WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultBoard(): BoardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoard(board: BoardEntity): Long

    @Update
    suspend fun updateBoard(board: BoardEntity)

    @Query("DELETE FROM boards WHERE id = :id AND isDefault = 0")
    suspend fun deleteBoard(id: Long)

    @Query("SELECT COUNT(*) FROM boards")
    suspend fun getBoardCount(): Int

    // Gestión de pictogramas asociados a un tablero
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<BoardPictogramCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(ref: BoardPictogramCrossRef)

    @Query("DELETE FROM board_pictograms WHERE boardId = :boardId")
    suspend fun deleteCrossRefsForBoard(boardId: Long)

    @Query("DELETE FROM board_pictograms WHERE boardId = :boardId AND pictoId = :pictoId")
    suspend fun removePictoFromBoard(boardId: Long, pictoId: Int)

    @Query(
        """
        SELECT p.* FROM pictograms p
        INNER JOIN board_pictograms bp ON p.id = bp.pictoId
        WHERE bp.boardId = :boardId
        ORDER BY bp.position ASC
        """
    )
    fun getPictogramsForBoardFlow(boardId: Long): Flow<List<PictogramEntity>>

    @Query(
        """
        SELECT p.* FROM pictograms p
        INNER JOIN board_pictograms bp ON p.id = bp.pictoId
        WHERE bp.boardId = :boardId
        ORDER BY bp.position ASC
        """
    )
    suspend fun getPictogramsForBoard(boardId: Long): List<PictogramEntity>

    @Query("SELECT MAX(position) FROM board_pictograms WHERE boardId = :boardId")
    suspend fun getMaxPosition(boardId: Long): Int?

    @Transaction
    suspend fun reorderPictograms(boardId: Long, orderedPictoIds: List<Int>) {
        deleteCrossRefsForBoard(boardId)
        val refs = orderedPictoIds.mapIndexed { index, pictoId ->
            BoardPictogramCrossRef(boardId = boardId, pictoId = pictoId, position = index)
        }
        insertCrossRefs(refs)
    }
}
