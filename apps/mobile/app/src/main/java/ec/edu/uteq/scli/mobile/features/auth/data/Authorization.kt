package ec.edu.uteq.scli.mobile.features.auth.data

fun AuthUserResponse.hasPermission(permission: String): Boolean = permission in permisos
fun AuthUserResponse.hasAnyPermission(vararg permissions: String): Boolean =
    permissions.any(permisos::contains)
fun AuthUserResponse.hasRole(role: String): Boolean = role in roles
