import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:terminal',
      order: 20,
      title: $t('developer.menu'),
    },
    name: 'Developer',
    path: '/developer',
    children: [
      {
        name: 'DeveloperApp',
        path: 'apps',
        component: () => import('#/views/developer/app/index.vue'),
        meta: {
          icon: 'lucide:app-window',
          title: $t('developer.app.menu'),
        },
      },
      {
        name: 'DeveloperApi',
        path: 'apis',
        component: () => import('#/views/developer/api/index.vue'),
        meta: {
          icon: 'lucide:braces',
          title: $t('developer.api.menu'),
        },
      },
      {
        name: 'DeveloperAuth',
        path: 'authorizations',
        component: () => import('#/views/developer/auth/index.vue'),
        meta: {
          icon: 'lucide:key-round',
          title: $t('developer.auth.menu'),
        },
      },
      {
        name: 'DeveloperGuide',
        path: 'guide',
        component: () => import('#/views/developer/guide/index.vue'),
        meta: {
          icon: 'lucide:book-open',
          title: '接入指南',
        },
      },
    ],
  },
];

export default routes;
