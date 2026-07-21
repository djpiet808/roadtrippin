package com.roadtrippin.shared.data

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual object DatabaseFactory {
    actual fun build(): RoomDatabase.Builder<RoadtrippinDatabase> {
        val documentUrl = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        val directory = requireNotNull(documentUrl?.path) { "Unable to locate the iOS documents directory" }
        return Room.databaseBuilder<RoadtrippinDatabase>(name = "$directory/roadtrippin.db")
    }
}
