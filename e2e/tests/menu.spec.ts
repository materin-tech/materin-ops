import { expect, test } from '@playwright/test';

import { login } from './helpers/login';

test('TC-SYS-003: 菜单渲染且无裸 i18n key', async ({ page }) => {
  await login(page);
  const sidebar = page.locator('aside, [class*="sidebar"]').first();
  const text = (await sidebar.innerText()).toString();
  for (const m of ['需求管理', '系统管理', '开发者中心', '工单管理']) {
    expect(text).toContain(m);
  }
  expect(text).not.toMatch(/\.menu$|\.title$/);
  // 演示/项目/关于菜单已清除
  expect(text).not.toContain('演示');
});

test('TC-SYS-003b: 顶栏搜索框不存在', async ({ page }) => {
  await login(page);
  await expect(page.locator('input[placeholder*="搜索"]')).toHaveCount(0);
});
