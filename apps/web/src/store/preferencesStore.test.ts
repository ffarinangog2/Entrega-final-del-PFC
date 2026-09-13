import { describe, expect, it } from 'vitest'
import { usePreferencesStore } from './preferencesStore'

describe('preferencesStore', () => {
  it('permite cambiar el idioma', () => {
    usePreferencesStore.getState().setIdioma('en')
    expect(usePreferencesStore.getState().idioma).toBe('en')

    usePreferencesStore.getState().setIdioma('es')
    expect(usePreferencesStore.getState().idioma).toBe('es')
  })

  it('permite alternar y fijar el tema visual', () => {
    usePreferencesStore.getState().setTema('light')
    expect(usePreferencesStore.getState().tema).toBe('light')

    usePreferencesStore.getState().toggleTema()
    expect(usePreferencesStore.getState().tema).toBe('dark')

    usePreferencesStore.getState().toggleTema()
    expect(usePreferencesStore.getState().tema).toBe('light')

    usePreferencesStore.getState().setTema('dark')
    expect(usePreferencesStore.getState().tema).toBe('dark')
  })
})
