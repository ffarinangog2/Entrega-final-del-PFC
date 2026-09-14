import { FormEvent, useEffect, useState } from 'react'
import { DashboardLayout } from '../components/DashboardLayout'
import {
  actualizarPerfilPropio,
  obtenerPerfilPropio,
  type Perfil,
} from '../services/usuariosApi'
import '../features/operaciones/Operations.css'
export function ProfilePage() {
  const [perfil, setPerfil] = useState<Perfil | null>(null),
    [error, setError] = useState(''),
    [mensaje, setMensaje] = useState(''),
    [loading, setLoading] = useState(true),
    [saving, setSaving] = useState(false)
  useEffect(() => {
    void obtenerPerfilPropio()
      .then(setPerfil)
      .catch((e) =>
        setError(
          e instanceof Error ? e.message : 'No se pudo cargar el perfil.',
        ),
      )
      .finally(() => setLoading(false))
  }, [])
  async function guardar(e: FormEvent) {
    e.preventDefault()
    if (!perfil) return
    setSaving(true)
    try {
      setPerfil(
        await actualizarPerfilPropio({
          emailPersonal: perfil.emailPersonal,
          telefono: perfil.telefono,
          direccion: perfil.direccion,
          fotoUrl: perfil.fotoUrl,
        }),
      )
      setMensaje('Perfil actualizado correctamente.')
    } catch (x) {
      setError(x instanceof Error ? x.message : 'No se pudo guardar.')
    } finally {
      setSaving(false)
    }
  }
  return (
    <DashboardLayout breadcrumb="Mi perfil">
      <div className="operations">
        <header>
          <div>
            <h1>Mi perfil</h1>
            <p>Los datos institucionales son de solo lectura.</p>
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
        {loading ? (
          <p>Cargando perfil…</p>
        ) : (
          perfil && (
            <form className="operations__form" onSubmit={guardar}>
              <label>
                Nombres
                <input
                  value={`${perfil.nombres} ${perfil.apellidos}`}
                  readOnly
                />
              </label>
              <label>
                Email institucional
                <input value={perfil.emailInstitucional} readOnly />
              </label>
              <label>
                Email personal
                <input
                  type="email"
                  value={perfil.emailPersonal ?? ''}
                  onChange={(e) =>
                    setPerfil({
                      ...perfil,
                      emailPersonal: e.target.value || null,
                    })
                  }
                />
              </label>
              <label>
                Teléfono
                <input
                  value={perfil.telefono ?? ''}
                  onChange={(e) =>
                    setPerfil({ ...perfil, telefono: e.target.value || null })
                  }
                />
              </label>
              <label className="operations__wide">
                Dirección
                <input
                  value={perfil.direccion ?? ''}
                  onChange={(e) =>
                    setPerfil({ ...perfil, direccion: e.target.value || null })
                  }
                />
              </label>
              <label className="operations__wide">
                URL de foto
                <input
                  type="url"
                  value={perfil.fotoUrl ?? ''}
                  onChange={(e) =>
                    setPerfil({ ...perfil, fotoUrl: e.target.value || null })
                  }
                />
              </label>
              <button disabled={saving}>
                {saving ? 'Guardando…' : 'Guardar cambios'}
              </button>
            </form>
          )
        )}
      </div>
    </DashboardLayout>
  )
}
