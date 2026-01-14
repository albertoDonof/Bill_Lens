package com.example.billlens.data.local

import com.example.billlens.data.model.UserData
import com.example.billlens.data.model.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.text.clear

class UserLocalDataSource @Inject constructor(
    private val dao: UserDao
) {
    fun getUserFlow(): Flow<UserData?> =
        dao.getUserFlow().map { it?.toUserData() }

    suspend fun save(user: UserData) = dao.upsert(user.toEntity())

    suspend fun clear() = dao.clear()

    private fun UserEntity.toUserData() = UserData(
        id = uid,
        displayName = name,
        email = email,
        profilePictureUrl = photoUrl,
        isAuthenticated = true
    )

    private fun UserData.toEntity() = UserEntity(
        uid = id,
        name = displayName,
        email = email,
        photoUrl = profilePictureUrl
    )
}