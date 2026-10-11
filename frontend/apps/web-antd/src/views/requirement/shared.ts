import { ref } from 'vue';

import { listSystemUsers } from '#/api/requirement';

import type { ProductModuleNode, SystemUserOption } from '#/api/requirement';

/** 需求状态：0草稿 / 1评审中 / 2已确认 / 3已关闭 */
export const STORY_STATUS = [
  { label: '草稿', value: 0 },
  { label: '评审中', value: 1 },
  { label: '已确认', value: 2 },
  { label: '已关闭', value: 3 },
] as const;

/** 任务/需求优先级：0=P1 ... 3=P4 */
export const PRIORITY = [
  { label: 'P1', value: 0 },
  { label: 'P2', value: 1 },
  { label: 'P3', value: 2 },
  { label: 'P4', value: 3 },
] as const;

/** 项目状态：0归档 / 1进行中 / 2已完结 */
export const PROJECT_STATUS = [
  { label: '归档', value: 0 },
  { label: '进行中', value: 1 },
  { label: '已完结', value: 2 },
] as const;

/** 迭代状态：0未开始 / 1进行中 / 2已结束 */
export const ITERATION_STATUS = [
  { label: '未开始', value: 0 },
  { label: '进行中', value: 1 },
  { label: '已结束', value: 2 },
] as const;

/** 任务状态：0待处理 / 1进行中 / 2已完成 / 3已关闭 */
export const TASK_STATUS = [
  { label: '待处理', value: 0 },
  { label: '进行中', value: 1 },
  { label: '已完成', value: 2 },
  { label: '已关闭', value: 3 },
] as const;

/** 任务类型：0开发 / 1测试 / 2设计 / 3其他 */
export const TASK_TYPE = [
  { label: '开发', value: 0 },
  { label: '测试', value: 1 },
  { label: '设计', value: 2 },
  { label: '其他', value: 3 },
] as const;

export function labelOf(
  options: ReadonlyArray<{ label: string; value: number }>,
  value: null | number | undefined,
): string {
  if (value === null || value === undefined) return '-';
  return options.find((o) => o.value === value)?.label ?? String(value);
}

/** 用户下拉选项（负责人/指派人选择），全局共享一份 */
export function useUserOptions() {
  const userOptions = ref<SystemUserOption[]>([]);
  async function loadUsers() {
    if (userOptions.value.length > 0) return;
    try {
      const res = await listSystemUsers();
      userOptions.value = res.list;
    } catch {
      userOptions.value = [];
    }
  }
  return { loadUsers, userOptions };
}

/** 模块树拍平成可选父节点列表 */
export function flattenModules(
  nodes: ProductModuleNode[],
  depth = 0,
  acc: Array<{ depth: number; id: number; name: string }> = [],
): Array<{ depth: number; id: number; name: string }> {
  for (const n of nodes) {
    acc.push({ depth, id: n.id, name: `${'　'.repeat(depth)}${n.name}` });
    if (n.children?.length) {
      flattenModules(n.children, depth + 1, acc);
    }
  }
  return acc;
}
