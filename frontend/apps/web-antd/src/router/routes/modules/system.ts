import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:settings',
      order: 10,
      title: $t('system.menu'),
    },
    name: 'System',
    path: '/system',
    children: [
      {
        name: 'SystemUser',
        path: 'users',
        component: () => import('#/views/system/user/index.vue'),
        meta: {
          icon: 'lucide:user',
          title: $t('system.user.menu'),
        },
      },
      {
        name: 'SystemRole',
        path: 'roles',
        component: () => import('#/views/system/role/index.vue'),
        meta: {
          icon: 'lucide:user-round-cog',
          title: $t('system.role.menu'),
        },
      },
      {
        name: 'SystemMenu',
        path: 'menus',
        component: () => import('#/views/system/menu/index.vue'),
        meta: {
          icon: 'lucide:menu',
          title: $t('system.menuPage.menu'),
        },
      },
      {
        name: 'SystemResource',
        path: 'resources',
        component: () => import('#/views/system/resource/index.vue'),
        meta: {
          icon: 'lucide:hard-drive',
          title: $t('resource.title'),
        },
      },
    ],
  },
];

export default routes;
