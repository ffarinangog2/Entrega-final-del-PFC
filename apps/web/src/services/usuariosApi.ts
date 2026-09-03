const GATEWAY_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(
  /\/$/,
  '',
)
const USUARIOS_BASE_URL = GATEWAY_BASE_URL

export interface Perfil {
  id: string
  identificacion: string
  nombres: string
  apellidos: string
  emailInstitucional: string
  emailPersonal: string | null
  telefono: string | null
  direccion: string | null
  fechaNacimiento: string | null
  fotoUrl: string | null
  activo: boolean
  creadoEn: string
  actualizadoEn: string
}

export interface CrearPerfilRequest {
  identificacion: string
  nombres: string
  apellidos: string
  emailInstitucional: string
  emailPersonal: string
  telefono: string
  direccion: string
  fechaNacimiento: string
}
export interface ActualizarPerfilRequest extends CrearPerfilRequest {
  fotoUrl: string | null
}

export interface ActualizarPerfilPropioRequest {
  emailPersonal: string | null
  telefono: string | null
  direccion: string | null
  fotoUrl: string | null
}
export interface AdministradorInstitucional {
  id: string
  perfilId: string
  codigoAdministrador: string
  cargo: string | null
  pisoId: string | null
  activo: boolean
}

interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
}

export class UsuariosApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'UsuariosApiError'
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = sessionStorage.getItem('accessToken')
  let response: Response

  try {
    response = await fetch(`${USUARIOS_BASE_URL}${path}`, {
      ...init,
      headers: {
        ...(init.body ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init.headers,
      },
    })
  } catch {
    throw new UsuariosApiError(
      503,
      'El servicio de usuarios no está disponible.',
    )
  }

  if (!response.ok) {
    let message = `No se pudo completar la solicitud (${response.status}).`
    try {
      const errorBody = (await response.json()) as { message?: string }
      if (errorBody.message) message = errorBody.message
    } catch {
      // Algunos errores del gateway no incluyen body JSON.
    }
    throw new UsuariosApiError(response.status, message)
  }

  return (await response.json()) as T
}

export async function listarPerfiles(filtro = ''): Promise<Perfil[]> {
  const query = filtro
    ? `?nombre=${encodeURIComponent(filtro)}&size=100`
    : '?size=100'
  return (await request<PageResponse<Perfil>>(`/api/v1/perfiles${query}`))
    .content
}

export function crearPerfil(datos: CrearPerfilRequest): Promise<Perfil> {
  return request<Perfil>('/api/v1/perfiles', {
    method: 'POST',
    body: JSON.stringify(datos),
  })
}
export function obtenerPerfil(id: string): Promise<Perfil> {
  return request<Perfil>(`/api/v1/perfiles/${encodeURIComponent(id)}`)
}
export function actualizarPerfil(
  id: string,
  datos: ActualizarPerfilRequest,
): Promise<Perfil> {
  return request<Perfil>(`/api/v1/perfiles/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(datos),
  })
}
export function cambiarEstadoPerfil(
  id: string,
  activo: boolean,
): Promise<Perfil> {
  return request<Perfil>(`/api/v1/perfiles/${encodeURIComponent(id)}/estado`, {
    method: 'PATCH',
    body: JSON.stringify({ activo }),
  })
}

export function obtenerPerfilPropio(): Promise<Perfil> {
  return request<Perfil>('/api/v1/perfiles/me')
}

export function actualizarPerfilPropio(
  datos: ActualizarPerfilPropioRequest,
): Promise<Perfil> {
  return request<Perfil>('/api/v1/perfiles/me', {
    method: 'PATCH',
    body: JSON.stringify(datos),
  })
}

export async function listarAdministradores(): Promise<AdministradorInstitucional[]> {
  return (await request<PageResponse<AdministradorInstitucional>>('/api/v1/administradores?size=100')).content
}

export function actualizarAdministrador(
  administrador: AdministradorInstitucional,
  pisoId: string | null,
): Promise<AdministradorInstitucional> {
  return request<AdministradorInstitucional>(`/api/v1/administradores/${encodeURIComponent(administrador.id)}`, {
    method: 'PUT',
    body: JSON.stringify({
      perfilId: administrador.perfilId,
      codigoAdministrador: administrador.codigoAdministrador,
      cargo: administrador.cargo,
      pisoId,
      activo: administrador.activo,
    }),
  })
}
