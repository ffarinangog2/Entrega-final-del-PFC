import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'

export function LogoutButton() {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [showConfirm, setShowConfirm] = useState(false)

  function handleLogout() {
    setShowConfirm(false)
    void logout().catch(() => undefined)
    navigate('/login', { replace: true })
  }

  return (
    <>
      <button className="logout-button" type="button" onClick={() => setShowConfirm(true)}>
        <span aria-hidden="true">↪</span>
        Cerrar sesión
      </button>

      {showConfirm && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="logout-dialog-title"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
          }}
        >
          <div
            style={{
              backgroundColor: '#fff',
              padding: '24px',
              borderRadius: '8px',
              maxWidth: '400px',
              width: '90%',
              boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
              color: '#1f2937',
            }}
          >
            <h3 id="logout-dialog-title" style={{ margin: '0 0 12px 0', fontSize: '1.25rem', fontWeight: 600 }}>
              Cerrar sesión
            </h3>
            <p style={{ margin: '0 0 20px 0', color: '#4b5563', fontSize: '0.95rem' }}>
              ¿Deseas cerrar tu sesión actual en la plataforma web?
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
              <button
                type="button"
                onClick={() => setShowConfirm(false)}
                style={{
                  padding: '8px 16px',
                  borderRadius: '6px',
                  border: '1px solid #d1d5db',
                  backgroundColor: '#fff',
                  cursor: 'pointer',
                  fontWeight: 500,
                }}
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleLogout}
                style={{
                  padding: '8px 16px',
                  borderRadius: '6px',
                  border: 'none',
                  backgroundColor: '#dc2626',
                  color: '#fff',
                  cursor: 'pointer',
                  fontWeight: 500,
                }}
              >
                Cerrar sesión
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
