package com.catchuppos.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "icon_color")
    val iconColor: String = "#FF6600", // hex color for the icon background

    @ColumnInfo(name = "icon_char")
    val iconChar: String = "☕", // emoji/char for the category icon

    @ColumnInfo(name = "auto_type")
    val autoType: String = "DRINK", // "DRINK" or "FOOD" — auto-set product type
)
