package com.roadtrippin.shared.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual object DatabaseFactory {
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    actual fun build(): RoomDatabase.Builder<RoadtrippinDatabase> {
        val appContext = checkNotNull(context) { "DatabaseFactory.initialize must run before creating the app store" }
        val path = appContext.getDatabasePath("roadtrippin.db").absolutePath
        return Room.databaseBuilder<RoadtrippinDatabase>(appContext, path)
    }
}
