import { LaboratoriosPanel } from '../features/laboratorios/LaboratoriosPanel'
import { LogoutButton } from '../components/LogoutButton'

export function MainPage() {
  return (
    <>
      <LogoutButton />
      <LaboratoriosPanel />
    </>
  )
}