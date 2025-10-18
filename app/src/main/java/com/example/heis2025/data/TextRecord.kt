package com.example.heis2025.data

data class TextRecord(
    val id: Long = 0,
    val text: String,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
