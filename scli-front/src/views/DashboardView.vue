<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

async function cerrarSesion(): Promise<void> {
  authStore.logout()

  await router.replace({
    name: 'login',
  })
}
</script>

<template>
  <main class="dashboard">
    <section class="card">
      <div>
        <p class="eyebrow">Sesión iniciada</p>

        <h1>
          Bienvenido, {{ authStore.usuario?.username }}
        </h1>

        <p>
          El login, el JWT y el endpoint protegido
          <code>/api/v1/auth/me</code> funcionan.
        </p>
      </div>

      <div class="details">
        <p>
          <strong>Rol:</strong>
          {{ authStore.usuario?.roles.join(', ') }}
        </p>

        <p>
          <strong>Permisos:</strong>
          {{ authStore.usuario?.permisos.length }}
        </p>
      </div>

      <button @click="cerrarSesion">
        Cerrar sesión local
      </button>
    </section>
  </main>
</template>

<style scoped>
.dashboard {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #eef3f8;
}

.card {
  width: min(100%, 720px);
  padding: 36px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 20px 50px rgba(23, 43, 77, 0.12);
}

.eyebrow {
  color: #174f8c;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

h1 {
  color: #172b4d;
}

.details {
  margin: 24px 0;
  padding: 18px;
  border-radius: 12px;
  background: #f4f7fa;
}

button {
  padding: 12px 18px;
  border: 0;
  border-radius: 10px;
  background: #174f8c;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
}
</style>