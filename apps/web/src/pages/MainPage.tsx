import { DashboardLayout } from '../components/DashboardLayout'
import { LaboratoriosPanel } from '../features/laboratorios/LaboratoriosPanel'
import { MonitoreoPanel } from '../features/monitoreo/MonitoreoPanel'

export function MainPage() {
  return (
    <DashboardLayout breadcrumb="Laboratorios">
      <LaboratoriosPanel />
      <MonitoreoPanel />
    </DashboardLayout>
  )
}