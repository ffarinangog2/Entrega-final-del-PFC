import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { DashboardLayout } from '../components/DashboardLayout'
import '../i18n'
import { usePreferencesStore, type Idioma, type Tema } from '../store/preferencesStore'
import './SettingsPage.css'

export function SettingsPage() {
  const { t } = useTranslation()
  const idioma = usePreferencesStore((state) => state.idioma)
  const tema = usePreferencesStore((state) => state.tema)
  const setIdioma = usePreferencesStore((state) => state.setIdioma)
  const setTema = usePreferencesStore((state) => state.setTema)
  const [aviso, setAviso] = useState('')

  function onIdiomaChange(nuevoIdioma: Idioma) {
    setIdioma(nuevoIdioma)
    setAviso(`${t('settings.language.label')}: ${t(`settings.language.${nuevoIdioma}`)}`)
  }

  function onTemaChange(nuevoTema: Tema) {
    setTema(nuevoTema)
    setAviso(`${t('settings.theme.label')}: ${t(`settings.theme.${nuevoTema}`)}`)
  }

  return (
    <DashboardLayout breadcrumb={t('settings.title')}>
      <div className="settings-page">
        <h1>{t('settings.title')}</h1>
        <p className="settings-page__subtitle">{t('settings.subtitle')}</p>

        <section className="settings-page__section">
          <label htmlFor="settings-idioma" className="settings-page__label">
            {t('settings.language.label')}
          </label>
          <p className="settings-page__description">{t('settings.language.description')}</p>
          <select
            id="settings-idioma"
            className="settings-page__select"
            value={idioma}
            onChange={(event) => onIdiomaChange(event.target.value as Idioma)}
          >
            <option value="es">{t('settings.language.es')}</option>
            <option value="en">{t('settings.language.en')}</option>
          </select>
        </section>

        <section className="settings-page__section">
          <span id="settings-tema-label" className="settings-page__label">
            {t('settings.theme.label')}
          </span>
          <p className="settings-page__description">{t('settings.theme.description')}</p>
          <div className="settings-page__theme-group" role="group" aria-labelledby="settings-tema-label">
            <button
              type="button"
              className="settings-page__theme-option"
              aria-pressed={tema === 'light'}
              onClick={() => onTemaChange('light')}
            >
              {t('settings.theme.light')}
            </button>
            <button
              type="button"
              className="settings-page__theme-option"
              aria-pressed={tema === 'dark'}
              onClick={() => onTemaChange('dark')}
            >
              {t('settings.theme.dark')}
            </button>
          </div>
        </section>

        <p className="settings-page__status" role="status" aria-live="polite">
          {aviso}
        </p>
      </div>
    </DashboardLayout>
  )
}
