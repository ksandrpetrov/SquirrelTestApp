package com.example.heis2025.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri

class TextRecordsProvider : ContentProvider() {
    
    private lateinit var dbHelper: DatabaseHelper
    
    companion object {
        const val AUTHORITY = "com.example.heis2025.provider"
        const val BASE_PATH = "text_records"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$BASE_PATH")
        
        private const val RECORDS = 1
        private const val RECORD_ID = 2
        
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, BASE_PATH, RECORDS)
            addURI(AUTHORITY, "$BASE_PATH/#", RECORD_ID)
        }
    }

    override fun onCreate(): Boolean {
        dbHelper = DatabaseHelper(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor
        
        when (uriMatcher.match(uri)) {
            RECORDS -> {
                cursor = db.query(
                    DatabaseHelper.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
                )
            }
            RECORD_ID -> {
                val id = uri.lastPathSegment
                cursor = db.query(
                    DatabaseHelper.TABLE_NAME,
                    projection,
                    "${DatabaseHelper.COLUMN_ID} = ?",
                    arrayOf(id),
                    null,
                    null,
                    sortOrder
                )
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            RECORDS -> "vnd.android.cursor.dir/vnd.com.example.heis2025.text_records"
            RECORD_ID -> "vnd.android.cursor.item/vnd.com.example.heis2025.text_records"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase
        val id = db.insert(DatabaseHelper.TABLE_NAME, null, values)
        return Uri.parse("$CONTENT_URI/$id")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = dbHelper.writableDatabase
        
        return when (uriMatcher.match(uri)) {
            RECORDS -> {
                val values = ContentValues().apply {
                    put(DatabaseHelper.COLUMN_IS_DELETED, 1)
                }
                db.update(DatabaseHelper.TABLE_NAME, values, selection, selectionArgs)
            }
            RECORD_ID -> {
                val id = uri.lastPathSegment
                val values = ContentValues().apply {
                    put(DatabaseHelper.COLUMN_IS_DELETED, 1)
                }
                db.update(
                    DatabaseHelper.TABLE_NAME,
                    values,
                    "${DatabaseHelper.COLUMN_ID} = ?",
                    arrayOf(id)
                )
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val db = dbHelper.writableDatabase
        
        return when (uriMatcher.match(uri)) {
            RECORDS -> db.update(DatabaseHelper.TABLE_NAME, values, selection, selectionArgs)
            RECORD_ID -> {
                val id = uri.lastPathSegment
                db.update(
                    DatabaseHelper.TABLE_NAME,
                    values,
                    "${DatabaseHelper.COLUMN_ID} = ?",
                    arrayOf(id)
                )
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}
