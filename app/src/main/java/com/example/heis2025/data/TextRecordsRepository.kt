package com.example.heis2025.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.example.heis2025.data.TextRecordsProvider.Companion.CONTENT_URI

class TextRecordsRepository(private val context: Context) {
    
    fun saveText(text: String): Long {
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_TEXT, text)
            put(DatabaseHelper.COLUMN_IS_DELETED, 0)
            put(DatabaseHelper.COLUMN_CREATED_AT, System.currentTimeMillis())
        }
        
        val uri = context.contentResolver.insert(CONTENT_URI, values)
        return uri?.lastPathSegment?.toLong() ?: -1
    }
    
    fun updateText(id: Long, newText: String): Boolean {
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_TEXT, newText)
        }
        
        val uri = Uri.parse("$CONTENT_URI/$id")
        val updatedRows = context.contentResolver.update(uri, values, null, null)
        return updatedRows > 0
    }
    
    fun deleteText(id: Long): Boolean {
        val uri = Uri.parse("$CONTENT_URI/$id")
        val deletedRows = context.contentResolver.delete(uri, null, null)
        return deletedRows > 0
    }
    
    fun getLastActiveRecord(): TextRecord? {
        val cursor = context.contentResolver.query(
            CONTENT_URI,
            null,
            "${DatabaseHelper.COLUMN_IS_DELETED} = ?",
            arrayOf("0"),
            "${DatabaseHelper.COLUMN_CREATED_AT} DESC"
        )
        
        return cursor?.use {
            if (it.moveToFirst()) {
                TextRecord(
                    id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)),
                    text = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TEXT)),
                    isDeleted = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_DELETED)) == 1,
                    createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT))
                )
            } else null
        }
    }
    
    fun getAllRecords(): List<TextRecord> {
        val cursor = context.contentResolver.query(
            CONTENT_URI,
            null,
            null,
            null,
            "${DatabaseHelper.COLUMN_CREATED_AT} DESC"
        )
        
        return cursor?.use {
            val records = mutableListOf<TextRecord>()
            while (it.moveToNext()) {
                records.add(
                    TextRecord(
                        id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)),
                        text = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TEXT)),
                        isDeleted = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_DELETED)) == 1,
                        createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT))
                    )
                )
            }
            records
        } ?: emptyList()
    }
    
    fun getActiveRecords(): List<TextRecord> {
        val cursor = context.contentResolver.query(
            CONTENT_URI,
            null,
            "${DatabaseHelper.COLUMN_IS_DELETED} = ?",
            arrayOf("0"),
            "${DatabaseHelper.COLUMN_CREATED_AT} DESC"
        )
        
        return cursor?.use {
            val records = mutableListOf<TextRecord>()
            while (it.moveToNext()) {
                records.add(
                    TextRecord(
                        id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)),
                        text = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TEXT)),
                        isDeleted = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_DELETED)) == 1,
                        createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT))
                    )
                )
            }
            records
        } ?: emptyList()
    }
    
    fun getDeletedRecords(): List<TextRecord> {
        val cursor = context.contentResolver.query(
            CONTENT_URI,
            null,
            "${DatabaseHelper.COLUMN_IS_DELETED} = ?",
            arrayOf("1"),
            "${DatabaseHelper.COLUMN_CREATED_AT} DESC"
        )
        
        return cursor?.use {
            val records = mutableListOf<TextRecord>()
            while (it.moveToNext()) {
                records.add(
                    TextRecord(
                        id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)),
                        text = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TEXT)),
                        isDeleted = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_DELETED)) == 1,
                        createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT))
                    )
                )
            }
            records
        } ?: emptyList()
    }
    
    fun getRecordsCount(): Int {
        val cursor = context.contentResolver.query(
            CONTENT_URI,
            arrayOf("COUNT(*) as count"),
            null,
            null,
            null
        )
        
        return cursor?.use {
            if (it.moveToFirst()) {
                it.getInt(0)
            } else 0
        } ?: 0
    }
    
    fun getActiveRecordsCount(): Int {
        val cursor = context.contentResolver.query(
            CONTENT_URI,
            arrayOf("COUNT(*) as count"),
            "${DatabaseHelper.COLUMN_IS_DELETED} = ?",
            arrayOf("0"),
            null
        )
        
        return cursor?.use {
            if (it.moveToFirst()) {
                it.getInt(0)
            } else 0
        } ?: 0
    }
    
    fun getDeletedRecordsCount(): Int {
        val cursor = context.contentResolver.query(
            CONTENT_URI,
            arrayOf("COUNT(*) as count"),
            "${DatabaseHelper.COLUMN_IS_DELETED} = ?",
            arrayOf("1"),
            null
        )
        
        return cursor?.use {
            if (it.moveToFirst()) {
                it.getInt(0)
            } else 0
        } ?: 0
    }
}
