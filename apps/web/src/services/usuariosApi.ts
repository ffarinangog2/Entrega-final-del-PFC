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

export interface AsociacionRolInstitucional {
  pisoId: string | null
  carreraId: string | null
}
export interface ContextoAcademicoEstudiante {
  id: string
  estudianteId: string
  carreraId: string
  periodoId: string
  nivel: number
  activo: boolean
  creadoEn: string
}
export interface DocenteResumen { id:string; nombres:string; apellidos:string; codigoDocente:string|null }

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

  if (response.status === 204) return undefined as T
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

export const obtenerMiContextoAcademico = () =>
  request<ContextoAcademicoEstudiante>('/api/v1/estudiantes/mi-contexto')

export const obtenerMisContextosAcademicos = () =>
  request<ContextoAcademicoEstudiante[]>('/api/v1/estudiantes/mis-contextos')
export const obtenerContextosAcademicos = (perfilId: string) =>
  request<ContextoAcademicoEstudiante[]>(`/api/v1/estudiantes/perfil/${encodeURIComponent(perfilId)}/contextos`)
export const asignarContextoAcademico = (perfilId: string, body: { carreraId: string; periodoId: string; nivel: number }) =>
  request<ContextoAcademicoEstudiante>(`/api/v1/estudiantes/perfil/${encodeURIComponent(perfilId)}/contextos`, { method: 'POST', body: JSON.stringify(body) })
export const obtenerDocenteResumen = (id:string) => request<DocenteResumen>(`/api/v1/docentes/${encodeURIComponent(id)}/resumen`)

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

export function actualizarAsociacionRol(
  perfilId: string,
  datos: { rol: string; pisoId: string | null; carreraId: string | null },
): Promise<void> {
  return request<void>(`/api/v1/perfiles/${encodeURIComponent(perfilId)}/asociacion-rol`, {
    method: 'PUT',
    body: JSON.stringify(datos),
  })
}

export function obtenerAsociacionRol(
  perfilId: string,
): Promise<AsociacionRolInstitucional> {
  return request<AsociacionRolInstitucional>(`/api/v1/perfiles/${encodeURIComponent(perfilId)}/asociacion-rol`)
}

interface CredencialInstitucionalRequest {
  username: string
  email: string
  rol: string
  activo?: boolean
  pisoId: string | null
  carreraId: string | null
  periodoId?: string | null
  nivel?: number | null
}

export function crearUsuarioInstitucionalCompleto(datos: CrearPerfilRequest & CredencialInstitucionalRequest & { passwordInicial: string }): Promise<Perfil> {
  const { username, email, passwordInicial, rol, pisoId, carreraId, ...perfil } = datos
  return request<Perfil>('/api/v1/perfiles/administracion-usuarios', {
    method: 'POST',
    body: JSON.stringify({ perfil, username, email, passwordInicial, rol, pisoId, carreraId }),
  })
}

export function actualizarUsuarioInstitucionalCompleto(
  perfilId: string,
  datos: ActualizarPerfilRequest & CredencialInstitucionalRequest & { authId: string; activo: boolean },
): Promise<Perfil> {
  const { authId, username, email, rol, activo, pisoId, carreraId, ...perfil } = datos
  return request<Perfil>(`/api/v1/perfiles/administracion-usuarios/${encodeURIComponent(perfilId)}`, {
    method: 'PUT',
    body: JSON.stringify({ authId, perfil, username, email, rol, activo, pisoId, carreraId }),
  })
}
