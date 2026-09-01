const GATEWAY_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL ?? import.meta.env.VITE_API_URL ?? ''
).replace(/\/$/, '')

type UnauthorizedHandler = () => Promise<boolean>

let unauthorizedHandler: UnauthorizedHandler | null = null
let refreshInFlight: Promise<boolean> | null = null

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export function configureUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  unauthorizedHandler = handler
}

function humanMessage(status: number, backendMessage?: string) {
  if (status === 401) return 'La sesión expiró. Inicie sesión nuevamente.'
  if (status === 403) return 'No tiene permisos para realizar esta acción.'
  if (status === 404) return backendMessage || 'El recurso solicitado no existe.'
  if (status === 409) return backendMessage || 'Existe un conflicto de disponibilidad o estado.'
  if (status === 423) return backendMessage || 'La cuenta estÃ¡ bloqueada temporalmente.'
  if (status === 503) return 'El servicio no estÃ¡ disponible temporalmente. Intente nuevamente.'
  if (status >= 500) return 'Se produjo un error al procesar la solicitud.'
  return backendMessage || 'No se pudo completar la solicitud.'
}

async function errorMessage(response: Response) {
  let backendMessage: string | undefined
  try {
    const body = (await response.json()) as { message?: string }
    if (typeof body.message === 'string' && body.message.trim()) backendMessage = body.message.trim()
  } catch {
    // El Gateway puede devolver errores sin cuerpo JSON.
  }
  return humanMessage(response.status, backendMessage)
}

async function send(path: string, init: RequestInit) {
  const accessToken = sessionStorage.getItem('accessToken')
  return fetch(`${GATEWAY_BASE_URL}${path}`, {
    ...init,
    headers: {
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...init.headers,
    },
  })
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response
  try {
    response = await send(path, init)
  } catch {
    throw new ApiError(503, 'No fue posible conectar con el sistema.')
  }

  if (response.status === 401 && unauthorizedHandler) {
    refreshInFlight ??= unauthorizedHandler().finally(() => {
      refreshInFlight = null
    })
    if (await refreshInFlight) {
      try {
        response = await send(path, init)
      } catch {
        throw new ApiError(503, 'No fue posible conectar con el sistema.')
      }
    }
  }

  if (!response.ok) throw new ApiError(response.status, await errorMessage(response))
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}
