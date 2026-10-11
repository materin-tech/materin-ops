import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:life-buoy',
      order: 30,
      title: $t('ticket.menu'),
    },
    name: 'Ticket',
    path: '/ticket',
    children: [
      {
        name: 'TicketCreate',
        path: 'create',
        component: () => import('#/views/ticket/create.vue'),
        meta: {
          icon: 'lucide:flag',
          title: $t('ticket.createTitle'),
        },
      },
      {
        name: 'TicketList',
        path: 'tickets',
        component: () => import('#/views/ticket/index.vue'),
        meta: {
          icon: 'lucide:inbox',
          title: $t('ticket.list'),
        },
      },
      {
        name: 'TicketSla',
        path: 'slas',
        component: () => import('#/views/ticket/sla/index.vue'),
        meta: {
          icon: 'lucide:timer',
          title: $t('ticket.sla.menu'),
        },
      },
    ],
  },
];

export default routes;
