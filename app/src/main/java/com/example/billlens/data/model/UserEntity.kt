package com.example.billlens.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String?,
    val email: String?,
    val photoUrl: String?
)