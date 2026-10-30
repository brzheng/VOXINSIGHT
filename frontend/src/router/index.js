import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import TopicsView from '../views/TopicsView.vue'
import PracticeView from '../views/PracticeView.vue'
import ReportView from '../views/ReportView.vue'
import HistoryView from '../views/HistoryView.vue'
import ProfileView from '../views/ProfileView.vue'
import LoginView from '../views/LoginView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    { path: '/', name: 'home', component: HomeView },
    { path: '/topics', name: 'topics', component: TopicsView },
    { path: '/practice/:topicId', name: 'practice', component: PracticeView },
    { path: '/report/:reportId', name: 'report', component: ReportView },
    { path: '/history', name: 'history', component: HistoryView },
    { path: '/profile', name: 'profile', component: ProfileView },
  ],
})

router.beforeEach((to) => {
  const saved = localStorage.getItem('voxinsight-token')
  if (to.path === '/login') {
    return saved ? '/' : true
  }
  return saved ? true : '/login'
})

export default router
