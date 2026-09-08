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
  it('muestra diálogo de confirmación y permite cancelar', () => {
    const logout = vi.fn().mockResolvedValue(undefined)
    renderButton(logout)

    fireEvent.click(screen.getByRole('button', { name: /cerrar sesión/i }))
    expect(screen.getByRole('dialog')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /cancelar/i }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(logout).not.toHaveBeenCalled()
  })

  it('redirige al login después de confirmar el cierre de sesión', async () => {
    const logout = vi.fn().mockResolvedValue(undefined)
    renderButton(logout)

    fireEvent.click(screen.getByRole('button', { name: /cerrar sesión/i }))
    expect(screen.getByRole('dialog')).toBeInTheDocument()

    const confirmBtn = screen.getAllByRole('button', { name: /cerrar sesión/i })[1]
    fireEvent.click(confirmBtn)

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/login'))
    expect(logout).toHaveBeenCalledOnce()
  })

  it('redirige al login incluso si el cierre de sesión rechaza', async () => {
    renderButton(vi.fn().mockRejectedValue(new Error('offline')))

    fireEvent.click(screen.getByRole('button', { name: /cerrar sesión/i }))
    const confirmBtn = screen.getAllByRole('button', { name: /cerrar sesión/i })[1]
    fireEvent.click(confirmBtn)

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/login'))
  })
})
