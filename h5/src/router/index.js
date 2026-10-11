import { createRouter, createWebHashHistory } from 'vue-router';

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/', component: () => import('../views/Workbench.vue') },
  { path: '/ticket', component: () => import('../views/ticket/List.vue') },
  { path: '/ticket/create', component: () => import('../views/ticket/Create.vue') },
  { path: '/ticket/:id', component: () => import('../views/ticket/Detail.vue') },
  { path: '/product', component: () => import('../views/product/List.vue') },
  { path: '/product/edit/:id?', component: () => import('../views/product/Edit.vue') },
  { path: '/product/:id', component: () => import('../views/product/Detail.vue') },
  { path: '/notice', component: () => import('../views/Notice.vue') },
];

const router = createRouter({ history: createWebHashHistory(), routes });

router.beforeEach((to) => {
  if (!to.meta.public && !localStorage.getItem('h5_token')) {
    return '/login';
  }
});

export default router;
