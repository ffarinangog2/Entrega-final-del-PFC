<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formulario = reactive({
  username: '',
  password: '',
})

async function iniciarSesion(): Promise<void> {
  if (!formulario.username.trim() || !formulario.password) {
    return
  }

  try {
    await authStore.login({
      username: formulario.username.trim(),
      password: formulario.password,
    })

    await router.replace({
      name: 'dashboard',
    })
  } catch {
    // El store ya establece el mensaje de error.
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <header class="login-header">
        <div class="brand-mark">SCLI</div>

        <div>
          <h1>Control de laboratorios</h1>
          <p>Ingresa con tus credenciales institucionales.</p>
        </div>
      </header>

      <form
        class="login-form"
        @submit.prevent="iniciarSesion"
      >
        <div class="field">
          <label for="username">
            Usuario o correo
          </label>

          <input
            id="username"
            v-model="formulario.username"
            name="username"
            type="text"
            autocomplete="username"
            maxlength="160"
            placeholder="admin o admin@scli.local"
            :disabled="authStore.cargando"
            required
          />
        </div>

        <div class="field">
          <label for="password">
            Contraseña
          </label>

          <input
            id="password"
            v-model="formulario.password"
            name="password"
            type="password"
            autocomplete="current-password"
            maxlength="100"
            placeholder="Ingresa tu contraseña"
            :disabled="authStore.cargando"
            required
          />
        </div>

        <p
          v-if="authStore.error"
          class="error-message"
          role="alert"
        >
          {{ authStore.error }}
        </p>

        <button
          type="submit"
          :disabled="authStore.cargando"
        >
          {{
            authStore.cargando
              ? 'Verificando...'
              : 'Iniciar sesión'
          }}
        </button>
      </form>

      <footer>
        <small>
          Sistema de Control de Laboratorios Informáticos
        </small>
      </footer>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(
      circle at top left,
      rgba(26, 91, 165, 0.16),
      transparent 38%
    ),
    #eef3f8;
}

.login-card {
  width: min(100%, 440px);
  padding: 36px;
  background: #ffffff;
  border: 1px solid #dbe3ec;
  border-radius: 20px;
  box-shadow: 0 20px 50px rgba(23, 43, 77, 0.12);
}

.login-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 30px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: #174f8c;
  color: #ffffff;
  font-weight: 800;
  letter-spacing: 1px;
}

h1 {
  margin: 0 0 6px;
  color: #172b4d;
  font-size: 1.5rem;
}

p {
  margin: 0;
  color: #5e6c84;
  line-height: 1.5;
}

.login-form {
  display: grid;
  gap: 20px;
}

.field {
  display: grid;
  gap: 8px;
}

label {
  color: #253858;
  font-weight: 600;
}

input {
  width: 100%;
  box-sizing: border-box;
  padding: 13px 14px;
  border: 1px solid #c8d2df;
  border-radius: 10px;
  background: #ffffff;
  color: #172b4d;
  font: inherit;
  outline: none;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

input:focus {
  border-color: #2468a9;
  box-shadow: 0 0 0 3px rgba(36, 104, 169, 0.15);
}

input:disabled {
  cursor: not-allowed;
  background: #f4f6f8;
}

button {
  min-height: 48px;
  border: 0;
  border-radius: 10px;
  background: #174f8c;
  color: #ffffff;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  transition:
    transform 0.15s,
    opacity 0.2s;
}

button:hover:not(:disabled) {
  transform: translateY(-1px);
}

button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.error-message {
  padding: 12px 14px;
  border: 1px solid #f1b8b8;
  border-radius: 10px;
  background: #fff1f1;
  color: #a51d1d;
  font-size: 0.92rem;
}

footer {
  margin-top: 28px;
  text-align: center;
  color: #6b778c;
}
</style>