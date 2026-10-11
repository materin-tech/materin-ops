import { expect, test } from '@playwright/test';

import { login } from './helpers/login';

test('TC-SYS-002: 错误密码登录失败', async ({ page }) => {
  await page.goto('/#/auth/login');
  await page.getByPlaceholder('请输入用户名').fill('admin');
  await page.getByPlaceholder('密码').fill('wrong-password');
  const track = page.locator('.relative.flex.h-10');
  const handle = track.locator('div').nth(2);
  await handle.hover();
  await page.mouse.down();
  const box = await track.boundingBox();
  await page.mouse.move(box!.x + box!.width - 12, box!.y + box!.height / 2, { steps: 10 });
  await page.mouse.up();
  await page.getByRole('button', { name: /登录|login/i }).click();
  // 断言未进入系统（错误提示为瞬时 message，这里以 URL 不变为准）
  await page.waitForTimeout(2_000);
  await expect(page).toHaveURL(/auth\/login/);
});

test('TC-SYS-001: admin 登录成功进入工作台', async ({ page }) => {
  await login(page);
  await expect(page).toHaveTitle(/materin-ops/);
});
