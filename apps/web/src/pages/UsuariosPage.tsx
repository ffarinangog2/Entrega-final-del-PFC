import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { DashboardLayout } from '../components/DashboardLayout'
import '../i18n'
import { crearPerfil, listarPerfiles, UsuariosApiError, type CrearPerfilRequest, type Perfil } from '../services/usuariosApi'
import './UsuariosPage.css'

const FORM_INICIAL: CrearPerfilRequest = {
  identificacion: '',
  nombres: '',
  apellidos: '',
  emailInstitucional: '',
  emailPersonal: '',
  telefono: '',
  direccion: '',
  fechaNacimiento: '',
}

export function UsuariosPage() {
  const { t } = useTranslation()
  const [perfiles, setPerfiles] = useState<Perfil[] | null>(null)
  const [loadError, setLoadError] = useState('')
  const [form, setForm] = useState<CrearPerfilRequest>(FORM_INICIAL)
  const [enviando, setEnviando] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [submitStatus, setSubmitStatus] = useState('')

  function cargarPerfiles() {
    setPerfiles(null)
    setLoadError('')
    listarPerfiles()
      .then(setPerfiles)
      .catch((error: unknown) => {
        const message = error instanceof UsuariosApiError ? error.message : t('usuarios.errorLoad')
        setLoadError(message)
      })
  }

  useEffect(() => {
    cargarPerfiles()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function onCampoChange(campo: keyof CrearPerfilRequest, valor: string) {
    setForm((actual) => ({ ...actual, [campo]: valor }))
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setEnviando(true)
    setSubmitError('')
    setSubmitStatus('')
    try {
      const creado = await crearPerfil(form)
      setPerfiles((actual) => (actual ? [...actual, creado] : [creado]))
      setForm(FORM_INICIAL)
      setSubmitStatus(t('usuarios.form.success'))
    } catch (error) {
      const message = error instanceof UsuariosApiError ? error.message : t('usuarios.form.error')
      setSubmitError(message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <DashboardLayout breadcrumb={t('usuarios.title')}>
      <div className="usuarios-page">
        <h1>{t('usuarios.title')}</h1>
        <p className="usuarios-page__subtitle">{t('usuarios.subtitle')}</p>

        <section className="usuarios-page__section">
          {loadError ? (
            <>
              <p className="usuarios-page__alert" role="alert">{loadError}</p>
              <button type="button" className="usuarios-page__retry" onClick={cargarPerfiles}>
                {t('usuarios.retry')}
              </button>
            </>
          ) : perfiles === null ? (
            <p>{t('usuarios.loading')}</p>
          ) : perfiles.length === 0 ? (
            <p>{t('usuarios.empty')}</p>
          ) : (
            <div className="usuarios-page__table-wrap">
              <table className="usuarios-page__table">
                <thead>
                  <tr>
                    <th scope="col">{t('usuarios.table.nombres')}</th>
                    <th scope="col">{t('usuarios.table.apellidos')}</th>
                    <th scope="col">{t('usuarios.table.emailInstitucional')}</th>
                    <th scope="col">{t('usuarios.table.activo')}</th>
                  </tr>
                </thead>
                <tbody>
                  {perfiles.map((perfil) => (
                    <tr key={perfil.id}>
                      <td>{perfil.nombres}</td>
                      <td>{perfil.apellidos}</td>
                      <td>{perfil.emailInstitucional}</td>
                      <td>{perfil.activo ? t('usuarios.table.si') : t('usuarios.table.no')}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="usuarios-page__section">
          <h2>{t('usuarios.form.title')}</h2>
          {submitError && <p className="usuarios-page__alert" role="alert">{submitError}</p>}
          <form className="usuarios-page__form" onSubmit={onSubmit} noValidate>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-identificacion">{t('usuarios.form.identificacion')}</label>
              <input
                id="usuario-identificacion"
                value={form.identificacion}
                onChange={(event) => onCampoChange('identificacion', event.target.value)}
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-nombres">{t('usuarios.form.nombres')}</label>
              <input
                id="usuario-nombres"
                required
                value={form.nombres}
                onChange={(event) => onCampoChange('nombres', event.target.value)}
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-apellidos">{t('usuarios.form.apellidos')}</label>
              <input
                id="usuario-apellidos"
                required
                value={form.apellidos}
                onChange={(event) => onCampoChange('apellidos', event.target.value)}
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-email-institucional">{t('usuarios.form.emailInstitucional')}</label>
              <input
                id="usuario-email-institucional"
                type="email"
                required
                value={form.emailInstitucional}
                onChange={(event) => onCampoChange('emailInstitucional', event.target.value)}
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-email-personal">{t('usuarios.form.emailPersonal')}</label>
              <input
                id="usuario-email-personal"
                type="email"
                value={form.emailPersonal}
                onChange={(event) => onCampoChange('emailPersonal', event.target.value)}
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-telefono">{t('usuarios.form.telefono')}</label>
              <input
                id="usuario-telefono"
                type="tel"
                value={form.telefono}
                onChange={(event) => onCampoChange('telefono', event.target.value)}
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-direccion">{t('usuarios.form.direccion')}</label>
              <input
                id="usuario-direccion"
                value={form.direccion}
                onChange={(event) => onCampoChange('direccion', event.target.value)}
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-fecha-nacimiento">{t('usuarios.form.fechaNacimiento')}</label>
              <input
                id="usuario-fecha-nacimiento"
                type="date"
                value={form.fechaNacimiento}
                onChange={(event) => onCampoChange('fechaNacimiento', event.target.value)}
              />
            </div>
            <div className="usuarios-page__form-actions">
              <button type="submit" className="usuarios-page__submit" disabled={enviando}>
                {enviando ? t('usuarios.form.submitting') : t('usuarios.form.submit')}
              </button>
              <p className="usuarios-page__status" role="status" aria-live="polite">
                {submitStatus}
              </p>
            </div>
          </form>
        </section>
      </div>
    </DashboardLayout>
  )
}
