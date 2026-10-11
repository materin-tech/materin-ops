import type { Page } from '@playwright/test';

/**
 * 登录页登录辅助：填账号密码 + 拖滑块（慢速多步）+ 断言验证通过 + 提交
 */
export async function login(page: Page) {
  await page.goto('/#/auth/login');
  await page.getByPlaceholder('请输入用户名').fill('admin');
  await page.getByPlaceholder('密码').fill('123456');

  await dragSlider(page);

  await page.getByRole('button', { name: /登录|login/i }).click();
  await page.waitForURL(/dashboard/, { timeout: 15_000 });
}

/** 拖动滑块到最右端并等待「验证通过」 */
export async function dragSlider(page: Page) {
  const track = page.locator('.relative.flex.h-10');
  await track.waitFor({ state: 'visible' });
  const handle = track.locator('div[class*="cursor-mo"]').first();
  const box = await track.boundingBox();
  const hBox = await handle.boundingBox();
  if (!box || !hBox) throw new Error('slider not visible');

  const startX = hBox.x + hBox.width / 2;
  const startY = hBox.y + hBox.height / 2;
  const endX = box.x + box.width - 12;

  await page.mouse.move(startX, startY);
  await page.mouse.down();
  // 慢速 30 步拖到最右，模拟真人
  for (let i = 1; i <= 30; i++) {
    await page.mouse.move(startX + ((endX - startX) * i) / 30, startY, { steps: 2 });
    await page.waitForTimeout(25);
  }
  await page.mouse.up();
  await expect_captcha_passed(page);
}

async function expect_captcha_passed(page: Page) {
  await expect_v2(page);
}

// 独立函数避免循环依赖 import
import { expect } from '@playwright/test';
async function expect_v2(page: Page) {
  await expect(page.getByText('验证通过')).toBeVisible({ timeout: 5_000 });
}
