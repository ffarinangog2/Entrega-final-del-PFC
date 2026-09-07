import { useCallback, useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { DashboardLayout } from '../components/DashboardLayout'
import { useAcademicPeriod } from '../academicPeriodContext'
import {
  obtenerCarreras,
  obtenerPeriodos,
  obtenerPisos,
  type Carrera,
  type PeriodoLectivo,
  type Piso,
} from '../services/academicoApi'
import {
  listarUsuariosInstitucionales,
  type RolInstitucional,
  type UsuarioInstitucional,
} from '../services/authApi'
import {
  actualizarUsuarioInstitucionalCompleto,
  crearUsuarioInstitucionalCompleto,
  listarContextosEstudiantesMasivos,
  listarPerfiles,
  obtenerAsociacionRol,
  obtenerContextosAcademicos,
  type ContextoEstudianteResumen,
  type Perfil,
} from '../services/usuariosApi'
import './UsuariosPage.css'

const ROLES: RolInstitucional[] = [
  'ADMINISTRADOR',
  'ADMINISTRADOR_PISO',
  'COORDINADOR',
  'DOCENTE',
  'ESTUDIANTE',
]

type Formulario = {
  perfilId: string
  authId: string
  identificacion: string
  nombres: string
  apellidos: string
  email: string
  username: string
  password: string
  rol: RolInstitucional
  activo: boolean
  pisoId: string
  carreraId: string
  periodoId: string
  nivel: string
}

const inicial: Formulario = {
  perfilId: '',
  authId: '',
  identificacion: '',
  nombres: '',
  apellidos: '',
  email: '',
  username: '',
  password: '',
  rol: 'ESTUDIANTE',
  activo: true,
  pisoId: '',
  carreraId: '',
  periodoId: '',
  nivel: '1',
}

export function UsuariosPage() {
  const { periodoSeleccionado } = useAcademicPeriod()
  const [perfiles, setPerfiles] = useState<Perfil[]>([])
  const [cuentas, setCuentas] = useState<UsuarioInstitucional[]>([])
  const [pisos, setPisos] = useState<Piso[]>([])
  const [carreras, setCarreras] = useState<Carrera[]>([])
  const [periodos, setPeriodos] = useState<PeriodoLectivo[]>([])
  const [contextos, setContextos] = useState<ContextoEstudianteResumen[]>([])
  const [form, setForm] = useState<Formulario>(inicial)
  const [busqueda, setBusqueda] = useState('')
  const [filtroRol, setFiltroRol] = useState('TODOS')
  const [filtroCarrera, setFiltroCarrera] = useState('TODAS')
  const [filtroNivel, setFiltroNivel] = useState('TODOS')
  const [filtroEstado, setFiltroEstado] = useState('TODOS')
  const [pagina, setPagina] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')

  const cargar = useCallback(async () => {
    setCargando(true)
    setError('')
    try {
      const [p, u, pisosData, carrerasData, periodosData, contextosData] =
        await Promise.all([
          listarPerfiles(),
          listarUsuariosInstitucionales(),
          obtenerPisos(),
          obtenerCarreras(),
          obtenerPeriodos(),
          periodoSeleccionado?.id
            ? listarContextosEstudiantesMasivos(periodoSeleccionado.id)
            : Promise.resolve([]),
        ])
      setPerfiles(p ?? [])
      setCuentas(u ?? [])
      setPisos(pisosData ?? [])
      setCarreras(carrerasData ?? [])
      setPeriodos(periodosData ?? [])
      setContextos(contextosData ?? [])
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No fue posible cargar los usuarios institucionales.',
      )
    } finally {
      setCargando(false)
    }
  }, [periodoSeleccionado?.id])

  useEffect(() => {
    void cargar()
  }, [cargar])

  useEffect(() => {
    setPagina(1)
  }, [busqueda, filtroRol, filtroCarrera, filtroNivel, filtroEstado, pageSize])

  const perfilPorId = useMemo(
    () => new Map(perfiles.map((item) => [item.id, item])),
    [perfiles],
  )
  const carreraPorId = useMemo(
    () => new Map(carreras.map((item) => [item.id, item])),
    [carreras],
  )
  const contextoPorPerfilId = useMemo(
    () => new Map(contextos.map((item) => [item.perfilId, item])),
    [contextos],
  )

  const filasFiltradas = useMemo(() => {
    return cuentas.filter((cuenta) => {
      const perfil = perfilPorId.get(cuenta.perfilId)
      const texto =
        `${perfil?.nombres ?? ''} ${perfil?.apellidos ?? ''} ${cuenta.username} ${cuenta.email}`.toLowerCase()
      const coincideBusqueda = texto.includes(busqueda.toLowerCase())
      const coincideRol = filtroRol === 'TODOS' || cuenta.rol === filtroRol
      const coincideEstado =
        filtroEstado === 'TODOS' || cuenta.activo === (filtroEstado === 'ACTIVO')

      if (!coincideBusqueda || !coincideRol || !coincideEstado) {
        return false
      }

      if (filtroRol === 'ESTUDIANTE') {
        const contexto = contextoPorPerfilId.get(cuenta.perfilId)
        if (filtroCarrera !== 'TODAS' && contexto?.carreraId !== filtroCarrera) {
          return false
        }
        if (
          filtroNivel !== 'TODOS' &&
          String(contexto?.nivel ?? '') !== filtroNivel
        ) {
          return false
        }
      }

      return true
    })
  }, [
    busqueda,
    cuentas,
    filtroCarrera,
    filtroEstado,
    filtroNivel,
    filtroRol,
    perfilPorId,
    contextoPorPerfilId,
  ])

  const totalElementos = filasFiltradas.length
  const totalPaginas = Math.max(1, Math.ceil(totalElementos / pageSize))
  const paginaActual = Math.min(Math.max(1, pagina), totalPaginas)
  const inicio = totalElementos === 0 ? 0 : (paginaActual - 1) * pageSize + 1
  const fin = Math.min(paginaActual * pageSize, totalElementos)

  const filasPaginadas = useMemo(() => {
    const start = (paginaActual - 1) * pageSize
    return filasFiltradas.slice(start, start + pageSize)
  }, [filasFiltradas, paginaActual, pageSize])

  async function editar(cuenta: UsuarioInstitucional) {
    const perfil = perfilPorId.get(cuenta.perfilId)
    if (!perfil) return
    setMensaje('')
    setError('')
    try {
      const asociacion = await obtenerAsociacionRol(perfil.id)
      const contexto =
        cuenta.rol === 'ESTUDIANTE'
          ? (await obtenerContextosAcademicos(perfil.id)).find((item) => item.activo)
          : undefined
      setForm({
        perfilId: perfil.id,
        authId: cuenta.id,
        identificacion: perfil.identificacion,
        nombres: perfil.nombres,
        apellidos: perfil.apellidos,
        email: cuenta.email,
        username: cuenta.username,
        password: '',
        rol: cuenta.rol,
        activo: cuenta.activo,
        pisoId: asociacion.pisoId ?? '',
        carreraId: contexto?.carreraId ?? asociacion.carreraId ?? '',
        periodoId: contexto?.periodoId ?? '',
        nivel: String(contexto?.nivel ?? 1),
      })
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No se pudo consultar la asociación institucional.',
      )
    }
  }

  async function guardar(event: FormEvent) {
    event.preventDefault()
    setGuardando(true)
    setError('')
    setMensaje('')
    try {
      if (form.authId) {
        await actualizarUsuarioInstitucionalCompleto(form.perfilId, {
          authId: form.authId,
          identificacion: form.identificacion,
          nombres: form.nombres,
          apellidos: form.apellidos,
          emailInstitucional: form.email,
          emailPersonal: '',
          telefono: '',
          direccion: '',
          fechaNacimiento: '',
          fotoUrl: null,
          username: form.username,
          email: form.email,
          rol: form.rol,
          activo: form.activo,
          pisoId: form.pisoId || null,
          carreraId: ['COORDINADOR', 'ESTUDIANTE'].includes(form.rol)
            ? form.carreraId || null
            : null,
          periodoId: form.rol === 'ESTUDIANTE' ? form.periodoId : null,
          nivel: form.rol === 'ESTUDIANTE' ? Number(form.nivel) : null,
        })
      } else {
        await crearUsuarioInstitucionalCompleto({
          identificacion: form.identificacion,
          nombres: form.nombres,
          apellidos: form.apellidos,
          emailInstitucional: form.email,
          emailPersonal: '',
          telefono: '',
          direccion: '',
          fechaNacimiento: '',
          username: form.username,
          email: form.email,
          passwordInicial: form.password,
          rol: form.rol,
          pisoId: form.pisoId || null,
          carreraId: ['COORDINADOR', 'ESTUDIANTE'].includes(form.rol)
            ? form.carreraId || null
            : null,
          periodoId: form.rol === 'ESTUDIANTE' ? form.periodoId : null,
          nivel: form.rol === 'ESTUDIANTE' ? Number(form.nivel) : null,
        })
      }
      setForm(inicial)
      setMensaje(
        form.authId
          ? 'Usuario actualizado correctamente.'
          : 'Usuario institucional creado correctamente.',
      )
      await cargar()
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No se pudo guardar el usuario institucional.',
      )
    } finally {
      setGuardando(false)
    }
  }

  async function alternar(cuenta: UsuarioInstitucional) {
    try {
      const nuevoEstado = !cuenta.activo
      const perfil = perfilPorId.get(cuenta.perfilId)
      if (!perfil)
        throw new Error('No se encontró el perfil institucional de la cuenta.')
      const asociacion = await obtenerAsociacionRol(cuenta.perfilId)
      const contexto =
        cuenta.rol === 'ESTUDIANTE'
          ? (await obtenerContextosAcademicos(cuenta.perfilId)).find(
              (item) => item.activo,
            )
          : undefined
      await actualizarUsuarioInstitucionalCompleto(cuenta.perfilId, {
        authId: cuenta.id,
        identificacion: perfil.identificacion,
        nombres: perfil.nombres,
        apellidos: perfil.apellidos,
        emailInstitucional: cuenta.email,
        emailPersonal: perfil.emailPersonal ?? '',
        telefono: perfil.telefono ?? '',
        direccion: perfil.direccion ?? '',
        fechaNacimiento: perfil.fechaNacimiento ?? '',
        fotoUrl: perfil.fotoUrl,
        username: cuenta.username,
        email: cuenta.email,
        rol: cuenta.rol,
        activo: nuevoEstado,
        pisoId: asociacion.pisoId,
        carreraId: contexto?.carreraId ?? asociacion.carreraId,
        periodoId: contexto?.periodoId ?? null,
        nivel: contexto?.nivel ?? null,
      })
      await cargar()
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No se pudo cambiar el estado.',
      )
    }
  }

  return (
    <DashboardLayout breadcrumb="Usuarios">
      <div className="usuarios-page">
        <h1>Usuarios institucionales</h1>
        <p className="usuarios-page__subtitle">
          Credenciales, rol y asociación institucional.
        </p>
        {error && (
          <p role="alert" className="usuarios-page__alert">
            {error}
          </p>
        )}
        {mensaje && <p role="status">{mensaje}</p>}
        <section className="usuarios-page__section">
          <div className="usuarios-page__search">
            <label>
              Buscar
              <input
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
                placeholder="Nombre, usuario o correo"
              />
            </label>
            <label>
              Rol
              <select
                value={filtroRol}
                onChange={(e) => {
                  const nuevoRol = e.target.value
                  setFiltroRol(nuevoRol)
                  if (nuevoRol !== 'ESTUDIANTE') {
                    setFiltroCarrera('TODAS')
                    setFiltroNivel('TODOS')
                  }
                }}
              >
                <option>TODOS</option>
                {ROLES.map((rol) => (
                  <option key={rol}>{rol}</option>
                ))}
              </select>
            </label>
            <label>
              Carrera
              <select
                aria-label="Carrera"
                disabled={filtroRol !== 'ESTUDIANTE'}
                value={filtroCarrera}
                onChange={(e) => setFiltroCarrera(e.target.value)}
              >
                <option value="TODAS">TODAS</option>
                {carreras.map((carrera) => (
                  <option key={carrera.id} value={carrera.id}>
                    {carrera.nombre}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Nivel
              <select
                aria-label="Nivel"
                disabled={filtroRol !== 'ESTUDIANTE'}
                value={filtroNivel}
                onChange={(e) => setFiltroNivel(e.target.value)}
              >
                <option value="TODOS">TODOS</option>
                {Array.from({ length: 10 }, (_, i) => (
                  <option key={i + 1} value={String(i + 1)}>
                    {i + 1}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Estado
              <select
                value={filtroEstado}
                onChange={(e) => setFiltroEstado(e.target.value)}
              >
                <option>TODOS</option>
                <option>ACTIVO</option>
                <option>INACTIVO</option>
              </select>
            </label>
          </div>
          {cargando ? (
            <p role="status">Cargando usuarios...</p>
          ) : filasFiltradas.length === 0 ? (
            <p>No existen usuarios para los filtros seleccionados.</p>
          ) : (
            <>
              <div className="usuarios-page__table-wrap">
                <table className="usuarios-page__table">
                  <thead>
                    <tr>
                      <th>Nombre</th>
                      <th>Usuario</th>
                      <th>Correo</th>
                      <th>Rol</th>
                      <th>Carrera / Nivel</th>
                      <th>Estado</th>
                      <th>Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filasPaginadas.map((cuenta) => {
                      const perfil = perfilPorId.get(cuenta.perfilId)
                      return (
                        <tr key={cuenta.id}>
                          <td>
                            {perfil
                              ? `${perfil.nombres} ${perfil.apellidos}`
                              : 'Perfil institucional'}
                          </td>
                          <td>{cuenta.username}</td>
                          <td>{cuenta.email}</td>
                          <td>{cuenta.rol}</td>
                          <td>
                            {cuenta.rol === 'ESTUDIANTE'
                              ? (() => {
                                  const ctx = contextoPorPerfilId.get(
                                    cuenta.perfilId,
                                  )
                                  if (!ctx || !ctx.carreraId || !ctx.nivel) {
                                    return 'Sin asignar'
                                  }
                                  const nombreCarrera =
                                    carreraPorId.get(ctx.carreraId)?.nombre ??
                                    ctx.carreraId
                                  return `${nombreCarrera} · Nivel ${ctx.nivel}`
                                })()
                              : '—'}
                          </td>
                          <td>{cuenta.activo ? 'Activo' : 'Inactivo'}</td>
                          <td>
                            <button onClick={() => editar(cuenta)}>Editar</button>{' '}
                            <button onClick={() => void alternar(cuenta)}>
                              {cuenta.activo ? 'Desactivar' : 'Activar'}
                            </button>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
              <div className="usuarios-page__pagination">
                <label className="usuarios-page__pagination-size">
                  Filas por página
                  <select
                    aria-label="Filas por página"
                    value={pageSize}
                    onChange={(e) => setPageSize(Number(e.target.value))}
                  >
                    <option value={10}>10</option>
                    <option value={25}>25</option>
                    <option value={50}>50</option>
                  </select>
                </label>
                <span className="usuarios-page__pagination-info">
                  Mostrando {inicio}-{fin} de {totalElementos}
                </span>
                <div className="usuarios-page__pagination-actions">
                  <button
                    type="button"
                    onClick={() => setPagina((p) => Math.max(1, p - 1))}
                    disabled={paginaActual <= 1}
                  >
                    Anterior
                  </button>
                  <span>
                    Página {paginaActual} de {totalPaginas}
                  </span>
                  <button
                    type="button"
                    onClick={() =>
                      setPagina((p) => Math.min(totalPaginas, p + 1))
                    }
                    disabled={paginaActual >= totalPaginas}
                  >
                    Siguiente
                  </button>
                </div>
              </div>
            </>
          )}
        </section>
        <section className="usuarios-page__section">
          <h2>
            {form.authId ? 'Editar usuario' : 'Crear usuario institucional'}
          </h2>
          <form className="usuarios-page__form" onSubmit={guardar}>
            <Field label="Identificación">
              <input
                required
                pattern="[0-9]{10}"
                value={form.identificacion}
                onChange={(e) =>
                  setForm({ ...form, identificacion: e.target.value })
                }
              />
            </Field>
            <Field label="Nombres">
              <input
                required
                value={form.nombres}
                onChange={(e) => setForm({ ...form, nombres: e.target.value })}
              />
            </Field>
            <Field label="Apellidos">
              <input
                required
                value={form.apellidos}
                onChange={(e) =>
                  setForm({ ...form, apellidos: e.target.value })
                }
              />
            </Field>
            <Field label="Correo institucional">
              <input
                required
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </Field>
            <Field label="Nombre de usuario">
              <input
                required
                value={form.username}
                onChange={(e) =>
                  setForm({ ...form, username: e.target.value })
                }
              />
            </Field>
            {!form.authId && (
              <Field label="Contraseña inicial">
                <input
                  required
                  type="password"
                  autoComplete="new-password"
                  value={form.password}
                  onChange={(e) =>
                    setForm({ ...form, password: e.target.value })
                  }
                />
              </Field>
            )}
            <Field label="Rol">
              <select
                value={form.rol}
                onChange={(e) =>
                  setForm({
                    ...form,
                    rol: e.target.value as RolInstitucional,
                    pisoId: '',
                    carreraId: '',
                  })
                }
              >
                {ROLES.map((rol) => (
                  <option key={rol}>{rol}</option>
                ))}
              </select>
            </Field>
            {form.rol === 'ADMINISTRADOR_PISO' && (
              <Field label="Piso">
                <select
                  required
                  value={form.pisoId}
                  onChange={(e) =>
                    setForm({ ...form, pisoId: e.target.value })
                  }
                >
                  <option value="">Seleccione un piso</option>
                  {pisos.map((piso) => (
                    <option key={piso.id} value={piso.id}>
                      Piso {piso.numero}
                    </option>
                  ))}
                </select>
              </Field>
            )}
            {form.rol === 'COORDINADOR' && (
              <Field label="Carrera">
                <select
                  required
                  value={form.carreraId}
                  onChange={(e) =>
                    setForm({ ...form, carreraId: e.target.value })
                  }
                >
                  <option value="">Seleccione una carrera</option>
                  {carreras.map((carrera) => (
                    <option key={carrera.id} value={carrera.id}>
                      {carrera.nombre}
                    </option>
                  ))}
                </select>
              </Field>
            )}
            {form.rol === 'ESTUDIANTE' && (
              <>
                <Field label="Carrera">
                  <select
                    required
                    value={form.carreraId}
                    onChange={(e) =>
                      setForm({ ...form, carreraId: e.target.value })
                    }
                  >
                    <option value="">Seleccione una carrera</option>
                    {carreras.map((carrera) => (
                      <option key={carrera.id} value={carrera.id}>
                        {carrera.nombre}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Período académico">
                  <select
                    required
                    value={form.periodoId}
                    onChange={(e) =>
                      setForm({ ...form, periodoId: e.target.value })
                    }
                  >
                    <option value="">Seleccione un período</option>
                    {periodos.map((periodo) => (
                      <option key={periodo.id} value={periodo.id}>
                        {periodo.nombre}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Nivel">
                  <select
                    value={form.nivel}
                    onChange={(e) =>
                      setForm({ ...form, nivel: e.target.value })
                    }
                  >
                    {Array.from({ length: 10 }, (_, i) => (
                      <option key={i + 1}>{i + 1}</option>
                    ))}
                  </select>
                </Field>
              </>
            )}
            {form.authId && (
              <label className="usuarios-page__field">
                <span>Estado</span>
                <input
                  type="checkbox"
                  checked={form.activo}
                  onChange={(e) =>
                    setForm({ ...form, activo: e.target.checked })
                  }
                />{' '}
                Cuenta activa
              </label>
            )}
            <div className="usuarios-page__form-actions">
              <button className="usuarios-page__submit" disabled={guardando}>
                {guardando
                  ? 'Guardando...'
                  : form.authId
                    ? 'Guardar cambios'
                    : 'Crear usuario'}
              </button>
              {form.authId && (
                <button type="button" onClick={() => setForm(inicial)}>
                  Cancelar
                </button>
              )}
            </div>
          </form>
        </section>
      </div>
    </DashboardLayout>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="usuarios-page__field">
      <span>{label}</span>
      {children}
    </label>
  )
}
