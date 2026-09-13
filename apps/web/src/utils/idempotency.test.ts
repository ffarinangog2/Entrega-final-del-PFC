import { describe, expect, it } from 'vitest'
import { generarIdempotencyKey } from './idempotency'

describe('generarIdempotencyKey', () => {
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

  it('genera un identificador con formato UUID v4 válido', () => {
    const key = generarIdempotencyKey()
    expect(key).toMatch(uuidRegex)
  })

  it('genera claves diferentes en llamadas sucesivas', () => {
    const key1 = generarIdempotencyKey()
    const key2 = generarIdempotencyKey()
    expect(key1).not.toBe(key2)
  })

  it('funciona correctamente si crypto.randomUUID no está disponible (simulaci\u00f3n HTTP en VM)', () => {
    const originalRandomUUID = globalThis.crypto?.randomUUID
    try {
      if (globalThis.crypto) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        delete (globalThis.crypto as any).randomUUID
      }
      const key = generarIdempotencyKey()
      expect(key).toMatch(uuidRegex)
    } finally {
      if (originalRandomUUID && globalThis.crypto) {
        globalThis.crypto.randomUUID = originalRandomUUID
      }
    }
  })

  it('funciona incluso sin crypto.getRandomValues con fallback aleatorio seguro', () => {
    const originalCrypto = globalThis.crypto
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      delete (globalThis as any).crypto
      const key = generarIdempotencyKey()
      expect(key).toMatch(uuidRegex)
    } finally {
      globalThis.crypto = originalCrypto
    }
  })
})
