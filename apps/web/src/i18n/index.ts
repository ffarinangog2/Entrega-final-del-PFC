import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import en from './locales/en.json'
import es from './locales/es.json'

const PREFERENCES_STORAGE_KEY = 'scli-preferencias'

function idiomaPersistido(): 'es' | 'en' {
  if (typeof window === 'undefined') return 'es'
  try {
    const raw = window.localStorage.getItem(PREFERENCES_STORAGE_KEY)
    if (!raw) return 'es'
    const parsed = JSON.parse(raw) as { state?: { idioma?: string } }
    return parsed.state?.idioma === 'en' ? 'en' : 'es'
  } catch {
    return 'es'
  }
}

if (!i18n.isInitialized) {
  void i18n.use(initReactI18next).init({
    resources: {
      es: { translation: es },
      en: { translation: en },
    },
    lng: idiomaPersistido(),
    fallbackLng: 'es',
    interpolation: { escapeValue: false },
  })
}

export default i18n
