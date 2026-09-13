import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import {
  actualizarLaboratorio,
  actualizarCampus,
  actualizarCarrera,
  actualizarEquipo,
  actualizarMateria,
  actualizarPiso,
  actualizarPeriodo, crearPeriodo,
  cambiarEstadoEquipo,
  cambiarEstadoLaboratorio,
  crearCampus,
  crearCarrera,
  crearEquipo,
  crearLaboratorio,
  crearMateria,
  crearPiso,
  obtenerBloques,
  obtenerCampus,
  obtenerCarreras,
  obtenerEquipos,
  obtenerLaboratorios,
  obtenerMaterias,
  obtenerPeriodos,
  obtenerPisos,
  obtenerTiposEquipo,
  obtenerFacultades,
  type Bloque,
  type Facultad,
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
type EquipoForm = { id: string; laboratorioId: string; tipoEquipoId: string; codigoInventario: string; numeroSerie: string; marca: string; modelo: string }
const equipoInicial: EquipoForm = { id: '', laboratorioId: '', tipoEquipoId: '', codigoInventario: '', numeroSerie: '', marca: '', modelo: '' }

export type AdminInstitutionModule = 'all' | 'overview' | 'laboratorios' | 'pisos' | 'equipos' | 'catalogos' | 'asignaciones'

export function AdminInstitutionPage({ module = 'all' }: { module?: AdminInstitutionModule }) {
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [pisos, setPisos] = useState<Piso[]>([])
  const [equipos, setEquipos] = useState<Equipo[]>([])
  const [tipos, setTipos] = useState<TipoEquipo[]>([])
  const [bloques, setBloques] = useState<Bloque[]>([])
  const [facultades, setFacultades] = useState<Facultad[]>([])
  const [campus, setCampus] = useState<Campus[]>([])
  const [carreras, setCarreras] = useState<Carrera[]>([])
  const [materias, setMaterias] = useState<Materia[]>([])
  const [periodos, setPeriodos] = useState<PeriodoLectivo[]>([])
  const [administradores, setAdministradores] = useState<AdministradorInstitucional[]>([])
  const [perfiles, setPerfiles] = useState<Perfil[]>([])
  const [labForm, setLabForm] = useState(labInicial)
  const [equipo, setEquipo] = useState<EquipoForm>(equipoInicial)
  const [campusForm, setCampusForm] = useState({ id: '', codigo: '', nombre: '', direccion: '' })
  const [pisoForm, setPisoForm] = useState({ id: '', bloqueId: '', numero: '1', descripcion: '' })
  const [carreraForm, setCarreraForm] = useState({ id: '', facultadId: '', codigo: '', nombre: '', descripcion: '' })
  const [materiaForm, setMateriaForm] = useState({ id: '', carreraId: '', codigo: '', nombre: '', numeroHoras: '1', nivel: '1' })
  const [periodoForm,setPeriodoForm]=useState({id:'',anio:'2026-2027',tipo:'PPA' as 'PPA'|'SPA',fechaInicio:'',fechaFin:'',estado:'PLANIFICADO' as PeriodoLectivo['estado']})
  const [pisoAbierto, setPisoAbierto] = useState('')
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
        listarAdministradores(), listarPerfiles(), obtenerBloques(), obtenerFacultades(),
      ])
      setLaboratorios(data[0]); setPisos(data[1]); setEquipos(data[2]); setTipos(data[3])
      setCampus(data[4]); setCarreras(data[5]); setMaterias(data[6]); setPeriodos(data[7])
      setAdministradores(data[8]); setPerfiles(data[9])
      setBloques(data[10] ?? []); setFacultades(data[11] ?? [])
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No fue posible cargar la configuración institucional.')
    } finally { setCargando(false) }
  }, [])
  useEffect(() => void cargar(), [cargar])

  const perfilPorId = useMemo(() => new Map(perfiles.map((item) => [item.id, item])), [perfiles])
  const pisoNombre = (id: string | null) => id ? `Piso ${pisos.find((item) => item.id === id)?.numero ?? 'desconocido'}` : 'Sin piso'
  const mostrar = (seccion: AdminInstitutionModule) => module === 'all' || module === seccion
  const encabezados: Record<AdminInstitutionModule, { eyebrow: string; title: string }> = {
    all: { eyebrow: 'Catálogos y asociaciones globales', title: 'Administración institucional' },
    overview: { eyebrow: 'Resumen global', title: 'Administración institucional' },
    laboratorios: { eyebrow: 'Infraestructura académica', title: 'Gestión de laboratorios' },
    pisos: { eyebrow: 'Estructura física', title: 'Gestión de pisos' },
    equipos: { eyebrow: 'Inventario institucional', title: 'Gestión de equipos' },
    catalogos: { eyebrow: 'Configuración académica', title: 'Catálogos institucionales' },
    asignaciones: { eyebrow: 'Ámbitos institucionales', title: 'Asignaciones administrativas' },
  }
  const encabezado = encabezados[module]

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
      const body = { laboratorioId: equipo.laboratorioId, tipoEquipoId: equipo.tipoEquipoId,
        codigoInventario: equipo.codigoInventario, numeroSerie: equipo.numeroSerie, marca: equipo.marca,
        modelo: equipo.modelo, procesador: '', memoriaRam: '', almacenamiento: '', direccionIp: '', direccionMac: '', observacion: '' }
      if (equipo.id) await actualizarEquipo(equipo.id, body)
      else await crearEquipo(body)
      setEquipo(equipoInicial)
      setMensaje('Equipo guardado correctamente.'); await cargar()
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo crear el equipo.') }
    finally { setOcupado(false) }
  }

  async function guardarCatalogo(event: FormEvent, tipo: 'campus' | 'piso' | 'carrera' | 'materia') {
    event.preventDefault(); setOcupado(true); setError(''); setMensaje('')
    try {
      if (tipo === 'campus') {
        const body = { codigo: campusForm.codigo, nombre: campusForm.nombre, direccion: campusForm.direccion }
        if (campusForm.id) await actualizarCampus(campusForm.id, body); else await crearCampus(body)
        setCampusForm({ id: '', codigo: '', nombre: '', direccion: '' })
      } else if (tipo === 'piso') {
        const body = { bloqueId: pisoForm.bloqueId, numero: Number(pisoForm.numero), descripcion: pisoForm.descripcion }
        if (pisoForm.id) await actualizarPiso(pisoForm.id, body); else await crearPiso(body)
        setPisoForm({ id: '', bloqueId: '', numero: '1', descripcion: '' })
      } else if (tipo === 'carrera') {
        const body = { facultadId: carreraForm.facultadId, codigo: carreraForm.codigo, nombre: carreraForm.nombre, descripcion: carreraForm.descripcion }
        if (carreraForm.id) await actualizarCarrera(carreraForm.id, body); else await crearCarrera(body)
        setCarreraForm({ id: '', facultadId: '', codigo: '', nombre: '', descripcion: '' })
      } else {
        const body = { carreraId: materiaForm.carreraId, codigo: materiaForm.codigo, nombre: materiaForm.nombre, numeroHoras: Number(materiaForm.numeroHoras), nivel: Number(materiaForm.nivel) }
        if (materiaForm.id) await actualizarMateria(materiaForm.id, body); else await crearMateria(body)
        setMateriaForm({ id: '', carreraId: '', codigo: '', nombre: '', numeroHoras: '1', nivel: '1' })
      }
      setMensaje('Catálogo actualizado correctamente.'); await cargar()
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo actualizar el catálogo.') }
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
  async function guardarPeriodo(event:FormEvent){event.preventDefault();setOcupado(true);setError('');try{const visible=`REGULAR ${periodoForm.anio} ${periodoForm.tipo}`;const body={codigo:visible,nombre:visible,ppaCodigo:visible,ppaNombre:visible,fechaInicio:periodoForm.fechaInicio,fechaFin:periodoForm.fechaFin,estado:periodoForm.estado,cicloAcademico:(periodoForm.tipo==='PPA'?1:2) as 1|2};if(periodoForm.id)await actualizarPeriodo(periodoForm.id,body);else await crearPeriodo(body);setPeriodoForm({id:'',anio:'2026-2027',tipo:'PPA',fechaInicio:'',fechaFin:'',estado:'PLANIFICADO'});setMensaje('Período académico guardado.');await cargar()}catch(cause){setError(cause instanceof Error?cause.message:'No se pudo guardar el período.')}finally{setOcupado(false)}}

  return <DashboardLayout breadcrumb={encabezado.title}>
    <section className="admin-institution">
      <header><p>{encabezado.eyebrow}</p><h1>{encabezado.title}</h1></header>
      {cargando && <p role="status">Cargando configuración...</p>}
      {error && <p role="alert" className="admin-institution__error">{error}</p>}
      {mensaje && <p role="status" className="admin-institution__success">{mensaje}</p>}
      {!cargando && !error && <>
        {mostrar('overview') && <div className="admin-institution__summary">
          <Summary label="Campus" value={campus.length} /><Summary label="Pisos" value={pisos.length} />
          <Summary label="Carreras" value={carreras.length} /><Summary label="Materias" value={materias.length} />
          <Summary label="Períodos académicos" value={periodos.length} /><Summary label="Laboratorios" value={laboratorios.length} />
          <Summary label="Equipos" value={equipos.length} />
          <Summary label="Asignaciones" value={administradores.filter((item) => item.pisoId).length} />
        </div>}

        {mostrar('asignaciones') && <section><h2>Administradores de piso</h2><div className="admin-institution__table-wrap"><table><thead><tr><th>Administrador</th><th>Perfil</th><th>Piso asignado</th></tr></thead><tbody>
          {administradores.map((item) => { const perfil = perfilPorId.get(item.perfilId); return <tr key={item.id}><td>{item.codigoAdministrador}</td><td>{perfil ? `${perfil.nombres} ${perfil.apellidos}` : 'Perfil institucional'}</td><td><select aria-label={`Piso de ${item.codigoAdministrador}`} value={item.pisoId ?? ''} disabled={ocupado} onChange={(event) => void asignarPiso(item, event.target.value)}><option value="">Sin piso</option>{pisos.map((piso) => <option key={piso.id} value={piso.id}>Piso {piso.numero}</option>)}</select></td></tr> })}
        </tbody></table></div></section>}

        {mostrar('laboratorios') && <section><h2>Laboratorios</h2><form onSubmit={guardarLaboratorio} className="admin-institution__form">
          <label>Código<input required value={labForm.codigo} onChange={(e) => setLabForm({ ...labForm, codigo: e.target.value })} /></label>
          <label>Nombre<input required value={labForm.nombre} onChange={(e) => setLabForm({ ...labForm, nombre: e.target.value })} /></label>
          <label>Piso<select required value={labForm.pisoId} onChange={(e) => setLabForm({ ...labForm, pisoId: e.target.value })}><option value="">Seleccione un piso</option>{pisos.map((piso) => <option key={piso.id} value={piso.id}>Piso {piso.numero}</option>)}</select></label>
          <label>Capacidad<input required min="1" type="number" value={labForm.capacidad} onChange={(e) => setLabForm({ ...labForm, capacidad: e.target.value })} /></label>
          <label className="wide">Descripción<input value={labForm.descripcion} onChange={(e) => setLabForm({ ...labForm, descripcion: e.target.value })} /></label>
          <button disabled={ocupado}>{labForm.id ? 'Guardar cambios' : 'Crear laboratorio'}</button>
          {labForm.id && <button type="button" onClick={() => setLabForm(labInicial)}>Cancelar</button>}
        </form><div className="admin-institution__cards">{laboratorios.map((lab) => <article key={lab.id}><strong>{lab.codigo} — {lab.nombre}</strong><span>{pisoNombre(lab.pisoId)} · Capacidad {lab.capacidad}</span><span>{lab.estado}</span><div><button onClick={() => setLabForm({ id: lab.id, pisoId: lab.pisoId, codigo: lab.codigo, nombre: lab.nombre, capacidad: String(lab.capacidad), descripcion: lab.descripcion ?? '' })}>Editar</button><button disabled={ocupado} onClick={() => void cambiarEstadoLaboratorio(lab.id, lab.estado === 'INACTIVO' ? 'DISPONIBLE' : 'INACTIVO').then(cargar).catch((cause: Error) => setError(cause.message))}>{lab.estado === 'INACTIVO' ? 'Activar' : 'Desactivar'}</button></div></article>)}</div></section>}

        {mostrar('equipos') && <section><h2>Equipos</h2><form onSubmit={guardarEquipo} className="admin-institution__form"><label>Laboratorio<select required value={equipo.laboratorioId} onChange={(e) => setEquipo({ ...equipo, laboratorioId: e.target.value })}><option value="">Seleccione</option>{laboratorios.map((lab) => <option key={lab.id} value={lab.id}>{lab.codigo} — {lab.nombre}</option>)}</select></label><label>Tipo<select required value={equipo.tipoEquipoId} onChange={(e) => setEquipo({ ...equipo, tipoEquipoId: e.target.value })}><option value="">Seleccione</option>{tipos.map((tipo) => <option key={tipo.id} value={tipo.id}>{tipo.nombre}</option>)}</select></label><label>Código de inventario<input required value={equipo.codigoInventario} onChange={(e) => setEquipo({ ...equipo, codigoInventario: e.target.value })} /></label><button disabled={ocupado}>{equipo.id ? 'Guardar equipo' : 'Crear equipo'}</button></form></section>}

        {mostrar('catalogos') && <section><h2>Estructura académica</h2><div className="admin-institution__catalogs"><article><h3>Campus</h3>{campus.map((item) => <p key={item.id}>{item.codigo} — {item.nombre}</p>)}</article><article><h3>Carreras</h3>{carreras.map((item) => <p key={item.id}>{item.codigo} — {item.nombre}</p>)}</article><article><h3>Materias por nivel</h3>{materias.map((item) => <p key={item.id}>{item.codigo} — {item.nombre} · Nivel {item.nivel ?? 'sin definir'}</p>)}</article><article><h3>Períodos académicos</h3>{periodos.map((item) => <p key={item.id}>{item.ppaNombre ?? item.nombre}</p>)}</article></div></section>}
        {mostrar('catalogos')&&<section><h2>Gestionar períodos PPA / SPA</h2><form className="admin-institution__form" onSubmit={guardarPeriodo}><label>Año académico<input required pattern="\d{4}-\d{4}" value={periodoForm.anio} onChange={e=>setPeriodoForm({...periodoForm,anio:e.target.value})}/></label><label>Tipo de período<select value={periodoForm.tipo} onChange={e=>setPeriodoForm({...periodoForm,tipo:e.target.value as 'PPA'|'SPA'})}><option>PPA</option><option>SPA</option></select></label><label>Fecha inicio<input required type="date" value={periodoForm.fechaInicio} onChange={e=>setPeriodoForm({...periodoForm,fechaInicio:e.target.value})}/></label><label>Fecha fin<input required type="date" value={periodoForm.fechaFin} onChange={e=>setPeriodoForm({...periodoForm,fechaFin:e.target.value})}/></label><label>Estado administrativo<select value={periodoForm.estado} onChange={e=>setPeriodoForm({...periodoForm,estado:e.target.value as PeriodoLectivo['estado']})}><option value="PLANIFICADO">Planificado</option><option value="ACTIVO">Activo</option><option value="FINALIZADO">Finalizado</option></select></label><button disabled={ocupado}>{periodoForm.id?'Guardar período':'Crear período'}</button></form><div className="admin-institution__cards">{periodos.filter(p=>p.cicloAcademico).map(p=><article key={p.id}><strong>{p.ppaNombre??p.nombre}</strong><span>{p.fechaInicio} — {p.fechaFin}</span><button onClick={()=>{const nombre=p.ppaNombre??p.nombre;setPeriodoForm({id:p.id,anio:nombre.match(/\d{4}-\d{4}/)?.[0]??'',tipo:p.cicloAcademico===2?'SPA':'PPA',fechaInicio:p.fechaInicio,fechaFin:p.fechaFin,estado:p.estado})}}>Editar</button></article>)}</div></section>}        {mostrar('pisos') && <section><h2>Gestión de pisos</h2><form onSubmit={(e) => void guardarCatalogo(e, 'piso')} className="admin-institution__form"><label>Bloque / campus<select required value={pisoForm.bloqueId} onChange={(e) => setPisoForm({ ...pisoForm, bloqueId: e.target.value })}><option value="">Seleccione</option>{bloques.map((bloque) => <option key={bloque.id} value={bloque.id}>{bloque.nombre} · {campus.find((c) => c.id === bloque.campusId)?.nombre ?? 'Campus'}</option>)}</select></label><label>Número<input required min="0" type="number" value={pisoForm.numero} onChange={(e) => setPisoForm({ ...pisoForm, numero: e.target.value })} /></label><label>Descripción<input value={pisoForm.descripcion} onChange={(e) => setPisoForm({ ...pisoForm, descripcion: e.target.value })} /></label><button disabled={ocupado}>{pisoForm.id ? 'Guardar piso' : 'Crear piso'}</button></form><div className="admin-institution__cards">{pisos.map((piso) => <article key={piso.id}><strong>Piso {piso.numero}</strong><span>{piso.descripcion}</span><span>{laboratorios.filter((lab) => lab.pisoId === piso.id).length} laboratorios</span><div><button onClick={() => setPisoAbierto(pisoAbierto === piso.id ? '' : piso.id)}>Ver laboratorios</button><button onClick={() => setPisoForm({ id: piso.id, bloqueId: piso.bloqueId, numero: String(piso.numero), descripcion: piso.descripcion ?? '' })}>Editar</button></div>{pisoAbierto === piso.id && <ul>{laboratorios.filter((lab) => lab.pisoId === piso.id).map((lab) => <li key={lab.id}>{lab.codigo} — {lab.nombre}</li>)}</ul>}</article>)}</div></section>}

        {mostrar('equipos') && <section><h2>Inventario de equipos</h2><div className="admin-institution__cards">{equipos.map((item) => <article key={item.id}><strong>{item.codigoInventario}</strong><span>{laboratorios.find((lab) => lab.id === item.laboratorioId)?.codigo ?? 'Laboratorio'}</span><span>{item.estado}</span><div><button onClick={() => setEquipo({ id: item.id, laboratorioId: item.laboratorioId, tipoEquipoId: item.tipoEquipoId, codigoInventario: item.codigoInventario, numeroSerie: item.numeroSerie ?? '', marca: item.marca ?? '', modelo: item.modelo ?? '' })}>Editar</button><button onClick={() => void cambiarEstadoEquipo(item.id, item.estado === 'INACTIVO' ? 'OPERATIVO' : 'INACTIVO').then(cargar).catch((cause: Error) => setError(cause.message))}>{item.estado === 'INACTIVO' ? 'Activar' : 'Desactivar'}</button></div></article>)}</div></section>}

        {mostrar('catalogos') && <section><h2>Gestionar campus</h2><form onSubmit={(e) => void guardarCatalogo(e, 'campus')} className="admin-institution__form"><label>Código<input required value={campusForm.codigo} onChange={(e) => setCampusForm({ ...campusForm, codigo: e.target.value })} /></label><label>Nombre<input required value={campusForm.nombre} onChange={(e) => setCampusForm({ ...campusForm, nombre: e.target.value })} /></label><label>Dirección<input value={campusForm.direccion} onChange={(e) => setCampusForm({ ...campusForm, direccion: e.target.value })} /></label><button>{campusForm.id ? 'Guardar campus' : 'Crear campus'}</button></form><div className="admin-institution__cards">{campus.map((item) => <article key={item.id}><strong>{item.codigo} — {item.nombre}</strong><span>{item.direccion}</span><button onClick={() => setCampusForm({ id: item.id, codigo: item.codigo, nombre: item.nombre, direccion: item.direccion ?? '' })}>Editar</button></article>)}</div></section>}

        {mostrar('catalogos') && <section><h2>Gestionar carreras</h2><form onSubmit={(e) => void guardarCatalogo(e, 'carrera')} className="admin-institution__form"><label>Facultad<select required value={carreraForm.facultadId} onChange={(e) => setCarreraForm({ ...carreraForm, facultadId: e.target.value })}><option value="">Seleccione</option>{facultades.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label><label>Código<input required value={carreraForm.codigo} onChange={(e) => setCarreraForm({ ...carreraForm, codigo: e.target.value })} /></label><label>Nombre<input required value={carreraForm.nombre} onChange={(e) => setCarreraForm({ ...carreraForm, nombre: e.target.value })} /></label><label>Descripción<input value={carreraForm.descripcion} onChange={(e) => setCarreraForm({ ...carreraForm, descripcion: e.target.value })} /></label><button>{carreraForm.id ? 'Guardar carrera' : 'Crear carrera'}</button></form><div className="admin-institution__cards">{carreras.map((item) => <article key={item.id}><strong>{item.codigo} — {item.nombre}</strong><button onClick={() => setCarreraForm({ id: item.id, facultadId: item.facultadId, codigo: item.codigo, nombre: item.nombre, descripcion: '' })}>Editar</button></article>)}</div></section>}

        {mostrar('catalogos') && <section><h2>Gestionar materias</h2><form onSubmit={(e) => void guardarCatalogo(e, 'materia')} className="admin-institution__form"><label>Carrera<select required value={materiaForm.carreraId} onChange={(e) => setMateriaForm({ ...materiaForm, carreraId: e.target.value })}><option value="">Seleccione</option>{carreras.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label><label>Código<input required value={materiaForm.codigo} onChange={(e) => setMateriaForm({ ...materiaForm, codigo: e.target.value })} /></label><label>Nombre<input required value={materiaForm.nombre} onChange={(e) => setMateriaForm({ ...materiaForm, nombre: e.target.value })} /></label><label>Horas<input min="1" type="number" value={materiaForm.numeroHoras} onChange={(e) => setMateriaForm({ ...materiaForm, numeroHoras: e.target.value })} /></label><label>Nivel<select value={materiaForm.nivel} onChange={(e) => setMateriaForm({ ...materiaForm, nivel: e.target.value })}>{Array.from({ length: 10 }, (_, index) => <option key={index + 1}>{index + 1}</option>)}</select></label><button>{materiaForm.id ? 'Guardar materia' : 'Crear materia'}</button></form><div className="admin-institution__cards">{materias.map((item) => <article key={item.id}><strong>{item.codigo} — {item.nombre}</strong><span>{carreras.find((c) => c.id === item.carreraId)?.nombre} · Nivel {item.nivel ?? 'sin definir'}</span><button onClick={() => setMateriaForm({ id: item.id, carreraId: item.carreraId, codigo: item.codigo, nombre: item.nombre, numeroHoras: String(item.numeroHoras), nivel: String(item.nivel ?? 1) })}>Editar</button></article>)}</div></section>}
      </>}
    </section>
  </DashboardLayout>
}

function Summary({ label, value }: { label: string; value: number }) { return <article><span>{label}</span><strong>{value}</strong></article> }

export function AdminOverviewPage() { return <AdminInstitutionPage module="overview" /> }
export function AdminLaboratoriosPage() { return <AdminInstitutionPage module="laboratorios" /> }
export function AdminPisosPage() { return <AdminInstitutionPage module="pisos" /> }
export function AdminEquiposPage() { return <AdminInstitutionPage module="equipos" /> }
export function AdminCatalogosPage() { return <AdminInstitutionPage module="catalogos" /> }
export function AdminAsignacionesPage() { return <AdminInstitutionPage module="asignaciones" /> }
