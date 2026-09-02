import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { DashboardLayout } from '../components/DashboardLayout'
import '../i18n'
import {
  actualizarPerfil,
  cambiarEstadoPerfil,
  crearPerfil,
  listarPerfiles,
  UsuariosApiError,
  type CrearPerfilRequest,
  type Perfil,
} from '../services/usuariosApi'
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
  const [busqueda, setBusqueda] = useState('')
  const [estado, setEstado] = useState<'TODOS' | 'ACTIVOS' | 'INACTIVOS'>(
    'TODOS',
  )
  const [seleccionado, setSeleccionado] = useState<Perfil | null>(null)

  function cargarPerfiles() {
    setPerfiles(null)
    setLoadError('')
    listarPerfiles()
      .then(setPerfiles)
      .catch((error: unknown) => {
        const message =
          error instanceof UsuariosApiError
            ? error.message
            : t('usuarios.errorLoad')
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
      const guardado = seleccionado
        ? await actualizarPerfil(seleccionado.id, {
            ...form,
            fotoUrl: seleccionado.fotoUrl,
          })
        : await crearPerfil(form)
      setPerfiles((actual) =>
        actual
          ? seleccionado
            ? actual.map((item) => (item.id === guardado.id ? guardado : item))
            : [...actual, guardado]
          : [guardado],
      )
      setForm(FORM_INICIAL)
      setSeleccionado(null)
      setSubmitStatus(
        seleccionado
          ? 'Perfil actualizado correctamente.'
          : t('usuarios.form.success'),
      )
    } catch (error) {
      const message =
        error instanceof UsuariosApiError
          ? error.message
          : t('usuarios.form.error')
      setSubmitError(message)
    } finally {
      setEnviando(false)
    }
  }
  function editar(perfil: Perfil) {
    setSeleccionado(perfil)
    setForm({
      identificacion: perfil.identificacion,
      nombres: perfil.nombres,
      apellidos: perfil.apellidos,
      emailInstitucional: perfil.emailInstitucional,
      emailPersonal: perfil.emailPersonal ?? '',
      telefono: perfil.telefono ?? '',
      direccion: perfil.direccion ?? '',
      fechaNacimiento: perfil.fechaNacimiento ?? '',
    })
  }
  async function cambiarEstado(perfil: Perfil) {
    try {
      const actualizado = await cambiarEstadoPerfil(perfil.id, !perfil.activo)
      setPerfiles(
        (actual) =>
          actual?.map((item) =>
            item.id === actualizado.id ? actualizado : item,
          ) ?? [],
      )
      if (seleccionado?.id === actualizado.id) setSeleccionado(actualizado)
    } catch (error) {
      setLoadError(
        error instanceof Error
          ? error.message
          : 'No se pudo cambiar el estado.',
      )
    }
  }

  return (
    <DashboardLayout breadcrumb={t('usuarios.title')}>
      <div className="usuarios-page">
        <h1>{t('usuarios.title')}</h1>
        <p className="usuarios-page__subtitle">{t('usuarios.subtitle')}</p>

        <section className="usuarios-page__section">
          <div className="usuarios-page__search">
            <label htmlFor="buscar-usuario">Buscar usuarios</label>
            <input
              id="buscar-usuario"
              value={busqueda}
              onChange={(event) => setBusqueda(event.target.value)}
              placeholder="Nombre, apellido o correo"
            />
            <label htmlFor="estado-usuario">Estado</label>
            <select
              id="estado-usuario"
              value={estado}
              onChange={(event) =>
                setEstado(event.target.value as typeof estado)
              }
            >
              <option value="TODOS">Todos</option>
              <option value="ACTIVOS">Activos</option>
              <option value="INACTIVOS">Inactivos</option>
            </select>
          </div>
          {loadError ? (
            <>
              <p className="usuarios-page__alert" role="alert">
                {loadError}
              </p>
              <button
                type="button"
                className="usuarios-page__retry"
                onClick={cargarPerfiles}
              >
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
                    <th scope="col">
                      {t('usuarios.table.emailInstitucional')}
                    </th>
                    <th scope="col">{t('usuarios.table.activo')}</th>
                    <th scope="col">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {perfiles
                    .filter((perfil) => {
                      const coincideEstado =
                        estado === 'TODOS' ||
                        (estado === 'ACTIVOS' ? perfil.activo : !perfil.activo)
                      return (
                        coincideEstado &&
                        `${perfil.nombres} ${perfil.apellidos} ${perfil.emailInstitucional} ${perfil.identificacion}`
                          .toLowerCase()
                          .includes(busqueda.toLowerCase())
                      )
                    })
                    .map((perfil) => (
                      <tr key={perfil.id}>
                        <td>{perfil.nombres}</td>
                        <td>{perfil.apellidos}</td>
                        <td>{perfil.emailInstitucional}</td>
                        <td>
                          {perfil.activo
                            ? t('usuarios.table.si')
                            : t('usuarios.table.no')}
                        </td>
                        <td>
                          <button type="button" onClick={() => editar(perfil)}>
                            Ver / editar
                          </button>{' '}
                          <button
                            type="button"
                            onClick={() => void cambiarEstado(perfil)}
                          >
                            {perfil.activo ? 'Desactivar' : 'Activar'}
                          </button>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="usuarios-page__section">
          <h2>
            {seleccionado
              ? `Editar perfil de ${seleccionado.nombres} ${seleccionado.apellidos}`
              : t('usuarios.form.title')}
          </h2>
          {submitError && (
            <p className="usuarios-page__alert" role="alert">
              {submitError}
            </p>
          )}
          <form className="usuarios-page__form" onSubmit={onSubmit} noValidate>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-identificacion">
                {t('usuarios.form.identificacion')}
              </label>
              <input
                id="usuario-identificacion"
                value={form.identificacion}
                onChange={(event) =>
                  onCampoChange('identificacion', event.target.value)
                }
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-nombres">
                {t('usuarios.form.nombres')}
              </label>
              <input
                id="usuario-nombres"
                required
                value={form.nombres}
                onChange={(event) =>
                  onCampoChange('nombres', event.target.value)
                }
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-apellidos">
                {t('usuarios.form.apellidos')}
              </label>
              <input
                id="usuario-apellidos"
                required
                value={form.apellidos}
                onChange={(event) =>
                  onCampoChange('apellidos', event.target.value)
                }
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-email-institucional">
                {t('usuarios.form.emailInstitucional')}
              </label>
              <input
                id="usuario-email-institucional"
                type="email"
                required
                value={form.emailInstitucional}
                onChange={(event) =>
                  onCampoChange('emailInstitucional', event.target.value)
                }
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-email-personal">
                {t('usuarios.form.emailPersonal')}
              </label>
              <input
                id="usuario-email-personal"
                type="email"
                value={form.emailPersonal}
                onChange={(event) =>
                  onCampoChange('emailPersonal', event.target.value)
                }
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-telefono">
                {t('usuarios.form.telefono')}
              </label>
              <input
                id="usuario-telefono"
                type="tel"
                value={form.telefono}
                onChange={(event) =>
                  onCampoChange('telefono', event.target.value)
                }
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-direccion">
                {t('usuarios.form.direccion')}
              </label>
              <input
                id="usuario-direccion"
                value={form.direccion}
                onChange={(event) =>
                  onCampoChange('direccion', event.target.value)
                }
              />
            </div>
            <div className="usuarios-page__field">
              <label htmlFor="usuario-fecha-nacimiento">
                {t('usuarios.form.fechaNacimiento')}
              </label>
              <input
                id="usuario-fecha-nacimiento"
                type="date"
                value={form.fechaNacimiento}
                onChange={(event) =>
                  onCampoChange('fechaNacimiento', event.target.value)
                }
              />
            </div>
            <div className="usuarios-page__form-actions">
              <button
                type="submit"
                className="usuarios-page__submit"
                disabled={enviando}
              >
                {enviando
                  ? t('usuarios.form.submitting')
                  : seleccionado
                    ? 'Guardar cambios'
                    : t('usuarios.form.submit')}
              </button>
              {seleccionado && (
                <button
                  type="button"
                  onClick={() => {
                    setSeleccionado(null)
                    setForm(FORM_INICIAL)
                  }}
                >
                  Cancelar edición
                </button>
              )}
              <p
                className="usuarios-page__status"
                role="status"
                aria-live="polite"
              >
                {submitStatus}
              </p>
            </div>
          </form>
        </section>
      </div>
    </DashboardLayout>
  )
}
