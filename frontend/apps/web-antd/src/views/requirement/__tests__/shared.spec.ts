import { describe, expect, it } from 'vitest';

import { PRIORITY, STORY_STATUS, TASK_STATUS, TASK_TYPE, flattenModules, labelOf } from '../shared';

describe('labelOf', () => {
  it('返回对应标签', () => {
    expect(labelOf(PRIORITY, 0)).toBe('P1');
    expect(labelOf(STORY_STATUS, 2)).toBe('已确认');
    expect(labelOf(TASK_TYPE, 1)).toBe('测试');
  });

  it('未知值回退字符串', () => {
    expect(labelOf(TASK_STATUS, 99)).toBe('99');
  });

  it('空值返回 -', () => {
    expect(labelOf(PRIORITY, null)).toBe('-');
    expect(labelOf(PRIORITY, undefined)).toBe('-');
  });
});

describe('flattenModules', () => {
  it('拍平树并带缩进层级', () => {
    const tree = [
      {
        children: [
          { children: [], id: 2, name: '子模块', parentId: 1, productId: 1, sort: 0 },
        ],
        id: 1,
        name: '根模块',
        parentId: 0,
        productId: 1,
        sort: 0,
      },
    ];
    const flat = flattenModules(tree as any);
    expect(flat).toHaveLength(2);
    expect(flat[0]?.name).toBe('根模块');
    expect(flat[1]?.name).toContain('子模块');
    expect(flat[1]?.depth).toBe(1);
  });

  it('空树返回空数组', () => {
    expect(flattenModules([])).toEqual([]);
  });
});
