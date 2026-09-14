import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import i18n from '../i18n'

export type Idioma = 'es' | 'en'
export type Tema = 'light' | 'dark'

interface PreferenciasState {
  idioma: Idioma
  tema: Tema
  setIdioma: (idioma: Idioma) => void
  setTema: (tema: Tema) => void
  toggleTema: () => void
}

function aplicarTema(tema: Tema) {
  if (typeof document === 'undefined') return
  document.documentElement.setAttribute('data-theme', tema)
}

// Zustand en lugar de Redux Toolkit: el estado global de este bloque se
// limita a dos preferencias primitivas (idioma y tema) sin lógica de
// reducers/thunks/normalización que justifique el boilerplate de RTK.
// Zustand da un store persistido en pocas líneas y sin <Provider>.
export const usePreferencesStore = create<PreferenciasState>()(
  persist(
    (set, get) => ({
      idioma: 'es',
      tema: 'light',
      setIdioma: (idioma) => {
        set({ idioma })
        void i18n.changeLanguage(idioma)
      },
      setTema: (tema) => {
        set({ tema })
        aplicarTema(tema)
      },
      toggleTema: () => {
        const siguiente: Tema = get().tema === 'light' ? 'dark' : 'light'
        get().setTema(siguiente)
      },
    }),
    {
      name: 'scli-preferencias',
      onRehydrateStorage: () => (state) => {
        if (state) {
          aplicarTema(state.tema)
          void i18n.changeLanguage(state.idioma)
        }
      },
    },
  ),
)

aplicarTema(usePreferencesStore.getState().tema)
