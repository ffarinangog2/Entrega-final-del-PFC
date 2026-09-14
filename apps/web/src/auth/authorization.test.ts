import { renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { hasAnyPermission, hasPermission, hasRole } from './authorization'
import { useAuth } from './useAuth'
import type { AuthUser } from '../types/auth'

function createAuthUser(roles: string[] = [], permisos: string[] = []): AuthUser {
  return {
    id: '1',
    perfilId: 'p1',
    username: 'u',
    nombres: 'Docente',
    apellidos: 'Prueba',
    emailInstitucional: 'docente@epn.edu.ec',
    roles,
    permisos,
    tiposPerfil: ['DOCENTE'],
  }
}

describe('authorization helpers', () => {
  it('hasPermission valida permisos con usuario nulo o definido', () => {
    expect(hasPermission(null, 'PERM_1')).toBe(false)
    expect(hasPermission(undefined, 'PERM_1')).toBe(false)
    expect(hasPermission({} as AuthUser, 'PERM_1')).toBe(false)

    const user = createAuthUser(['DOCENTE'], ['PERM_1'])
    expect(hasPermission(user, 'PERM_1')).toBe(true)
    expect(hasPermission(user, 'PERM_2')).toBe(false)
  })

  it('hasAnyPermission valida listas de permisos', () => {
    expect(hasAnyPermission(null, ['PERM_1'])).toBe(false)

    const user = createAuthUser(['DOCENTE'], ['PERM_1'])
    expect(hasAnyPermission(user, ['PERM_1', 'PERM_2'])).toBe(true)
    expect(hasAnyPermission(user, ['PERM_2', 'PERM_3'])).toBe(false)
  })

  it('hasRole maneja prefijos ROLE_ y casos nulos', () => {
    expect(hasRole(null, 'ADMIN')).toBe(false)
    expect(hasRole(undefined, 'ADMIN')).toBe(false)
    expect(hasRole({} as AuthUser, 'ADMIN')).toBe(false)

    const userWithPrefix = createAuthUser(['ROLE_ADMINISTRADOR'], [])
    expect(hasRole(userWithPrefix, 'ADMINISTRADOR')).toBe(true)
    expect(hasRole(userWithPrefix, 'ROLE_ADMINISTRADOR')).toBe(true)
    expect(hasRole(userWithPrefix, 'DOCENTE')).toBe(false)

    const userWithoutPrefix = createAuthUser(['DOCENTE'], [])
    expect(hasRole(userWithoutPrefix, 'ROLE_DOCENTE')).toBe(true)
    expect(hasRole(userWithoutPrefix, 'DOCENTE')).toBe(true)
  })

  it('useAuth lanza error fuera de proveedor', () => {
    expect(() => renderHook(() => useAuth())).toThrow('useAuth debe utilizarse dentro de AuthProvider')
  })
})
