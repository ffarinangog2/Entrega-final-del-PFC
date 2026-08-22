import { DashboardLayout } from '../components/DashboardLayout'
import { LaboratoriosPanel } from '../features/laboratorios/LaboratoriosPanel'

export function MainPage() {
  return (
    <DashboardLayout breadcrumb="Laboratorios">
      <LaboratoriosPanel />
    </DashboardLayout>
  )
}
