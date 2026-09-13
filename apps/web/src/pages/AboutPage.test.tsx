import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { usePreferencesStore } from '../store/preferencesStore'
import { AboutPage } from './AboutPage'

vi.mock('../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

function renderPage() {
  render(
    <MemoryRouter>
      <AboutPage />
    </MemoryRouter>,
  )
}

describe('AboutPage', () => {
  beforeEach(() => {
    window.localStorage.clear()
    usePreferencesStore.getState().setIdioma('es')
  })

  it('muestra el nombre del sistema, la versión y el stack', () => {
    renderPage()
    expect(screen.getByRole('heading', { name: 'Acerca de SCLI' })).toBeInTheDocument()
    expect(screen.getByText('Versión')).toBeInTheDocument()
    expect(screen.getByText('0.1.0')).toBeInTheDocument()
    expect(screen.getByText('Stack tecnológico')).toBeInTheDocument()
  })

  it('traduce el contenido cuando el idioma es inglés', () => {
    usePreferencesStore.getState().setIdioma('en')
    renderPage()
    expect(screen.getByRole('heading', { name: 'About SCLI' })).toBeInTheDocument()
    expect(screen.getByText('Version')).toBeInTheDocument()
  })
})
