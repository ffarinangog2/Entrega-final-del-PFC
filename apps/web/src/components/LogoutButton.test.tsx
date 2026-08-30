import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext, type AuthContextValue } from '../auth/context'
import { LogoutButton } from './LogoutButton'

function LocationProbe() {
  return <span data-testid="location">{useLocation().pathname}</span>
}

function renderButton(logout: AuthContextValue['logout']) {
  const value: AuthContextValue = {
    usuario: null,
    isAuthenticated: true,
    isLoading: false,
    login: vi.fn(),
    logout,
    refreshSession: vi.fn(),
  }
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/main']}>
        <LogoutButton />
        <LocationProbe />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

describe('LogoutButton', () => {
  it('redirige al login después de cerrar la sesión', async () => {
    const logout = vi.fn().mockResolvedValue(undefined)
    renderButton(logout)

    fireEvent.click(screen.getByRole('button', { name: /cerrar sesión/i }))

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/login'))
    expect(logout).toHaveBeenCalledOnce()
  })

  it('redirige al login incluso si el cierre de sesión rechaza', async () => {
    renderButton(vi.fn().mockRejectedValue(new Error('offline')))

    fireEvent.click(screen.getByRole('button', { name: /cerrar sesión/i }))

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/login'))
  })
})
