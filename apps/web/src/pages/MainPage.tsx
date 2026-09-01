import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { DashboardLayout } from '../components/DashboardLayout'
import { LaboratoriosPanel } from '../features/laboratorios/LaboratoriosPanel'
import { MonitoreoPanel } from '../features/monitoreo/MonitoreoPanel'
import { hasRole, useAuth } from '../auth'
import {
  obtenerDocentePorPerfil,
  obtenerHorariosDocente,
  obtenerLaboratorios,
  obtenerMaterias,
  type HorarioAcademico,
  type Laboratorio,
  type Materia,
} from '../services/academicoApi'

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
      )}
    </section>
  )
}

function InicioPorRol() {
  const { usuario } = useAuth()
  const estudiante = hasRole(usuario, 'ESTUDIANTE')
  const coordinador = hasRole(usuario, 'COORDINADOR')
  const administradorPiso = hasRole(usuario, 'ADMINISTRADOR_PISO')
  const titulo = estudiante
    ? 'Mi información académica'
    : coordinador
      ? 'Planificación de mi carrera'
      : administradorPiso
        ? 'Operación de mi piso'
        : 'Bienvenido a SCLI'
  const descripcion = estudiante
    ? 'Consulte sus clases, laboratorios asignados y asistencia desde las opciones disponibles.'
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
            <Link to="/asistencia">Mi asistencia</Link>
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
  const docente = hasRole(usuario, 'DOCENTE')
  return (
    <DashboardLayout breadcrumb="Inicio">
      {administrador && (
        <>
          <LaboratoriosPanel />
          <MonitoreoPanel />
        </>
      )}
      {docente && usuario?.perfilId && <MiSemana perfilId={usuario.perfilId} />}
      {!administrador && !docente && <InicioPorRol />}
    </DashboardLayout>
  )
}
