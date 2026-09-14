package ec.edu.uteq.scli.mobile.features.auth.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedAuthStorage(context: Context) : SecureTokenStorage {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "scli_auth_secure",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun save(session: AuthSession) {
        preferences.edit()
            .putString(KEY_TOKEN_TYPE, session.tokenType)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAtMillis)
            .putString(KEY_USER_ID, session.usuario.id)
            .putString(KEY_PROFILE_ID, session.usuario.perfilId)
            .putString(KEY_USERNAME, session.usuario.username)
            .putString(KEY_NAMES, session.usuario.nombres)
            .putString(KEY_SURNAMES, session.usuario.apellidos)
            .putString(KEY_EMAIL, session.usuario.emailInstitucional)
            .putStringSet(KEY_ROLES, session.usuario.roles.toSet())
            .putStringSet(KEY_PERMISSIONS, session.usuario.permisos.toSet())
            .putStringSet(KEY_PROFILE_TYPES, session.usuario.tiposPerfil.toSet())
            .apply()
    }

    override fun read(): AuthSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        return AuthSession(
            tokenType = preferences.getString(KEY_TOKEN_TYPE, null) ?: "Bearer",
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = preferences.getLong(KEY_EXPIRES_AT, 0L),
            usuario = AuthUserResponse(
                id = userId,
                perfilId = preferences.getString(KEY_PROFILE_ID, "") ?: "",
                username = preferences.getString(KEY_USERNAME, "") ?: "",
                nombres = preferences.getString(KEY_NAMES, "") ?: "",
                apellidos = preferences.getString(KEY_SURNAMES, "") ?: "",
                emailInstitucional = preferences.getString(KEY_EMAIL, "") ?: "",
                roles = preferences.getStringSet(KEY_ROLES, emptySet())?.toList() ?: emptyList(),
                permisos = preferences.getStringSet(KEY_PERMISSIONS, emptySet())?.toList() ?: emptyList(),
                tiposPerfil = preferences.getStringSet(KEY_PROFILE_TYPES, emptySet())?.toList() ?: emptyList(),
            ),
        )
    }

    override fun clear() = preferences.edit().clear().apply()

    private companion object {
        const val KEY_TOKEN_TYPE = "token_type"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_USER_ID = "user_id"
        const val KEY_PROFILE_ID = "profile_id"
        const val KEY_USERNAME = "username"
        const val KEY_NAMES = "names"
        const val KEY_SURNAMES = "surnames"
        const val KEY_EMAIL = "email"
        const val KEY_ROLES = "roles"
        const val KEY_PERMISSIONS = "permissions"
        const val KEY_PROFILE_TYPES = "profile_types"
    }
}
