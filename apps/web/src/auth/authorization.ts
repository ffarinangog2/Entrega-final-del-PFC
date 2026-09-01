import type { AuthUser } from '../types/auth'

export function hasPermission(usuario: AuthUser | null | undefined, permission: string) {
  return usuario?.permisos?.includes(permission) ?? false
}

export function hasAnyPermission(
  usuario: AuthUser | null | undefined,
  permissions: readonly string[],
) {
  return permissions.some((permission) => hasPermission(usuario, permission))
}

export function hasRole(usuario: AuthUser | null | undefined, role: string) {
  const expected = role.replace(/^ROLE_/, '')
  return usuario?.roles?.some((value) => value.replace(/^ROLE_/, '') === expected) ?? false
}
