import { useEffect, useMemo, useState } from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import { hasRole, useAuth } from '../../auth'
import {
  obtenerLaboratorios,
  obtenerMaterias,
  type Laboratorio,
  type Materia,
} from '../../services/academicoApi'
import * as api from '../../services/operationalApi'
import {
  obtenerReservas,
  obtenerSolicitudPorId,
  type Reserva,
  type SolicitudReserva,
} from '../reservas/reservasApi'
import '../operaciones/Operations.css'

type ClaseDocente = { reserva: Reserva; solicitud: SolicitudReserva }

export function AsistenciaPage() {
  const { usuario } = useAuth()
  const docente = hasRole(usuario, 'DOCENTE')
  const estudiante = hasRole(usuario, 'ESTUDIANTE')
  const [clases, setClases] = useState<ClaseDocente[]>([])
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [materias, setMaterias] = useState<Materia[]>([])
  const [sesion, setSesion] = useState<api.SesionAsistencia | null>(null)
  const [abiertas, setAbiertas] = useState<api.SesionAsistencia[]>([])
  const [registros, setRegistros] = useState<api.RegistroAsistencia[]>([])
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [cargando, setCargando] = useState(true)
  const hoy = new Date().toISOString().slice(0, 10)

  useEffect(() => {
    let active = true
    async function cargar() {
      try {
        if (docente) {
          const [reservas, labs, materiasData] = await Promise.all([
            obtenerReservas(),
            obtenerLaboratorios(),
            obtenerMaterias(),
          ])
          const reservasHoy = reservas.filter(
            (item) =>
              item.fechaReserva === hoy &&
              ['PROGRAMADA', 'EN_CURSO'].includes(item.estado),
          )
          const solicitudes = await Promise.all(
            reservasHoy.map((item) => obtenerSolicitudPorId(item.solicitudId)),
          )
          if (active) {
            setClases(
              reservasHoy.map((reserva, index) => ({
                reserva,
                solicitud: solicitudes[index],
              })),
            )
            setLaboratorios(labs)
            setMaterias(materiasData)
          }
        } else if (estudiante) {
          const [sesiones, historial] = await Promise.all([
            api.listarSesionesAbiertas(),
            api.historialAsistencia(),
          ])
          if (active) {
            setAbiertas(sesiones)
            setRegistros(historial)
          }
        }
      } catch (cause) {
        if (active)
          setError(
            cause instanceof Error
              ? cause.message
              : 'No se pudo cargar la asistencia.',
          )
      } finally {
        if (active) setCargando(false)
      }
    }
    void cargar()
    return () => {
      active = false
    }
  }, [docente, estudiante, hoy])

  const labs = useMemo(
    () => new Map(laboratorios.map((item) => [item.id, item])),
    [laboratorios],
  )
  const materiasPorId = useMemo(
    () => new Map(materias.map((item) => [item.id, item])),
    [materias],
  )

  async function habilitar(reservaId: string) {
    setError('')
    setMensaje('')
    try {
      setSesion(await api.abrirAsistencia(reservaId))
      setMensaje('Asistencia habilitada temporalmente para esta clase.')
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No se pudo habilitar la asistencia.',
      )
    }
  }
  async function actualizar() {
    if (!sesion) return
    try {
      setRegistros(await api.listarAsistentes(sesion.id))
      setSesion(await api.consultarAsistencia(sesion.id))
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No se pudo actualizar la sesión.',
      )
    }
  }
  async function cerrar() {
    if (!sesion || !confirm('¿Cerrar la sesión de asistencia?')) return
    try {
      await api.cerrarAsistencia(sesion.id)
      setMensaje('Sesión de asistencia cerrada.')
      await actualizar()
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : 'No se pudo cerrar la sesión.',
      )
    }
  }
  async function registrar(sesionId: string) {
    try {
      await api.registrarAsistenciaPropia(sesionId)
      setMensaje('Asistencia registrada correctamente.')
      setAbiertas((actual) => actual.filter((item) => item.id !== sesionId))
      setRegistros(await api.historialAsistencia())
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No se pudo registrar la asistencia.',
      )
    }
  }

  return (
    <DashboardLayout breadcrumb="Asistencia">
      <div className="operations">
        <header>
          <div>
            <h1>{docente ? 'Asistencia de mis clases' : 'Mi asistencia'}</h1>
            <p>
              {docente
                ? 'Seleccione una clase asignada de hoy para habilitar el registro.'
                : 'Registre su asistencia cuando el docente habilite la clase.'}
            </p>
          </div>
        </header>
        {error && (
          <p role="alert" className="operations__error">
            {error}
          </p>
        )}
        {mensaje && (
          <p role="status" className="operations__success">
            {mensaje}
          </p>
        )}
        {cargando ? (
          <p>Cargando...</p>
        ) : (
          <>
            {docente && (
              <section>
                <h2>Mis clases de hoy</h2>
                {clases.length === 0 ? (
                  <p className="operations__empty">
                    No tiene clases programadas para hoy.
                  </p>
                ) : (
                  <div className="operations__cards">
                    {clases.map(({ reserva, solicitud }) => (
                      <article className="operations__card" key={reserva.id}>
                        <h3>
                          {materiasPorId.get(solicitud.materiaId)?.nombre ??
                            'Clase asignada'}
                        </h3>
                        <p>
                          <strong>
                            {reserva.horaInicio}–{reserva.horaFin}
                          </strong>
                        </p>
                        <p>
                          {labs.get(reserva.laboratorioId)?.nombre ??
                            'Laboratorio asignado'}
                        </p>
                        <button
                          onClick={() => void habilitar(reserva.id)}
                          disabled={sesion?.reservaId === reserva.id}
                        >
                          Habilitar asistencia
                        </button>
                      </article>
                    ))}
                  </div>
                )}
              </section>
            )}
            {docente && sesion && (
              <section className="operations__card">
                <h2>Asistencia habilitada</h2>
                <p>
                  Disponible hasta{' '}
                  {new Date(sesion.expiraEn).toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                  .
                </p>
                <div className="operations__actions">
                  <button onClick={() => void actualizar()}>
                    Actualizar asistentes
                  </button>
                  <button className="danger" onClick={() => void cerrar()}>
                    Cerrar sesión
                  </button>
                </div>
              </section>
            )}
            {estudiante && (
              <section>
                <h2>Mis clases de hoy</h2>
                {abiertas.length === 0 ? (
                  <p className="operations__empty">
                    Asistencia aún no habilitada.
                  </p>
                ) : (
                  abiertas.map((item) => (
                    <article className="operations__card" key={item.id}>
                      <h3>Clase con asistencia habilitada</h3>
                      <p>
                        Disponible hasta{' '}
                        {new Date(item.expiraEn).toLocaleTimeString([], {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                        .
                      </p>
                      <button onClick={() => void registrar(item.id)}>
                        Registrar asistencia
                      </button>
                    </article>
                  ))
                )}
              </section>
            )}
            <div className="operations__table-wrap">
              <h2>{docente ? 'Estudiantes registrados' : 'Mi historial'}</h2>
              {registros.length === 0 ? (
                <p className="operations__empty">
                  No hay registros de asistencia.
                </p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {registros.map((item) => (
                      <tr key={item.id}>
                        <td>{new Date(item.registradaEn).toLocaleString()}</td>
                        <td>
                          <span className="status">{item.estado}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        )}
      </div>
    </DashboardLayout>
  )
}
