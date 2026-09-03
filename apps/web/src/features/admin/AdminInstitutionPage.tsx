import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import {
  actualizarLaboratorio,
  cambiarEstadoLaboratorio,
  crearEquipo,
  crearLaboratorio,
  obtenerCampus,
  obtenerCarreras,
  obtenerEquipos,
  obtenerLaboratorios,
  obtenerMaterias,
  obtenerPeriodos,
  obtenerPisos,
  obtenerTiposEquipo,
  type Campus,
  type Carrera,
  type Equipo,
  type Laboratorio,
  type Materia,
  type PeriodoLectivo,
  type Piso,
  type TipoEquipo,
} from '../../services/academicoApi'
import {
  actualizarAdministrador,
  listarAdministradores,
  listarPerfiles,
  type AdministradorInstitucional,
  type Perfil,
} from '../../services/usuariosApi'
import './AdminInstitutionPage.css'

type LabForm = { id: string; pisoId: string; codigo: string; nombre: string; capacidad: string; descripcion: string }
const labInicial: LabForm = { id: '', pisoId: '', codigo: '', nombre: '', capacidad: '1', descripcion: '' }

export function AdminInstitutionPage() {
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [pisos, setPisos] = useState<Piso[]>([])
  const [equipos, setEquipos] = useState<Equipo[]>([])
  const [tipos, setTipos] = useState<TipoEquipo[]>([])
  const [campus, setCampus] = useState<Campus[]>([])
  const [carreras, setCarreras] = useState<Carrera[]>([])
  const [materias, setMaterias] = useState<Materia[]>([])
  const [periodos, setPeriodos] = useState<PeriodoLectivo[]>([])
  const [administradores, setAdministradores] = useState<AdministradorInstitucional[]>([])
  const [perfiles, setPerfiles] = useState<Perfil[]>([])
  const [labForm, setLabForm] = useState(labInicial)
  const [equipo, setEquipo] = useState({ laboratorioId: '', tipoEquipoId: '', codigoInventario: '' })
  const [cargando, setCargando] = useState(true)
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')

  const cargar = useCallback(async () => {
    setCargando(true)
    setError('')
    try {
      const data = await Promise.all([
        obtenerLaboratorios(), obtenerPisos(), obtenerEquipos(), obtenerTiposEquipo(),
        obtenerCampus(), obtenerCarreras(), obtenerMaterias(), obtenerPeriodos(),
        listarAdministradores(), listarPerfiles(),
      ])
      setLaboratorios(data[0]); setPisos(data[1]); setEquipos(data[2]); setTipos(data[3])
      setCampus(data[4]); setCarreras(data[5]); setMaterias(data[6]); setPeriodos(data[7])
      setAdministradores(data[8]); setPerfiles(data[9])
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No fue posible cargar la configuración institucional.')
    } finally { setCargando(false) }
  }, [])
  useEffect(() => void cargar(), [cargar])

  const perfilPorId = useMemo(() => new Map(perfiles.map((item) => [item.id, item])), [perfiles])
  const pisoNombre = (id: string | null) => id ? `Piso ${pisos.find((item) => item.id === id)?.numero ?? 'desconocido'}` : 'Sin piso'

  async function guardarLaboratorio(event: FormEvent) {
    event.preventDefault(); setOcupado(true); setError(''); setMensaje('')
    try {
      const body = { ...labForm, capacidad: Number(labForm.capacidad) }
      if (labForm.id) await actualizarLaboratorio(labForm.id, body)
      else await crearLaboratorio(body)
      setLabForm(labInicial); setMensaje('Laboratorio guardado correctamente.'); await cargar()
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo guardar el laboratorio.') }
    finally { setOcupado(false) }
  }

  async function guardarEquipo(event: FormEvent) {
    event.preventDefault(); setOcupado(true); setError(''); setMensaje('')
    try {
      await crearEquipo({ ...equipo, numeroSerie: '', marca: '', modelo: '', procesador: '', memoriaRam: '', almacenamiento: '', direccionIp: '', direccionMac: '', observacion: '' })
      setEquipo({ laboratorioId: '', tipoEquipoId: '', codigoInventario: '' })
      setMensaje('Equipo creado correctamente.'); await cargar()
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo crear el equipo.') }
    finally { setOcupado(false) }
  }

  async function asignarPiso(item: AdministradorInstitucional, pisoId: string) {
    setOcupado(true); setError(''); setMensaje('')
    try {
      await actualizarAdministrador(item, pisoId || null)
      setMensaje('Asignación de piso actualizada.'); await cargar()
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo actualizar la asignación.') }
    finally { setOcupado(false) }
  }

  return <DashboardLayout breadcrumb="Administración institucional">
    <section className="admin-institution">
      <header><p>Catálogos y asociaciones globales</p><h1>Administración institucional</h1></header>
      {cargando && <p role="status">Cargando configuración...</p>}
      {error && <p role="alert" className="admin-institution__error">{error}</p>}
      {mensaje && <p role="status" className="admin-institution__success">{mensaje}</p>}
      {!cargando && !error && <>
        <div className="admin-institution__summary">
          <Summary label="Campus" value={campus.length} /><Summary label="Pisos" value={pisos.length} />
          <Summary label="Carreras" value={carreras.length} /><Summary label="Materias" value={materias.length} />
          <Summary label="Ciclos" value={periodos.length} /><Summary label="Laboratorios" value={laboratorios.length} />
          <Summary label="Equipos" value={equipos.length} />
        </div>

        <section><h2>Administradores de piso</h2><div className="admin-institution__table-wrap"><table><thead><tr><th>Administrador</th><th>Perfil</th><th>Piso asignado</th></tr></thead><tbody>
          {administradores.map((item) => { const perfil = perfilPorId.get(item.perfilId); return <tr key={item.id}><td>{item.codigoAdministrador}</td><td>{perfil ? `${perfil.nombres} ${perfil.apellidos}` : 'Perfil institucional'}</td><td><select aria-label={`Piso de ${item.codigoAdministrador}`} value={item.pisoId ?? ''} disabled={ocupado} onChange={(event) => void asignarPiso(item, event.target.value)}><option value="">Sin piso</option>{pisos.map((piso) => <option key={piso.id} value={piso.id}>Piso {piso.numero}</option>)}</select></td></tr> })}
        </tbody></table></div></section>

        <section><h2>Laboratorios</h2><form onSubmit={guardarLaboratorio} className="admin-institution__form">
          <label>Código<input required value={labForm.codigo} onChange={(e) => setLabForm({ ...labForm, codigo: e.target.value })} /></label>
          <label>Nombre<input required value={labForm.nombre} onChange={(e) => setLabForm({ ...labForm, nombre: e.target.value })} /></label>
          <label>Piso<select required value={labForm.pisoId} onChange={(e) => setLabForm({ ...labForm, pisoId: e.target.value })}><option value="">Seleccione un piso</option>{pisos.map((piso) => <option key={piso.id} value={piso.id}>Piso {piso.numero}</option>)}</select></label>
          <label>Capacidad<input required min="1" type="number" value={labForm.capacidad} onChange={(e) => setLabForm({ ...labForm, capacidad: e.target.value })} /></label>
          <label className="wide">Descripción<input value={labForm.descripcion} onChange={(e) => setLabForm({ ...labForm, descripcion: e.target.value })} /></label>
          <button disabled={ocupado}>{labForm.id ? 'Guardar cambios' : 'Crear laboratorio'}</button>
          {labForm.id && <button type="button" onClick={() => setLabForm(labInicial)}>Cancelar</button>}
        </form><div className="admin-institution__cards">{laboratorios.map((lab) => <article key={lab.id}><strong>{lab.codigo} — {lab.nombre}</strong><span>{pisoNombre(lab.pisoId)} · Capacidad {lab.capacidad}</span><span>{lab.estado}</span><div><button onClick={() => setLabForm({ id: lab.id, pisoId: lab.pisoId, codigo: lab.codigo, nombre: lab.nombre, capacidad: String(lab.capacidad), descripcion: lab.descripcion ?? '' })}>Editar</button><button disabled={ocupado} onClick={() => void cambiarEstadoLaboratorio(lab.id, lab.estado === 'INACTIVO' ? 'DISPONIBLE' : 'INACTIVO').then(cargar).catch((cause: Error) => setError(cause.message))}>{lab.estado === 'INACTIVO' ? 'Activar' : 'Desactivar'}</button></div></article>)}</div></section>

        <section><h2>Equipos</h2><form onSubmit={guardarEquipo} className="admin-institution__form"><label>Laboratorio<select required value={equipo.laboratorioId} onChange={(e) => setEquipo({ ...equipo, laboratorioId: e.target.value })}><option value="">Seleccione</option>{laboratorios.map((lab) => <option key={lab.id} value={lab.id}>{lab.codigo} — {lab.nombre}</option>)}</select></label><label>Tipo<select required value={equipo.tipoEquipoId} onChange={(e) => setEquipo({ ...equipo, tipoEquipoId: e.target.value })}><option value="">Seleccione</option>{tipos.map((tipo) => <option key={tipo.id} value={tipo.id}>{tipo.nombre}</option>)}</select></label><label>Código de inventario<input required value={equipo.codigoInventario} onChange={(e) => setEquipo({ ...equipo, codigoInventario: e.target.value })} /></label><button disabled={ocupado}>Crear equipo</button></form></section>

        <section><h2>Estructura académica</h2><div className="admin-institution__catalogs"><article><h3>Campus</h3>{campus.map((item) => <p key={item.id}>{item.codigo} — {item.nombre}</p>)}</article><article><h3>Carreras</h3>{carreras.map((item) => <p key={item.id}>{item.codigo} — {item.nombre}</p>)}</article><article><h3>Materias por nivel</h3>{materias.map((item) => <p key={item.id}>{item.codigo} — {item.nombre} · Nivel {item.nivel ?? 'sin definir'}</p>)}</article><article><h3>Períodos y ciclos</h3>{periodos.map((item) => <p key={item.id}>{item.ppaNombre ?? item.nombre} · Ciclo {item.cicloAcademico ?? 'histórico'}</p>)}</article></div></section>
      </>}
    </section>
  </DashboardLayout>
}

function Summary({ label, value }: { label: string; value: number }) { return <article><span>{label}</span><strong>{value}</strong></article> }
