import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:layers',
      order: 0,
      title: $t('requirement.menu'),
    },
    name: 'Requirement',
    path: '/requirement',
    children: [
      {
        name: 'RequirementProduct',
        path: 'products',
        component: () => import('#/views/requirement/product/index.vue'),
        meta: {
          icon: 'lucide:package',
          title: $t('requirement.product.menu'),
        },
      },
      {
        name: 'RequirementStory',
        path: 'stories',
        component: () => import('#/views/requirement/story/index.vue'),
        meta: {
          icon: 'lucide:lightbulb',
          title: $t('requirement.story.menu'),
        },
      },
      {
        name: 'RequirementProject',
        path: 'projects',
        component: () => import('#/views/requirement/project/index.vue'),
        meta: {
          icon: 'lucide:folder-kanban',
          title: $t('requirement.project.menu'),
        },
      },
      {
        name: 'RequirementIteration',
        path: 'iterations',
        component: () => import('#/views/requirement/iteration/index.vue'),
        meta: {
          icon: 'lucide:refresh-cw',
          title: $t('requirement.iteration.menu'),
        },
      },
      {
        name: 'RequirementTask',
        path: 'tasks',
        component: () => import('#/views/requirement/task/index.vue'),
        meta: {
          icon: 'lucide:clipboard-list',
          title: $t('requirement.task.menu'),
        },
      },
      {
        name: 'RequirementBoard',
        path: 'board',
        component: () => import('#/views/requirement/board/index.vue'),
        meta: {
          icon: 'lucide:trello',
          title: $t('board.title'),
        },
      },
    ],
  },
];

export default routes;
