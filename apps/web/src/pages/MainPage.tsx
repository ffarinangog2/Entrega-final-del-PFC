import { DashboardLayout } from '../components/DashboardLayout'
import { LaboratoriosPanel } from '../features/laboratorios/LaboratoriosPanel'
import { MonitoreoPanel } from '../features/monitoreo/MonitoreoPanel'
import { hasAnyPermission, useAuth } from '../auth'

export function MainPage() {
  const { usuario } = useAuth()
  const puedeVerLaboratorios = hasAnyPermission(usuario, ['ACADEMICO_LEER', 'LABORATORIO_LEER'])
  return (
    <DashboardLayout breadcrumb="Laboratorios">
      {puedeVerLaboratorios ? <><LaboratoriosPanel /><MonitoreoPanel /></> : <section className="dashboard-welcome"><h1>Bienvenido a SCLI</h1><p>Use el menú para acceder a las funciones disponibles para su perfil.</p></section>}
    </DashboardLayout>
  )
}
