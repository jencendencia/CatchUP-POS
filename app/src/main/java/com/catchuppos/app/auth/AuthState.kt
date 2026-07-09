package com.catchuppos.app.auth

import com.catchuppos.app.data.UserEntity
import com.catchuppos.app.data.UserRole

object AuthState {
    var currentUser: UserEntity? = null
        private set

    val isAdmin: Boolean
        get() = currentUser?.role == UserRole.ADMIN.name

    val isUser: Boolean
        get() = currentUser?.role == UserRole.USER.name

    val isLoggedIn: Boolean
        get() = currentUser != null

    fun login(user: UserEntity) {
        currentUser = user
    }

    fun logout() {
        currentUser = null
    }
}
