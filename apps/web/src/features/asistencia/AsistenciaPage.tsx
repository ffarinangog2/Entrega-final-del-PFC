import { useEffect, useMemo, useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
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

function getInitialToken() {
  try {
    return typeof window !== 'undefined'
      ? new URLSearchParams(window.location.search).get('token') ?? ''
      : ''
  } catch {
    return ''
  }
}

export function AsistenciaPage() {
  const { usuario } = useAuth()
  const docente = hasRole(usuario, 'DOCENTE')
  const estudiante = hasRole(usuario, 'ESTUDIANTE')
  const [clases, setClases] = useState<ClaseDocente[]>([])
  const [bloquesDocente, setBloquesDocente] = useState<api.Planificacion[]>([])
  const [horarioEstudiante, setHorarioEstudiante] = useState<api.Planificacion[]>([])
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [materias, setMaterias] = useState<Materia[]>([])
  const [sesion, setSesion] = useState<api.SesionAsistencia | null>(null)
  const [abiertas, setAbiertas] = useState<api.SesionAsistencia[]>([])
  const [registros, setRegistros] = useState<api.RegistroAsistencia[]>([])
  const [participantes, setParticipantes] = useState<api.ParticipanteUso[]>([])
  const [tema, setTema] = useState('')
  const [observacion, setObservacion] = useState('')
  const [tokenInput, setTokenInput] = useState(getInitialToken)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [cargando, setCargando] = useState(true)
  const [registrando, setRegistrando] = useState<string | null>(null)
  const hoy = new Date().toISOString().slice(0, 10)

  useEffect(() => {
    let active = true
    async function cargar() {
      try {
        if (docente) {
          const [reservas, labs, materiasData, bloques] = await Promise.all([
            obtenerReservas().catch(() => []),
            obtenerLaboratorios().catch(() => []),
            obtenerMaterias().catch(() => []),
            api.obtenerClasesDocenteHoy().catch(() => []),
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
            setBloquesDocente(bloques)
          }
        } else if (estudiante) {
          const [sesiones, historial, horario, labs, materiasData] =
            await Promise.all([
              api.listarSesionesAbiertas(),
              api.historialAsistencia().catch(() => []),
              api.obtenerMiHorario().catch(() => []),
              obtenerLaboratorios().catch(() => []),
              obtenerMaterias().catch(() => []),
            ])
          if (active) {
            setAbiertas(sesiones ?? [])
            setRegistros(historial ?? [])
            setHorarioEstudiante(horario ?? [])
            setLaboratorios(labs ?? [])
            setMaterias(materiasData ?? [])
          }
        }
      } catch (cause) {
        if (active) {
          setError(
            cause instanceof Error
              ? cause.message
              : 'No se pudo cargar la asistencia.',
          )
        }
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
      const s = await api.abrirAsistencia(reservaId)
      setSesion(s)
      setTema(s.temaActividad ?? '')
      setObservacion(s.observacionUso ?? '')
      if (typeof api.listarParticipantesUso === 'function') {
        api.listarParticipantesUso(s.id).then(setParticipantes).catch(() => undefined)
      }
      setMensaje('Asistencia habilitada temporalmente para esta clase.')
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No se pudo habilitar la asistencia.',
      )
    }
  }

  async function habilitarBloque(bloqueId: string) {
    setError('')
    setMensaje('')
    try {
      const s = await api.abrirAsistenciaBloque(bloqueId)
      setSesion(s)
      setTema(s.temaActividad ?? '')
      setObservacion(s.observacionUso ?? '')
      if (typeof api.listarParticipantesUso === 'function') {
        api.listarParticipantesUso(s.id).then(setParticipantes).catch(() => undefined)
      }
      setMensaje('Asistencia habilitada para esta clase planificada.')
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
      if (typeof api.listarParticipantesUso === 'function') {
        api.listarParticipantesUso(sesion.id).then(setParticipantes).catch(() => undefined)
      }
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No se pudo actualizar la sesión.',
      )
    }
  }

  async function guardarUso() {
    if (!sesion || !tema.trim()) {
      setError('Indique el tema o actividad desarrollada.')
      return
    }
    try {
      const s = await api.completarUsoLaboratorio(sesion.id, {
        temaActividad: tema.trim(),
        observacionUso: observacion.trim(),
      })
      setSesion(s)
      setMensaje('Registro de uso actualizado.')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se pudo guardar el uso.')
    }
  }

  async function cerrar() {
    if (!sesion || !confirm('¿Cerrar la sesión de asistencia?')) return
    try {
      if (tema.trim() && typeof api.completarUsoLaboratorio === 'function') {
        await api.completarUsoLaboratorio(sesion.id, {
          temaActividad: tema.trim(),
          observacionUso: observacion.trim(),
        }).catch(() => undefined)
      }
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
    if (registrando || registros.some((item) => item.sesionId === sesionId))
      return
    setRegistrando(sesionId)
    setError('')
    try {
      if (tokenInput.trim() && typeof api.registrarAsistencia === 'function') {
        await api.registrarAsistencia(sesionId, tokenInput.trim())
      } else {
        await api.registrarAsistenciaPropia(sesionId)
      }
      setMensaje('Tu presencia fue registrada correctamente.')
      setAbiertas((actual) => actual.filter((item) => item.id !== sesionId))
      setRegistros(await api.historialAsistencia())
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'No se pudo registrar la presencia.',
      )
    } finally {
      setRegistrando(null)
    }
  }

  const qrUrl = sesion?.token
    ? `${typeof window !== 'undefined' ? window.location.origin : ''}/asistencia?sesionId=${encodeURIComponent(
        sesion.id,
      )}&token=${encodeURIComponent(sesion.token)}`
    : ''

  return (
    <DashboardLayout breadcrumb="Asistencia">
      <div className="operations">
        <header>
          <div>
            <h1>
              {docente ? 'Asistencia de mis clases' : 'Registro de laboratorio'}
            </h1>
            <p>
              {docente
                ? 'Seleccione una clase asignada de hoy para habilitar el registro.'
                : 'Registre su presencia cuando exista una actividad habilitada para su carrera.'}
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
                {clases.length === 0 && bloquesDocente.length === 0 ? (
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
                    {bloquesDocente.map((bloque) => (
                      <article className="operations__card" key={bloque.id}>
                        <h3>
                          {materiasPorId.get(bloque.materiaId)?.nombre ??
                            'Clase planificada'}
                        </h3>
                        <p>
                          <strong>
                            {bloque.horaInicio}–{bloque.horaFin}
                          </strong>
                        </p>
                        <p>
                          {labs.get(bloque.laboratorioId)?.nombre ??
                            'Laboratorio asignado'}
                        </p>
                        <button
                          onClick={() => void habilitarBloque(bloque.id)}
                          disabled={sesion?.bloqueId === bloque.id}
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

                {sesion.token && qrUrl && (
                  <div className="usage-qr" style={{ margin: '1rem 0' }}>
                    <QRCodeSVG
                      value={qrUrl}
                      size={160}
                      title="QR temporal de presencia"
                    />
                    <br />
                    <small>QR temporal válido hasta {new Date(sesion.expiraEn).toLocaleTimeString()}</small>
                  </div>
                )}

                <div style={{ margin: '1rem 0' }}>
                  <label>
                    Tema / actividad desarrollada:
                    <input
                      style={{ display: 'block', width: '100%', margin: '0.25rem 0' }}
                      value={tema}
                      placeholder="Tema abordado en la sesión"
                      maxLength={500}
                      onChange={(e) => setTema(e.target.value)}
                    />
                  </label>
                  <label>
                    Observación de uso:
                    <textarea
                      style={{ display: 'block', width: '100%', margin: '0.25rem 0' }}
                      value={observacion}
                      placeholder="Observaciones de uso de equipos o incidencias"
                      maxLength={2000}
                      onChange={(e) => setObservacion(e.target.value)}
                    />
                  </label>
                  {tema.trim() && (
                    <button onClick={() => void guardarUso()}>Guardar tema/observación</button>
                  )}
                </div>

                <div className="operations__actions">
                  <button onClick={() => void actualizar()}>
                    Actualizar asistentes
                  </button>
                  <button className="danger" onClick={() => void cerrar()}>
                    Cerrar sesión
                  </button>
                </div>

                {participantes.length > 0 && (
                  <div style={{ marginTop: '1rem' }}>
                    <h3>Participantes esperados ({participantes.length})</h3>
                    <ul>
                      {participantes.map((p) => (
                        <li key={p.id}>
                          {p.estudiantePerfilId} - <strong>{p.estado}</strong>
                          {p.registradaEn && ` (${new Date(p.registradaEn).toLocaleTimeString()})`}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </section>
            )}

            {estudiante && (
              <section>
                <h2>Actividad de laboratorio disponible</h2>
                {abiertas.length === 0 ? (
                  <p className="operations__empty">
                    No hay registros de laboratorio habilitados en este momento.
                  </p>
                ) : (
                  abiertas.map((item) => (
                    <article className="operations__card" key={item.id}>
                      <h3>
                        {materiasPorId.get(
                          horarioEstudiante.find((b) => b.id === item.bloqueId)
                            ?.materiaId ?? '',
                        )?.nombre ?? 'Registro habilitado'}
                      </h3>
                      {item.bloqueId && (
                        <p>
                          {labs.get(
                            horarioEstudiante.find((b) => b.id === item.bloqueId)
                              ?.laboratorioId ?? '',
                          )?.nombre ?? 'Laboratorio asignado'}
                        </p>
                      )}
                      <p>
                        Disponible hasta{' '}
                        {new Date(item.expiraEn).toLocaleTimeString([], {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                        .
                      </p>
                      {registros.some(
                        (reg) => reg.sesionId === item.id,
                      ) ? (
                        <p>Tu presencia ya fue registrada en esta sesión.</p>
                      ) : (
                        <div>
                          {tokenInput && (
                            <input
                              type="text"
                              value={tokenInput}
                              placeholder="Token temporal QR"
                              onChange={(e) => setTokenInput(e.target.value)}
                              style={{ marginRight: '0.5rem' }}
                            />
                          )}
                          <button
                            disabled={registrando === item.id}
                            onClick={() => void registrar(item.id)}
                          >
                            {registrando === item.id
                              ? 'Registrando...'
                              : 'Registrar mi presencia'}
                          </button>
                        </div>
                      )}
                    </article>
                  ))
                )}
              </section>
            )}

            <div className="operations__table-wrap">
              <h2>
                {docente
                  ? 'Estudiantes registrados'
                  : 'Mi historial de presencia'}
              </h2>
              {registros.length === 0 ? (
                <p className="operations__empty">
                  {docente
                    ? 'No hay registros de asistencia.'
                    : 'Aún no tienes registros de uso.'}
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
