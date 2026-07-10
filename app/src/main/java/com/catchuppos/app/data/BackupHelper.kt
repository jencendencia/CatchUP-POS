package com.catchuppos.app.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupHelper {

    /**
     * Backup database and images to a zip output stream.
     * Runs a WAL checkpoint first so the main db file is fully up to date.
     */
    fun backup(context: Context, output: OutputStream, database: SupportSQLiteDatabase? = null) {
        // Force a WAL checkpoint so all data is flushed to the main db file
        try {
            database?.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
        } catch (_: Exception) {
            // Best-effort; some environments may not support this
        }

        val dbFile = context.getDatabasePath("catchup_pos.db")
        val productImagesDir = File(context.filesDir, "product_images")
        val profileImagesDir = File(context.filesDir, "profile_images")

        ZipOutputStream(output).use { zip ->
            if (dbFile.exists()) {
                zip.putNextEntry(ZipEntry("catchup_pos.db"))
                dbFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }

            if (productImagesDir.exists()) {
                productImagesDir.listFiles()?.forEach { file ->
                    zip.putNextEntry(ZipEntry("product_images/${file.name}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }

            if (profileImagesDir.exists()) {
                profileImagesDir.listFiles()?.forEach { file ->
                    zip.putNextEntry(ZipEntry("profile_images/${file.name}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    fun restore(context: Context, input: InputStream): Boolean {
        return try {
            // Delete old database files (including WAL/SHM) before restoring
            val dbFile = context.getDatabasePath("catchup_pos.db")
            dbFile.delete()
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            File(dbFile.path + "-journal").delete()

            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == "catchup_pos.db" -> {
                            dbFile.parentFile?.mkdirs()
                            dbFile.outputStream().use { zip.copyTo(it) }
                        }
                        name.startsWith("product_images/") -> {
                            val fileName = name.removePrefix("product_images/")
                            val dir = File(context.filesDir, "product_images")
                            if (!dir.exists()) dir.mkdirs()
                            val file = File(dir, fileName)
                            file.outputStream().use { zip.copyTo(it) }
                        }
                        name.startsWith("profile_images/") -> {
                            val fileName = name.removePrefix("profile_images/")
                            val dir = File(context.filesDir, "profile_images")
                            if (!dir.exists()) dir.mkdirs()
                            val file = File(dir, fileName)
                            file.outputStream().use { zip.copyTo(it) }
                        }
                    }
                    entry = zip.nextEntry
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
