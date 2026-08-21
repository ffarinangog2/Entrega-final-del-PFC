import { Navigate, Route, Routes } from 'react-router-dom'
import { AboutPage } from '../pages/AboutPage'
import { LoginPage } from '../pages/LoginPage'
import { MainPage } from '../pages/MainPage'
import { SettingsPage } from '../pages/SettingsPage'
import { ProtectedRoute } from './ProtectedRoute'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/main" element={<MainPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/about" element={<AboutPage />} />
      </Route>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
