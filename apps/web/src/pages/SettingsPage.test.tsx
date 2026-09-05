import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { usePreferencesStore } from '../store/preferencesStore'
import { SettingsPage } from './SettingsPage'

vi.mock('../components/DashboardLayout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

function renderPage() {
  render(
    <MemoryRouter>
      <SettingsPage />
    </MemoryRouter>,
  )
}

describe('SettingsPage', () => {
  beforeEach(() => {
    window.localStorage.clear()
    usePreferencesStore.getState().setIdioma('es')
    usePreferencesStore.getState().setTema('light')
  })

  it('muestra el título y los selectores en español por defecto', () => {
    renderPage()
    expect(screen.getByRole('heading', { name: 'Configuración' })).toBeInTheDocument()
    expect(screen.getByLabelText('Idioma')).toBeInTheDocument()
    expect(screen.getByRole('group', { name: 'Tema' })).toBeInTheDocument()
  })

  it('cambia el idioma de la interfaz al seleccionar English', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.selectOptions(screen.getByLabelText('Idioma'), 'en')

    expect(await screen.findByRole('heading', { name: 'Settings' })).toBeInTheDocument()
    expect(usePreferencesStore.getState().idioma).toBe('en')
  })

  it('cambia el tema y refleja el estado en aria-pressed y en <html data-theme>', async () => {
    const user = userEvent.setup()
    renderPage()

    const botonOscuro = screen.getByRole('button', { name: 'Oscuro' })
    expect(botonOscuro).toHaveAttribute('aria-pressed', 'false')

    await user.click(botonOscuro)

    expect(botonOscuro).toHaveAttribute('aria-pressed', 'true')
    expect(usePreferencesStore.getState().tema).toBe('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('permite operar los selectores solo con teclado', async () => {
    const user = userEvent.setup()
    renderPage()

    const botonClaro = screen.getByRole('button', { name: 'Claro' })
    botonClaro.focus()
    expect(botonClaro).toHaveFocus()

    await user.keyboard('{Enter}')
    expect(usePreferencesStore.getState().tema).toBe('light')
  })
})
