<script lang="ts" setup>
// Trello 风格任务看板：按状态分列，拖拽流转
import type { Task } from '#/api/requirement';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { Select, Tag } from 'ant-design-vue';
import draggable from 'vuedraggable';

import { changeTaskStatus, pageIterations, pageProjects, pageTasks } from '#/api/requirement';
import { $t } from '#/locales';
import { PRIORITY, TASK_TYPE, labelOf } from '../shared';

const projects = ref<Array<{ id: number; name: string }>>([]);
const iterations = ref<Array<{ id: number; name: string }>>([]);
const projectId = ref<number>();
const iterationId = ref<number>();
const loading = ref(false);

interface Column {
  key: number;
  title: string;
  color: string;
  tasks: Task[];
}

const columns = ref<Column[]>([
  { color: '#64748b', key: 0, tasks: [], title: $t('board.colPending') },
  { color: '#3b82f6', key: 1, tasks: [], title: $t('board.colProcessing') },
  { color: '#f59e0b', key: 2, tasks: [], title: $t('board.colConfirming') },
  { color: '#10b981', key: 3, tasks: [], title: $t('board.colDone') },
]);

async function load() {
  loading.value = true;
  try {
    const res = await pageTasks({
      iterationId: iterationId.value,
      page: 1,
      pageSize: 500,
      projectId: projectId.value,
    });
    const byStatus = new Map<number, Task[]>();
    for (const col of columns.value) byStatus.set(col.key, []);
    for (const t of res.list) {
      byStatus.get(t.status)?.push(t);
    }
    columns.value = columns.value.map((c) => ({ ...c, tasks: byStatus.get(c.key) ?? [] }));
  } finally {
    loading.value = false;
  }
}

async function onProjectChange(pid?: number) {
  iterationId.value = undefined;
  iterations.value = [];
  if (pid) {
    const res = await pageIterations({ page: 1, pageSize: 100, projectId: pid });
    iterations.value = res.list;
  }
  await load();
}

/** 拖拽落列：跨列移动时流转状态 */
async function onDrop(column: Column, e: any) {
  const moved = e.added?.element as Task | undefined;
  if (!moved || moved.status === column.key) return;
  try {
    await changeTaskStatus(moved.id, column.key);
  } catch {
    // 流转被拒（如非法迁移）时回滚重新加载
  }
  await load();
}

const totalTasks = computed(() => columns.value.reduce((s, c) => s + c.tasks.length, 0));

function cardAvatar(t: Task) {
  const name = t.assigneeName ?? '?';
  return name.slice(0, 1).toUpperCase();
}

const AVATAR_COLORS = ['#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6', '#f59e0b', '#ef4444'];
function avatarColor(t: Task) {
  return AVATAR_COLORS[(t.assigneeId ?? 0) % AVATAR_COLORS.length];
}

onMounted(async () => {
  projects.value = (await pageProjects({ page: 1, pageSize: 100 })).list;
  await load();
});
</script>

<template>
  <Page :title="$t('board.title')" :description="$t('board.desc')">
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <Select
        v-model:value="projectId"
        :placeholder="$t('board.projectPh')"
        style="width: 200px"
        allow-clear
        :options="projects.map((p) => ({ label: p.name, value: p.id }))"
        @change="(v: any) => onProjectChange(v)"
      />
      <Select
        v-model:value="iterationId"
        :placeholder="$t('board.iterationPh')"
        style="width: 180px"
        allow-clear
        :options="iterations.map((i) => ({ label: i.name, value: i.id }))"
        @change="load"
      />
      <div class="flex-1"></div>
      <span class="text-gray-400">{{ totalTasks }} {{ $t('board.tasks') }}</span>
    </div>

    <div class="board-scroll flex gap-4 overflow-x-auto pb-4">
      <div
        v-for="col in columns"
        :key="col.key"
        class="board-column flex w-[300px] flex-none flex-col rounded-xl border bg-gray-50 dark:bg-gray-900/60"
      >
        <div
          class="flex items-center gap-2 rounded-t-xl px-4 py-3 text-sm font-semibold text-white"
          :style="{ background: `linear-gradient(135deg, ${col.color}, ${col.color}cc)` }"
        >
          <span>{{ col.title }}</span>
          <span class="ml-auto rounded-full bg-white/25 px-2 py-0.5 text-xs">
            {{ col.tasks.length }}
          </span>
        </div>
        <div class="flex-1 overflow-y-auto p-3">
          <draggable
            :list="col.tasks"
            item-key="id"
            group="tasks"
            ghost-class="card-ghost"
            drag-class="card-drag"
            animation="200"
            class="flex min-h-[120px] flex-col gap-3"
            @change="(e: any) => onDrop(col, e)"
          >
            <template #item="{ element: t }">
              <div
                class="group cursor-pointer rounded-lg border bg-white p-3 shadow-sm transition hover:shadow-md dark:bg-gray-800"
              >
                <div
                  class="mb-2 h-1 w-10 rounded-full"
                  :style="{ background: ['#ef4444', '#f59e0b', '#3b82f6', '#94a3b8'][t.priority] }"
                ></div>
                <div class="mb-2 text-sm font-medium leading-5">{{ t.title }}</div>
                <div v-if="t.storyTitle" class="mb-2 truncate text-xs text-gray-400">
                  {{ t.storyTitle }}
                </div>
                <div class="flex items-center gap-2">
                  <Tag class="!m-0" :color="['red', 'orange', 'blue', 'default'][t.priority]">
                    {{ labelOf(PRIORITY, t.priority) }}
                  </Tag>
                  <Tag class="!m-0" color="default">{{ labelOf(TASK_TYPE, t.type) }}</Tag>
                  <span class="ml-auto flex size-6 items-center justify-center rounded-full text-xs text-white"
                    :style="{ background: avatarColor(t) }">
                    {{ cardAvatar(t) }}
                  </span>
                </div>
              </div>
            </template>
          </draggable>
        </div>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.board-column {
  max-height: calc(100vh - 220px);
}

.card-ghost {
  opacity: 0.4;
  transform: rotate(2deg);
}

.card-drag {
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.18);
  transform: rotate(3deg);
}
</style>
