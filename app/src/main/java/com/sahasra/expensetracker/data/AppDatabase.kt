package com.sahasra.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Transaction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_db",
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO transactions (title, category, amount, date, isIncome) VALUES ('Monthly Salary', 'Income', 4500.00, 'Today', 1)")
                        db.execSQL("INSERT INTO transactions (title, category, amount, date, isIncome) VALUES ('Grocery Shopping', 'Food', 124.50, 'Today', 0)")
                        db.execSQL("INSERT INTO transactions (title, category, amount, date, isIncome) VALUES ('Electricity Bill', 'Utilities', 68.20, 'Yesterday', 0)")
                        db.execSQL("INSERT INTO transactions (title, category, amount, date, isIncome) VALUES ('Freelance Work', 'Income', 350.00, 'Nov 05', 1)")
                        db.execSQL("INSERT INTO transactions (title, category, amount, date, isIncome) VALUES ('Coffee & Snacks', 'Food & Drink', 12.80, 'Nov 04', 0)")
                        db.execSQL("INSERT INTO transactions (title, category, amount, date, isIncome) VALUES ('Gym Membership', 'Health', 45.00, 'Nov 02', 0)")
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
