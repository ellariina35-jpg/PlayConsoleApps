package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WorkdayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkdayDao {
    @Query("SELECT * FROM workdays ORDER BY year ASC, month ASC, day ASC, startTime ASC")
    fun getAllWorkdays(): Flow<List<WorkdayEntity>>

    @Query("SELECT * FROM workdays WHERE (year > :curYear) OR (year = :curYear AND month > :curMonth) OR (year = :curYear AND month = :curMonth AND day >= :curDay) ORDER BY year ASC, month ASC, day ASC, startTime ASC")
    fun getUpcomingWorkdays(curYear: Int, curMonth: Int, curDay: Int): Flow<List<WorkdayEntity>>

    @Query("SELECT * FROM workdays WHERE year = :year AND month = :month ORDER BY day ASC, startTime ASC")
    fun getWorkdaysForMonth(year: Int, month: Int): Flow<List<WorkdayEntity>>

    @Query("SELECT * FROM workdays WHERE isSyncedToGoogle = 0 ORDER BY year ASC, month ASC, day ASC")
    fun getUnsyncedWorkdays(): Flow<List<WorkdayEntity>>

    @Query("SELECT * FROM workdays WHERE id = :id LIMIT 1")
    suspend fun getWorkdayById(id: Long): WorkdayEntity?

    @Query("SELECT * FROM workdays WHERE day = :day AND month = :month AND year = :year LIMIT 1")
    suspend fun getWorkdayForDate(day: Int, month: Int, year: Int): WorkdayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkday(workday: WorkdayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkdays(workdays: List<WorkdayEntity>): List<Long>

    @Update
    suspend fun updateWorkday(workday: WorkdayEntity)

    @Delete
    suspend fun deleteWorkday(workday: WorkdayEntity)

    @Query("DELETE FROM workdays WHERE id = :id")
    suspend fun deleteWorkdayById(id: Long)

    @Query("DELETE FROM workdays")
    suspend fun clearAll()
}
