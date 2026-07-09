package com.catchuppos.app.data

import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    suspend fun login(usernameOrEmail: String, password: String): UserEntity? {
        return userDao.login(usernameOrEmail, password)
    }

    suspend fun getUserById(id: Int): UserEntity? {
        return userDao.getUserById(id)
    }

    fun getAllUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }

    suspend fun getAllUsersOnce(): List<UserEntity> {
        return userDao.getAllUsersOnce()
    }

    suspend fun insertUser(user: UserEntity): Long {
        return userDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun deleteUserById(id: Int) {
        userDao.deleteUserById(id)
    }

    suspend fun getUserCount(): Int {
        return userDao.getUserCount()
    }

    suspend fun hasAdmin(): Boolean {
        return userDao.hasAdmin()
    }

    suspend fun seedDefaultAdmin() {
        if (!hasAdmin()) {
            insertUser(
                UserEntity(
                    username = "admin",
                    email = "admin@catchuppos.com",
                    password = "admin123",
                    role = UserRole.ADMIN.name,
                    isActive = true
                )
            )
        }
    }
}
