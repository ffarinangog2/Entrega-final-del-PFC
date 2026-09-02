import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { DashboardLayout } from '../components/DashboardLayout'
import { LaboratoriosPanel } from '../features/laboratorios/LaboratoriosPanel'
import { MonitoreoPanel } from '../features/monitoreo/MonitoreoPanel'
import { AdminDashboard } from '../features/admin/AdminDashboard'
import { hasRole, useAuth } from '../auth'
import {
  obtenerDocentePorPerfil,
  obtenerHorariosDocente,
  obtenerLaboratorios,
  obtenerMaterias,
  obtenerPeriodoActual,
  obtenerCarreras,
  type HorarioAcademico,
  type Laboratorio,
  type Materia,
} from '../services/academicoApi'
import {
  listarPlanificaciones,
  type Planificacion,
} from '../services/operationalApi'

const dias = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES']

function MiSemana({ perfilId }: { perfilId: string }) {
  const [horarios, setHorarios] = useState<HorarioAcademico[]>([])
  const [materias, setMaterias] = useState<Materia[]>([])
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    async function cargar() {
      setCargando(true)
      setError('')
      try {
        const docente = await obtenerDocentePorPerfil(perfilId)
        const [horariosData, materiasData, laboratoriosData] =
          await Promise.all([
            obtenerHorariosDocente(docente.id),
            obtenerMaterias(),
            obtenerLaboratorios(),
          ])
        if (!active) return
        setHorarios(horariosData.filter((horario) => horario.activo))
        setMaterias(materiasData)
        setLaboratorios(laboratoriosData)
      } catch (cause) {
        if (active)
          setError(
            cause instanceof Error
              ? cause.message
              : 'No se pudo cargar su horario.',
          )
      } finally {
        if (active) setCargando(false)
      }
    }
    void cargar()
    return () => {
      active = false
    }
  }, [perfilId])

  const materiaPorId = useMemo(
    () => new Map(materias.map((materia) => [materia.id, materia])),
    [materias],
  )
  const laboratorioPorId = useMemo(
    () =>
      new Map(laboratorios.map((laboratorio) => [laboratorio.id, laboratorio])),
    [laboratorios],
  )
  const diaActual = dias[new Date().getDay() - 1]
  const clasesHoy = horarios.filter(
    (horario) => horario.diaSemana === diaActual,
  )

  return (
    <section className="my-week" aria-labelledby="mi-semana-title">
      <header>
        <div>
          <p>Docencia</p>
          <h1 id="mi-semana-title">Mi semana</h1>
        </div>
        <Link to="/reservas/nueva">Nueva solicitud extraordinaria</Link>
      </header>
      {cargando && <p role="status">Cargando su horario...</p>}
      {!cargando && error && (
        <p role="alert" className="my-week__error">
          {error}
        </p>
      )}
      {!cargando && !error && horarios.length === 0 && (
        <p>No tiene clases asignadas en el periodo actual.</p>
      )}
      {!cargando && !error && horarios.length > 0 && (
        <>
          <section
            className="my-week__today"
            aria-labelledby="clases-hoy-title"
          >
            <h2 id="clases-hoy-title">Hoy</h2>
            {clasesHoy.length === 0 ? (
              <p>No tienes clases programadas para hoy.</p>
            ) : (
              clasesHoy.map((clase) => (
                <article key={clase.id}>
                  <time>
                    {clase.horaInicio}–{clase.horaFin}
                  </time>
                  <strong>
                    {materiaPorId.get(clase.materiaId)?.nombre ??
                      'Materia asignada'}
                  </strong>
                  <span>
                    {clase.laboratorioId
                      ? (laboratorioPorId.get(clase.laboratorioId)?.nombre ??
                        'Laboratorio asignado')
                      : 'Aula por confirmar'}
                  </span>
                  <span>Programada</span>
                </article>
              ))
            )}
          </section>
          <div className="my-week__grid">
            {dias.map((dia) => {
              const clases = horarios
                .filter((horario) => horario.diaSemana === dia)
                .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio))
              return (
                <section key={dia} className="my-week__day">
                  <h2>{dia.charAt(0) + dia.slice(1).toLowerCase()}</h2>
                  {clases.length === 0 ? (
                    <p>Sin clases</p>
                  ) : (
                    clases.map((clase) => (
                      <article key={clase.id}>
                        <time>
                          {clase.horaInicio}–{clase.horaFin}
                        </time>
                        <strong>
                          {materiaPorId.get(clase.materiaId)?.nombre ??
                            'Materia asignada'}
                        </strong>
                        <span>
                          {clase.laboratorioId
                            ? (laboratorioPorId.get(clase.laboratorioId)
                                ?.nombre ?? 'Laboratorio asignado')
                            : 'Aula por confirmar'}
                        </span>
                      </article>
                    ))
                  )}
                </section>
              )
            })}
          </div>
        </>
      )}
    </section>
  )
}

function InicioPorRol() {
  const { usuario } = useAuth()
  const estudiante = hasRole(usuario, 'ESTUDIANTE')
  const coordinador = hasRole(usuario, 'COORDINADOR')
  const administradorPiso = hasRole(usuario, 'ADMINISTRADOR_PISO')
  const [resumenCoordinacion, setResumenCoordinacion] = useState<{
    periodo: string
    carrera: string
    planes: Planificacion[]
    materias: number
    docentes: number
    laboratorios: number
  } | null>(null)
  const [errorCoordinacion, setErrorCoordinacion] = useState('')
  useEffect(() => {
    if (!coordinador) return
    let active = true
    void Promise.all([
      listarPlanificaciones(),
      obtenerPeriodoActual(),
      obtenerMaterias(),
      obtenerCarreras(),
    ])
      .then(([planes, periodo, materias, carreras]) => {
        if (!active) return
        const carreraId = materias[0]?.carreraId
        setResumenCoordinacion({
          periodo: periodo.nombre,
          carrera:
            carreras.find((item) => item.id === carreraId)?.nombre ??
            'Mi carrera institucional',
          planes,
          materias: new Set(planes.map((item) => item.materiaId)).size,
          docentes: new Set(
            planes.map((item) => item.docenteId).filter(Boolean),
          ).size,
          laboratorios: new Set(planes.map((item) => item.laboratorioId)).size,
        })
      })
      .catch((cause) => {
        if (active)
          setErrorCoordinacion(
            cause instanceof Error
              ? cause.message
              : 'No se pudo cargar el resumen de coordinación.',
          )
      })
    return () => {
      active = false
    }
  }, [coordinador])
  const titulo = estudiante
    ? 'Mi información académica'
    : coordinador
      ? 'Planificación de mi carrera'
      : administradorPiso
        ? 'Operación de mi piso'
        : 'Bienvenido a SCLI'
  const descripcion = estudiante
    ? 'Registre su presencia cuando exista una actividad de laboratorio habilitada y consulte su historial propio.'
    : coordinador
      ? 'Organice el horario semestral de su carrera y consulte su estado sin intervenir en la operación diaria.'
      : administradorPiso
        ? 'Revise la planificación, las solicitudes y los incidentes correspondientes a su piso.'
        : 'Use el menú para acceder a las funciones disponibles para su perfil.'

  return (
    <section className="role-home">
      <h1>{titulo}</h1>
      <p>{descripcion}</p>
      <div className="role-home__links">
        {coordinador && (
          <>
            {errorCoordinacion && <p role="alert">{errorCoordinacion}</p>}
            {resumenCoordinacion && (
              <section
                className="role-home__summary"
                aria-label="Resumen de coordinación"
              >
                <p>
                  <strong>Carrera:</strong> {resumenCoordinacion.carrera}
                </p>
                <p>
                  <strong>Periodo:</strong> {resumenCoordinacion.periodo}
                </p>
                <p>
                  <strong>Estado:</strong>{' '}
                  {resumenCoordinacion.planes.some(
                    (item) => item.estado === 'PROPUESTA_CAMBIO',
                  )
                    ? 'Devuelta con propuesta'
                    : resumenCoordinacion.planes.some(
                          (item) => item.estado === 'ENVIADA',
                        )
                      ? 'En revisión'
                      : resumenCoordinacion.planes.length > 0 &&
                          resumenCoordinacion.planes.every(
                            (item) => item.estado === 'CONFIRMADA',
                          )
                        ? 'Aprobada'
                        : resumenCoordinacion.planes.length > 0
                          ? 'Borrador'
                          : 'Sin iniciar'}
                </p>
                <p>
                  {resumenCoordinacion.materias} materias ·{' '}
                  {resumenCoordinacion.docentes} docentes ·{' '}
                  {resumenCoordinacion.laboratorios} laboratorios
                </p>
              </section>
            )}
            <Link to="/planificacion">Abrir planificación</Link>
            <Link to="/reservas/calendario">Consultar calendario</Link>
          </>
        )}
        {administradorPiso && (
          <>
            <Link to="/planificacion">Planificación recibida</Link>
            <Link to="/reservas">Solicitudes de mi piso</Link>
            <Link to="/incidentes">Incidentes de mi piso</Link>
          </>
        )}
        {estudiante && (
          <>
            <Link to="/asistencia">Registro de laboratorio</Link>
            <Link to="/perfil">Mi perfil</Link>
          </>
        )}
      </div>
    </section>
  )
}

export function MainPage() {
  const { usuario } = useAuth()
  const administrador = hasRole(usuario, 'ADMINISTRADOR')
  const administradorPiso = hasRole(usuario, 'ADMINISTRADOR_PISO')
  const docente = hasRole(usuario, 'DOCENTE')
  return (
    <DashboardLayout breadcrumb="Inicio">
      {administrador && (
        <>
          <AdminDashboard />
          <MonitoreoPanel />
        </>
      )}
      {administradorPiso && <LaboratoriosPanel />}
      {docente && usuario?.perfilId && <MiSemana perfilId={usuario.perfilId} />}
      {!administrador && !docente && <InicioPorRol />}
    </DashboardLayout>
  )
}
