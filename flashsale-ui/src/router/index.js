import { createRouter, createWebHistory } from 'vue-router'
import SeckillDetail from '../components/SeckillDetail.vue'
import LoginView from '../views/LoginView.vue'
import ProfileView from '../views/ProfileView.vue'

const routes = [
  { path: '/', name: 'Home', component: SeckillDetail },
  { path: '/login', name: 'Login', component: LoginView },
  { path: '/profile', name: 'Profile', component: ProfileView }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router