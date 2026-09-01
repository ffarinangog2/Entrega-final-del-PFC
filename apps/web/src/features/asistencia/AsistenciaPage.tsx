import { FormEvent, useEffect, useState } from 'react'
import { DashboardLayout } from '../../components/DashboardLayout'
import { hasRole, useAuth } from '../../auth'
import { obtenerReservas, type Reserva } from '../reservas/reservasApi'
import * as api from '../../services/operationalApi'
import '../operaciones/Operations.css'
export function AsistenciaPage() {
  const { usuario } = useAuth()
  const docente = hasRole(usuario, 'DOCENTE')
  const estudiante = hasRole(usuario, 'ESTUDIANTE')
  const [reservas, setReservas] = useState<Reserva[]>([]),
    [sesion, setSesion] = useState<api.SesionAsistencia | null>(null),
    [registros, setRegistros] = useState<api.RegistroAsistencia[]>([]),
    [reservaId, setReservaId] = useState(''),
    [sesionId, setSesionId] = useState(''),
    [token, setToken] = useState(''),
    [error, setError] = useState(''),
    [mensaje, setMensaje] = useState(''),
    [cargando, setCargando] = useState(true)
  useEffect(() => {
    void (async () => {
      try {
        if (docente)
          setReservas(
            (await obtenerReservas()).filter((r) =>
              ['PROGRAMADA', 'EN_CURSO'].includes(r.estado),
            ),
          )
        if (estudiante) setRegistros(await api.historialAsistencia())
      } catch (e) {
        setError(
          e instanceof Error ? e.message : 'No se pudo cargar asistencia.',
        )
      } finally {
        setCargando(false)
      }
    })()
  }, [docente, estudiante])
  async function abrir(e: FormEvent) {
    e.preventDefault()
    try {
      const s = await api.abrirAsistencia(reservaId)
      setSesion(s)
      setSesionId(s.id)
      setToken(s.token ?? '')
      setMensaje(
        'Sesión abierta. Comparta el código temporal de forma segura.',
      )
    } catch (x) {
      setError(x instanceof Error ? x.message : 'No se pudo abrir.')
    }
  }
  async function actualizar() {
    if (!sesionId) return
    try {
      setSesion(await api.consultarAsistencia(sesionId))
      setRegistros(await api.listarAsistentes(sesionId))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo consultar.')
    }
  }
  async function cerrar() {
    if (!sesionId || !confirm('¿Cerrar la sesión de asistencia?')) return
    try {
      await api.cerrarAsistencia(sesionId)
      setMensaje('Sesión cerrada.')
      await actualizar()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo cerrar.')
    }
  }
  async function registrar(e: FormEvent) {
    e.preventDefault()
    try {
      await api.registrarAsistencia(sesionId, token)
      setMensaje('Asistencia registrada correctamente.')
      setRegistros(await api.historialAsistencia())
    } catch (x) {
      setError(x instanceof Error ? x.message : 'No se pudo registrar.')
    }
  }
  return (
    <DashboardLayout breadcrumb="Asistencia">
      <div className="operations">
        <header>
          <div>
            <h1>Asistencia</h1>
            <p>
              {docente
                ? 'Abra y controle sesiones de sus reservas.'
                : 'Registre y consulte exclusivamente su asistencia.'}
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
          <p>Cargando…</p>
        ) : (
          <>
            {docente && (
              <>
                <form className="operations__form" onSubmit={abrir}>
                  <label className="operations__wide">
                    Reserva
                    <select
                      required
                      value={reservaId}
                      onChange={(e) => setReservaId(e.target.value)}
                    >
                      <option value="">Seleccione una reserva</option>
                      {reservas.map((r) => (
                        <option key={r.id} value={r.id}>
                          {r.codigoReserva} — {r.fechaReserva} {r.horaInicio}
                        </option>
                      ))}
                    </select>
                  </label>
                  <button>Abrir sesión</button>
                </form>
                {sesion && (
                  <section className="operations__card">
                    <h2>Sesión {sesion.estado}</h2>
                    <p>Vence: {new Date(sesion.expiraEn).toLocaleString()}</p>
                    {sesion.token && (
                      <>
                        <label>Código temporal</label>
                        <output className="operations__token">
                          {sesion.token}
                        </output>
                      </>
                    )}
                    <div className="operations__actions">
                      <button onClick={() => void actualizar()}>
                        Actualizar asistentes
                      </button>
                      <button className="danger" onClick={() => void cerrar()}>
                        Cerrar
                      </button>
                    </div>
                  </section>
                )}
              </>
            )}
            {estudiante && (
              <form className="operations__form" onSubmit={registrar}>
                <label>
                  ID de sesión
                  <input
                    required
                    value={sesionId}
                    onChange={(e) => setSesionId(e.target.value)}
                    autoComplete="off"
                  />
                </label>
                <label>
                  Código temporal
                  <input
                    required
                    value={token}
                    onChange={(e) => setToken(e.target.value)}
                    autoComplete="one-time-code"
                  />
                </label>
                <button>Registrar mi asistencia</button>
              </form>
            )}
            <div className="operations__table-wrap">
              <h2>{docente ? 'Asistentes' : 'Mi historial'}</h2>
              {registros.length === 0 ? (
                <p className="operations__empty">No hay registros.</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Estado</th>
                      {docente && <th>Estudiante</th>}
                    </tr>
                  </thead>
                  <tbody>
                    {registros.map((r) => (
                      <tr key={r.id}>
                        <td>{new Date(r.registradaEn).toLocaleString()}</td>
                        <td>
                          <span className="status">{r.estado}</span>
                        </td>
                        {docente && <td>{r.estudianteId}</td>}
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
