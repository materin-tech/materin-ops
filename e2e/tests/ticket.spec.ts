import { expect, test } from '@playwright/test';

import { login } from './helpers/login';

const STAMP = Date.now();

test('TC-TKT-001/007: 提交工单→列表可见→受理→提交确认', async ({ page }) => {
  await login(page);

  // 独立创建页提交
  await page.goto('/#/ticket/create');
  await page.getByPlaceholder('一句话描述问题').fill(`E2E 工单 ${STAMP}`);
  await page.getByRole('button', { name: '提交工单' }).click();
  await page.waitForURL(/ticket\/tickets/, { timeout: 15_000 });

  // 列表断言
  await expect(page.getByText(`E2E 工单 ${STAMP}`)).toBeVisible();

  // 受理（第一行 = 最新工单）
  await page.getByRole('button', { name: '受 理' }).first().click();
  await expect(page.getByText('处理中').first()).toBeVisible();
});
