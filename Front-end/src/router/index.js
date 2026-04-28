import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import SignupView from '../views/SignupView.vue'
import MainLayout from '../layouts/MainLayout.vue'
import { isAuthenticated } from '@/auth/session'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/signup',
      name: 'signup',
      component: SignupView,
    },

    // 🔥 옛날 경로 강제 리다이렉트 (핵심)
    {
      path: '/dashboard',
      redirect: '/main/dashboard',
    },
    {
      path: '/analysis',
      redirect: '/main/analysis',
    },
    {
      path: '/V2X',
      redirect: '/main/V2X',
    },
    {
      path: '/Recommend',
      redirect: '/main/Recommend',
    },

    {
      path: '/main',
      component: MainLayout,
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/Dashboard.vue'),
        },
        {
          path: 'analysis',
          name: 'analysis',
          component: () => import('../views/Analysis.vue'),
        },
        {
          path: 'V2X',
          name: 'V2X',
          component: () => import('../views/V2X.vue'),
        },
        {
          path: 'v2x',
          redirect: '/main/V2X',
        },
        {
          path: 'Recommend',
          name: 'Recommend',
          component: () => import('../views/RecommendView.vue'),
        },
        {
          path: 'recommend',
          redirect: '/main/Recommend',
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  // 🔒 로그인 안 했으면 main 접근 차단
  if (to.path.startsWith('/main') && !isAuthenticated()) {
    return '/'
  }

  // 🔁 로그인 되어있으면 로그인 페이지 못 가게
  if ((to.path === '/' || to.path === '/signup') && isAuthenticated()) {
    return '/main/dashboard'
  }

  return true
})

export default router