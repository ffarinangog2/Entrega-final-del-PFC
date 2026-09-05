import { useCallback, useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { DashboardLayout } from '../components/DashboardLayout'
import { obtenerCarreras, obtenerPeriodos, obtenerPisos, type Carrera, type PeriodoLectivo, type Piso } from '../services/academicoApi'
import { listarUsuariosInstitucionales, type RolInstitucional, type UsuarioInstitucional } from '../services/authApi'
import { actualizarUsuarioInstitucionalCompleto, crearUsuarioInstitucionalCompleto, listarPerfiles, obtenerAsociacionRol, obtenerContextosAcademicos, type Perfil } from '../services/usuariosApi'
import './UsuariosPage.css'

const ROLES: RolInstitucional[] = ['ADMINISTRADOR', 'ADMINISTRADOR_PISO', 'COORDINADOR', 'DOCENTE', 'ESTUDIANTE']
type Formulario = { perfilId: string; authId: string; identificacion: string; nombres: string; apellidos: string; email: string; username: string; password: string; rol: RolInstitucional; activo: boolean; pisoId: string; carreraId: string; periodoId: string; nivel: string }
const inicial: Formulario = { perfilId: '', authId: '', identificacion: '', nombres: '', apellidos: '', email: '', username: '', password: '', rol: 'ESTUDIANTE', activo: true, pisoId: '', carreraId: '', periodoId: '', nivel: '1' }

export function UsuariosPage() {
  const [perfiles, setPerfiles] = useState<Perfil[]>([])
  const [cuentas, setCuentas] = useState<UsuarioInstitucional[]>([])
  const [pisos, setPisos] = useState<Piso[]>([])
  const [carreras, setCarreras] = useState<Carrera[]>([])
  const [periodos, setPeriodos] = useState<PeriodoLectivo[]>([])
  const [form, setForm] = useState<Formulario>(inicial)
  const [busqueda, setBusqueda] = useState('')
  const [filtroRol, setFiltroRol] = useState('TODOS')
  const [filtroEstado, setFiltroEstado] = useState('TODOS')
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')

  const cargar = useCallback(async () => {
    setCargando(true); setError('')
    try {
      const [p, u, pisosData, carrerasData, periodosData] = await Promise.all([listarPerfiles(), listarUsuariosInstitucionales(), obtenerPisos(), obtenerCarreras(), obtenerPeriodos()])
      setPerfiles(p ?? []); setCuentas(u ?? []); setPisos(pisosData ?? []); setCarreras(carrerasData ?? []); setPeriodos(periodosData ?? [])
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'No fue posible cargar los usuarios institucionales.') }
    finally { setCargando(false) }
  }, [])
  useEffect(() => void cargar(), [cargar])

  const perfilPorId = useMemo(() => new Map(perfiles.map((item) => [item.id, item])), [perfiles])
  const filas = useMemo(() => cuentas.filter((cuenta) => {
    const perfil = perfilPorId.get(cuenta.perfilId)
    const texto = `${perfil?.nombres ?? ''} ${perfil?.apellidos ?? ''} ${cuenta.username} ${cuenta.email}`.toLowerCase()
    return texto.includes(busqueda.toLowerCase()) && (filtroRol === 'TODOS' || cuenta.rol === filtroRol) && (filtroEstado === 'TODOS' || cuenta.activo === (filtroEstado === 'ACTIVO'))
  }), [busqueda, cuentas, filtroEstado, filtroRol, perfilPorId])

  async function editar(cuenta: UsuarioInstitucional) {
    const perfil = perfilPorId.get(cuenta.perfilId)
    if (!perfil) return
    setMensaje(''); setError('')
    try {
      const asociacion = await obtenerAsociacionRol(perfil.id)
      const contexto = cuenta.rol === 'ESTUDIANTE' ? (await obtenerContextosAcademicos(perfil.id)).find(item => item.activo) : undefined
      setForm({ perfilId: perfil.id, authId: cuenta.id, identificacion: perfil.identificacion, nombres: perfil.nombres, apellidos: perfil.apellidos, email: cuenta.email, username: cuenta.username, password: '', rol: cuenta.rol, activo: cuenta.activo, pisoId: asociacion.pisoId ?? '', carreraId: contexto?.carreraId ?? asociacion.carreraId ?? '', periodoId: contexto?.periodoId ?? '', nivel: String(contexto?.nivel ?? 1) })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo consultar la asociación institucional.')
    }
  }

  async function guardar(event: FormEvent) {
    event.preventDefault(); setGuardando(true); setError(''); setMensaje('')
    try {
      if (form.authId) {
        await actualizarUsuarioInstitucionalCompleto(form.perfilId, { authId: form.authId, identificacion: form.identificacion, nombres: form.nombres, apellidos: form.apellidos, emailInstitucional: form.email, emailPersonal: '', telefono: '', direccion: '', fechaNacimiento: '', fotoUrl: null, username: form.username, email: form.email, rol: form.rol, activo: form.activo, pisoId: form.pisoId || null, carreraId: ['COORDINADOR','ESTUDIANTE'].includes(form.rol) ? form.carreraId || null : null, periodoId:form.rol==='ESTUDIANTE'?form.periodoId:null,nivel:form.rol==='ESTUDIANTE'?Number(form.nivel):null })
      } else {
        await crearUsuarioInstitucionalCompleto({ identificacion: form.identificacion, nombres: form.nombres, apellidos: form.apellidos, emailInstitucional: form.email, emailPersonal: '', telefono: '', direccion: '', fechaNacimiento: '', username: form.username, email: form.email, passwordInicial: form.password, rol: form.rol, pisoId: form.pisoId || null, carreraId: ['COORDINADOR','ESTUDIANTE'].includes(form.rol) ? form.carreraId || null : null, periodoId:form.rol==='ESTUDIANTE'?form.periodoId:null,nivel:form.rol==='ESTUDIANTE'?Number(form.nivel):null })
      }
      setForm(inicial); setMensaje(form.authId ? 'Usuario actualizado correctamente.' : 'Usuario institucional creado correctamente.'); await cargar()
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo guardar el usuario institucional.') }
    finally { setGuardando(false) }
  }

  async function alternar(cuenta: UsuarioInstitucional) {
    try {
      const nuevoEstado = !cuenta.activo
      const perfil = perfilPorId.get(cuenta.perfilId)
      if (!perfil) throw new Error('No se encontró el perfil institucional de la cuenta.')
      const asociacion = await obtenerAsociacionRol(cuenta.perfilId)
      const contexto = cuenta.rol === 'ESTUDIANTE'
        ? (await obtenerContextosAcademicos(cuenta.perfilId)).find((item) => item.activo)
        : undefined
      await actualizarUsuarioInstitucionalCompleto(cuenta.perfilId, { authId: cuenta.id, identificacion: perfil.identificacion, nombres: perfil.nombres, apellidos: perfil.apellidos, emailInstitucional: cuenta.email, emailPersonal: perfil.emailPersonal ?? '', telefono: perfil.telefono ?? '', direccion: perfil.direccion ?? '', fechaNacimiento: perfil.fechaNacimiento ?? '', fotoUrl: perfil.fotoUrl, username: cuenta.username, email: cuenta.email, rol: cuenta.rol, activo: nuevoEstado, pisoId: asociacion.pisoId, carreraId: contexto?.carreraId ?? asociacion.carreraId, periodoId: contexto?.periodoId ?? null, nivel: contexto?.nivel ?? null })
      await cargar()
    }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo cambiar el estado.') }
  }

  return <DashboardLayout breadcrumb="Usuarios"><div className="usuarios-page">
    <h1>Usuarios institucionales</h1><p className="usuarios-page__subtitle">Credenciales, rol y asociación institucional.</p>
    {error && <p role="alert" className="usuarios-page__alert">{error}</p>}{mensaje && <p role="status">{mensaje}</p>}
    <section className="usuarios-page__section"><div className="usuarios-page__search">
      <label>Buscar<input value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Nombre, usuario o correo" /></label>
      <label>Rol<select value={filtroRol} onChange={(e) => setFiltroRol(e.target.value)}><option>TODOS</option>{ROLES.map((rol) => <option key={rol}>{rol}</option>)}</select></label>
      <label>Estado<select value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value)}><option>TODOS</option><option>ACTIVO</option><option>INACTIVO</option></select></label>
    </div>{cargando ? <p role="status">Cargando usuarios...</p> : filas.length === 0 ? <p>No existen usuarios para los filtros seleccionados.</p> : <div className="usuarios-page__table-wrap"><table className="usuarios-page__table"><thead><tr><th>Nombre</th><th>Usuario</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{filas.map((cuenta) => { const perfil = perfilPorId.get(cuenta.perfilId); return <tr key={cuenta.id}><td>{perfil ? `${perfil.nombres} ${perfil.apellidos}` : 'Perfil institucional'}</td><td>{cuenta.username}</td><td>{cuenta.email}</td><td>{cuenta.rol}</td><td>{cuenta.activo ? 'Activo' : 'Inactivo'}</td><td><button onClick={() => editar(cuenta)}>Editar</button> <button onClick={() => void alternar(cuenta)}>{cuenta.activo ? 'Desactivar' : 'Activar'}</button></td></tr> })}</tbody></table></div>}</section>
    <section className="usuarios-page__section"><h2>{form.authId ? 'Editar usuario' : 'Crear usuario institucional'}</h2><form className="usuarios-page__form" onSubmit={guardar}>
      <Field label="Identificación"><input required pattern="[0-9]{10}" value={form.identificacion} onChange={(e) => setForm({ ...form, identificacion: e.target.value })} /></Field>
      <Field label="Nombres"><input required value={form.nombres} onChange={(e) => setForm({ ...form, nombres: e.target.value })} /></Field>
      <Field label="Apellidos"><input required value={form.apellidos} onChange={(e) => setForm({ ...form, apellidos: e.target.value })} /></Field>
      <Field label="Correo institucional"><input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
      <Field label="Nombre de usuario"><input required value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></Field>
      {!form.authId && <Field label="Contraseña inicial"><input required type="password" autoComplete="new-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></Field>}
      <Field label="Rol"><select value={form.rol} onChange={(e) => setForm({ ...form, rol: e.target.value as RolInstitucional, pisoId: '', carreraId: '' })}>{ROLES.map((rol) => <option key={rol}>{rol}</option>)}</select></Field>
      {form.rol === 'ADMINISTRADOR_PISO' && <Field label="Piso"><select required value={form.pisoId} onChange={(e) => setForm({ ...form, pisoId: e.target.value })}><option value="">Seleccione un piso</option>{pisos.map((piso) => <option key={piso.id} value={piso.id}>Piso {piso.numero}</option>)}</select></Field>}
      {form.rol === 'COORDINADOR' && <Field label="Carrera"><select required value={form.carreraId} onChange={(e) => setForm({ ...form, carreraId: e.target.value })}><option value="">Seleccione una carrera</option>{carreras.map((carrera) => <option key={carrera.id} value={carrera.id}>{carrera.nombre}</option>)}</select></Field>}
      {form.rol === 'ESTUDIANTE' && <><Field label="Carrera"><select required value={form.carreraId} onChange={(e) => setForm({ ...form, carreraId: e.target.value })}><option value="">Seleccione una carrera</option>{carreras.map((carrera) => <option key={carrera.id} value={carrera.id}>{carrera.nombre}</option>)}</select></Field><Field label="Período académico"><select required value={form.periodoId} onChange={(e) => setForm({ ...form, periodoId: e.target.value })}><option value="">Seleccione un período</option>{periodos.map((periodo) => <option key={periodo.id} value={periodo.id}>{periodo.nombre}</option>)}</select></Field><Field label="Nivel"><select value={form.nivel} onChange={(e) => setForm({ ...form, nivel: e.target.value })}>{Array.from({length:10},(_,i)=><option key={i+1}>{i+1}</option>)}</select></Field></>}
      {form.authId && <label className="usuarios-page__field"><span>Estado</span><input type="checkbox" checked={form.activo} onChange={(e) => setForm({ ...form, activo: e.target.checked })} /> Cuenta activa</label>}
      <div className="usuarios-page__form-actions"><button className="usuarios-page__submit" disabled={guardando}>{guardando ? 'Guardando...' : form.authId ? 'Guardar cambios' : 'Crear usuario'}</button>{form.authId && <button type="button" onClick={() => setForm(inicial)}>Cancelar</button>}</div>
    </form></section>
  </div></DashboardLayout>
}

function Field({ label, children }: { label: string; children: ReactNode }) { return <label className="usuarios-page__field"><span>{label}</span>{children}</label> }
