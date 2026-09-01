import { render, screen } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  AccessDeniedPage,
  NotFoundPage,
  RouteErrorBoundary,
} from './RouteFeedback'

function BrokenRoute(): ReactElement {
  throw new Error('fallo controlado')
}

describe('RouteFeedback', () => {
  afterEach(() => vi.restoreAllMocks())

  it('muestra estados navegables para 403 y 404', () => {
    const { rerender } = render(
      <MemoryRouter><AccessDeniedPage /></MemoryRouter>,
    )
    expect(screen.getByRole('heading', { name: 'No tienes permiso' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/main')

    rerender(<MemoryRouter><NotFoundPage /></MemoryRouter>)
    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument()
  })

  it('renderiza normalmente y recupera una excepción de ruta', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    const { rerender } = render(
      <MemoryRouter><RouteErrorBoundary><p>Vista disponible</p></RouteErrorBoundary></MemoryRouter>,
    )
    expect(screen.getByText('Vista disponible')).toBeInTheDocument()

    rerender(
      <MemoryRouter><RouteErrorBoundary><BrokenRoute /></RouteErrorBoundary></MemoryRouter>,
    )
    expect(screen.getByRole('heading', { name: 'No se pudo mostrar esta página' })).toBeInTheDocument()
    expect(consoleError).toHaveBeenCalled()
  })
})
