import {
  createRouter,
  createWebHistory,
} from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import DashboardView from '@/views/DashboardView.vue'

const router = createRouter({
  history: createWebHistory(
    import.meta.env.BASE_URL,
  ),
  routes: [
    {
      path: '/',
      redirect: {
        name: 'login',
      },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: {
        requiresGuest: true,
      },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: {
        name: 'login',
      },
    },
  ],
})

router.beforeEach((to) => {
  const token = sessionStorage.getItem(
    'scli_access_token',
  )

  if (to.meta.requiresAuth && !token) {
    return {
      name: 'login',
    }
  }

  if (to.meta.requiresGuest && token) {
    return {
      name: 'dashboard',
    }
  }

  return true
})

export default router