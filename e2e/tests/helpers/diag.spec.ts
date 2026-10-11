import { test } from '@playwright/test';

test('slider diag', async ({ page }) => {
  await page.goto('/#/auth/login');
  const count = await page.locator('.relative.flex.h-10').count();
  console.log('DIAG track count:', count);
  const track = page.locator('.relative.flex.h-10').first();
  const kids = await track.locator('div').count();
  console.log('DIAG children:', kids);
  const hBox = await track.locator('div').nth(2).boundingBox();
  console.log('DIAG handle box:', JSON.stringify(hBox));
  const box = await track.boundingBox();
  console.log('DIAG track box:', JSON.stringify(box));
});
