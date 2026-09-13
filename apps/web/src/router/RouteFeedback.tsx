import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import './RouteFeedback.css'

export function AccessDeniedPage() {
  return <RouteStatusPage title="No tienes permiso" message="Tu cuenta no está autorizada para utilizar esta sección." />
}

export function NotFoundPage() {
  return <RouteStatusPage title="Página no encontrada" message="La dirección solicitada no existe o ya no está disponible." />
}

function RouteStatusPage({ title, message }: { title: string; message: string }) {
  return <main className="route-feedback"><section className="route-feedback__card">
    <h1>{title}</h1><p>{message}</p><Link to="/main">Volver al inicio</Link>
  </section></main>
}

class ErrorBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false }
  static getDerivedStateFromError() { return { failed: true } }
  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Error no controlado en una ruta', error, info)
  }
  render() {
    if (this.state.failed) return <RouteStatusPage title="No se pudo mostrar esta página" message="Ocurrió un error inesperado. Puedes volver al inicio y continuar navegando." />
    return this.props.children
  }
}

export function RouteErrorBoundary({ children }: { children: ReactNode }) {
  const location = useLocation()
  return <ErrorBoundary key={location.key}>{children}</ErrorBoundary>
}
