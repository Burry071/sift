package com.sift.data

import android.content.Context
import androidx.room.Room

object Db {
    @Volatile
    private var instance: SiftDb? = null

    fun get(context: Context): SiftDb =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SiftDb::class.java,
                "sift.db"
            ).build().also { instance = it }
        }
}
