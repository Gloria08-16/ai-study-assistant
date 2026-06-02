import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '@/views/ChatView.vue'
import KnowledgeView from '@/views/KnowledgeView.vue'

const routes = [
    {
        path: '/',
        redirect: '/chat'
    },
    {
        path: '/chat',
        name: 'Chat',
        component: ChatView,
        meta: { title: 'AI伴学' }
    },
    {
        path: '/knowledge',
        name: 'Knowledge',
        component: KnowledgeView,
        meta: { title: '知识库' }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
