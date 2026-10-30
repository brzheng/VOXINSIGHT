import { createRouter, createWebHistory } from 'vue-router'
import { token } from './api'
import Shell from './views/Shell.vue'
import LoginView from './views/LoginView.vue'
import HomeView from './views/HomeView.vue'
import TopicsView from './views/TopicsView.vue'
import PracticeView from './views/PracticeView.vue'
import ReportView from './views/ReportView.vue'
import HistoryView from './views/HistoryView.vue'
import ProfileView from './views/ProfileView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/',
      component: Shell,
      children: [
        { path: '', name: 'home', component: HomeView },
        { path: 'topics', name: 'topics', component: TopicsView },
        { path: 'practice/:topicId', name: 'practice', component: PracticeView },
        { path: 'report/:reportId', name: 'report', component: ReportView },
        { path: 'history', name: 'history', component: HistoryView },
        { path: 'profile', name: 'profile', component: ProfileView },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const saved = token()
  if (to.path === '/login') return saved ? '/' : true
  return saved ? true : '/login'
})

export default router
