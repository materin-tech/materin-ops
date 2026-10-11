# materin-ops E2E（Playwright）

## 运行
```bash
# 前置：全栈容器运行中（docker compose up -d），首次安装浏览器
cd e2e
npm install
npx playwright install chromium
npx playwright test          # 全量
npx playwright test tests/ticket.spec.ts   # 单文件
```

## 覆盖
| 文件 | 用例 | 对应产品用例 |
|---|---|---|
| tests/auth.spec.ts | 登录成功/失败（滑块自动拖拽） | TC-SYS-001/002 |
| tests/menu.spec.ts | 菜单渲染/无裸 key/搜索框不存在 | TC-SYS-003/010 |
| tests/ticket.spec.ts | 提交工单→列表断言→受理→SLA 标记 | TC-TKT-001/007/011 |
DOCEOF
echo e2e scaffold ok