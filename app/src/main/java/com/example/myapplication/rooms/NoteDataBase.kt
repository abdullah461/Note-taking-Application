package com.example.myapplication.rooms
import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.omgodse.note_taker.room.dao.BaseNoteDao
import com.omgodse.note_taker.room.dao.CommonDao
import com.omgodse.note_taker.room.dao.LabelDao

@TypeConverters(Converters::class)
@Database(entities = [BaseNote::class, Label::class], version = 1)
abstract class NotallyDatabase : RoomDatabase() {

    abstract val labelDao: LabelDao
    abstract val commonDao: CommonDao
    abstract val baseNoteDao: BaseNoteDao

    companion object {

        private const val databaseName = "NotallyDatabase"

        @Volatile
        private var instance: NotallyDatabase? = null

        fun getDatabase(application: Application): NotallyDatabase {
            return instance ?: synchronized(this) {
                val instance = Room.databaseBuilder(application, NotallyDatabase::class.java, databaseName).build()
                this.instance = instance
                return instance
            }
        }
    }
}