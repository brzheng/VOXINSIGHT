import { createRouter, createWebHistory } from 'vue-router'
import { token } from './api'
import Shell from './views/Shell.vue'
import LoginView from './views/LoginView.vue'
import OverviewView from './views/OverviewView.vue'
import UsersView from './views/UsersView.vue'
import TopicsView from './views/TopicsView.vue'
import TopicFormView from './views/TopicFormView.vue'
import SessionsView from './views/SessionsView.vue'
import FeedbackView from './views/FeedbackView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    {
      path: '/',
      component: Shell,
      children: [
        { path: '', component: OverviewView },
        { path: 'users', component: UsersView },
        { path: 'topics', component: TopicsView },
        { path: 'topics/new', component: TopicFormView },
        { path: 'topics/:id', component: TopicFormView },
        { path: 'sessions', component: SessionsView },
        { path: 'feedbacks', component: FeedbackView },
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
