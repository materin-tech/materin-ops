# materin-ops 测试计划与用例总纲

> 配套文档：`docs/product-spec.md`（产品功能表述）
> 分层：产品功能用例（本文档 §3）→ 后端单测（JUnit5+Mockito）→ 前端单测（Vitest）→ E2E（Playwright）→ API 自动化（JMeter）

## 1. 测试框架与工具链

| 层 | 工具 | 位置 | 运行方式 |
|---|---|---|---|
| 后端单元测试 | JUnit 5 + Mockito + AssertJ（spring-boot-starter-test 自带） | `backend/src/test/java` | `mvn test -pl components/ticket -am` |
| 后端集成验证 | curl 冒烟脚本（容器验收） | `jmeter/smoke.sh` | `bash jmeter/smoke.sh` |
| 前端单元测试 | Vitest（vben 内置 vitest.config.ts） | `frontend/apps/web-antd/src/**/__tests__` | `pnpm vitest run` |
| E2E 测试 | Playwright（Chromium，复用 ECC e2e-runner 约定） | `e2e/` | `cd e2e && npx playwright test` |
| API 自动化/压测 | Apache JMeter 5.6 | `jmeter/materin-ops-test.jmx` | `jmeter -n -t jmeter/materin-ops-test.jmx -l result.jtl` |
| 冒烟测试 | Chrome DevTools MCP / e2e-runner agent | 会话内 | 人工触发 |

## 2. 环境要求

- E2E / JMeter / 冒烟：全栈容器运行中（`docker compose up -d`），前端 http://localhost
- 后端单测：无需数据库（Mockito mock mapper）
- JMeter 登录鉴权：HTTP 信息管理器 + 正则/JSON 提取器取 `data.accessToken`，后续请求 `Authorization: Bearer ${token}`

## 3. 产品功能测试用例矩阵

### TC-SYS 系统管理
| 编号 | 用例 | 步骤 | 预期 |
|---|---|---|---|
| TC-SYS-001 | 登录成功 | 输入 admin/123456 + 滑块 | 进入工作台 |
| TC-SYS-002 | 登录失败 | 错误密码 | 提示错误，不进入系统 |
| TC-SYS-003 | 菜单渲染 | 检查侧边菜单 | 概览/需求管理/系统管理/开发者中心/工单管理，无演示菜单，中文正常（无裸 key） |
| TC-SYS-004 | 用户 CRUD | 新增 user1/绑定 ops 角色/编辑昵称/重置密码/禁用/删除 | 每步成功且列表刷新 |
| TC-SYS-005 | 角色-菜单授权 | 新角色勾选部分菜单保存 | 重新打开勾选状态回显 |
| TC-SYS-006 | 按钮权限码 | ops 角色用户登录 | 仅见授权按钮；/auth/codes 返回字面码 |
| TC-SYS-007 | 资源上传下载 | 上传 txt → 下载 → 内容一致 | 内容一致 |
| TC-SYS-008 | 资源删除 | 删除后列表消失，再下载 404 | 通过 |
| TC-SYS-009 | 个人中心路由 | 头像下拉 → 个人中心 | 进入 /profile 不 404 |
| TC-SYS-010 | 通知铃铛 | 广播通知给 admin → 30s 内铃铛蓝点 | 未读数 +1 |

### TC-REQ 需求管理
| 编号 | 用例 | 步骤 | 预期 |
|---|---|---|---|
| TC-REQ-001 | 产品 CRUD | 新增产品/模块树两级/成员授权 owner/dev | 模块树回显，成员保存回显 |
| TC-REQ-002 | 产品删除保护 | 产品下有需求时删除 | 400「产品下仍有需求，无法删除」 |
| TC-REQ-003 | 需求状态机 | 草稿→提交评审→通过→关闭；评审中打回 | 每步成功；非法迁移 400 |
| TC-REQ-004 | 需求富文本 | 描述插入加粗/图片（上传资源中心） | 保存回显 HTML，图片 URL 可访问 |
| TC-REQ-005 | 项目-产品绑定 | 项目勾选多个产品保存 | 详情回显 productNames |
| TC-REQ-006 | 迭代结束校验 | 迭代下有未完成任务时点结束 | 400 拒绝；完成后可结束并回填 actual_end |
| TC-REQ-007 | 任务状态流转 | 待处理→进行中→已完成→已关闭；1→0 退回 | 合法通过、非法 400 |
| TC-REQ-008 | 看板拖拽 | 拖卡片 待处理→进行中 | 状态变更成功，非法迁移回滚 |
| TC-REQ-009 | 数据权限 | 用户 A 仅授权产品 P1，查需求列表 | 仅见 P1 的需求；越权查 P2 详情 403 |
| TC-REQ-010 | i18n 切换 | 切 en-US | 菜单/页面全英文，无裸 key |

### TC-TKT 工单系统
| 编号 | 用例 | 步骤 | 预期 |
|---|---|---|---|
| TC-TKT-001 | 提交工单（入口） | 侧边菜单→提交工单页，富文本填写，提交 | 跳转列表，新单待受理 |
| TC-TKT-002 | 工单号 | 同日连续创建 | TK-yyyyMMdd-0001 递增；删除后重建号不撞 |
| TC-TKT-003 | SLA 匹配 | 创建 P1 工单 | responseDeadline=+30min, resolveDeadline=+4h |
| TC-TKT-004 | SLA 重算 | 编辑 P2→P1 | deadline 重算并留痕 |
| TC-TKT-005 | 创建即委派 | 创建时选处理人 | assigneeId 落库 |
| TC-TKT-006 | 受理计时 | 受理待受理单 | first_response_at 写入，状态处理中 |
| TC-TKT-007 | 流转全链路 | 受理→提交确认→确认解决→关闭 | 每步 code=0，流水完整 |
| TC-TKT-008 | 非法流转 | 待受理直接→已解决 | 400「不允许的状态变更」 |
| TC-TKT-009 | 编辑限制 | 编辑已解决工单 | 400 |
| TC-TKT-010 | 删除清理 | 删除工单 | 逻辑删除+流水清理；查询 404 |
| TC-TKT-011 | SLA 超期标红 | P1 工单超过 30min 未受理 | 列表 SLA 列红色「SLA 超期」 |
| TC-TKT-012 | 按钮权限 | 无 ticket:handle 用户 | 不显示受理/委派按钮 |

### TC-DEV 开发者中心
| 编号 | 用例 | 步骤 | 预期 |
|---|---|---|---|
| TC-DEV-001 | 同步接口 | 点同步（knife4j） | created/updated 计数返回 |
| TC-DEV-002 | 应用创建 | 新建应用 | AppKey ak_ 前缀、Secret sk_ 前缀 |
| TC-DEV-003 | Secret 重置 | 重置后旧签名调用 | 401 |
| TC-DEV-004 | 授权勾选 | 授权 GET /requirement/products 并开放 | APP 签名调用 code=0 |
| TC-DEV-005 | 未授权拒绝 | APP 调未授权接口 | 403 |
| TC-DEV-006 | 错误签名 | 篡改签名 | 401 签名校验失败 |
| TC-DEV-007 | 重放防护 | 同 nonce 重放 | 401 nonce 已被使用 |
| TC-DEV-008 | 时间窗 | 时间戳 -10min | 401 签名已过期 |
| TC-DEV-009 | 接入指南 | 打开接入指南页 | 流程/示例代码渲染 |

### TC-NTC 通知中心
| 编号 | 用例 | 步骤 | 预期 |
|---|---|---|---|
| TC-NTC-001 | 广播发送 | POST /notice/send userIds=[1] | 收件箱出现，未读+1 |
| TC-NTC-002 | 已读管理 | 单条已读/全部已读/删除 | 未读数相应变化 |
| TC-NTC-003 | 越权 | 用户 A 操作用户 B 的通知 | 403 |

## 4. 自动化映射

- TC-TKT-002/003/004/007/008 → 后端单测 `TicketServiceTest`（Mockito）
- TC-REQ-007 → 后端单测 `StoryServiceTest`（状态机）
- TC-SYS-003/010 → Playwright `menu.spec.ts` / `notice.spec.ts`
- TC-TKT-001/007/008 → Playwright `ticket.spec.ts`
- TC-SYS-001 → Playwright `auth.spec.ts`
- TC-DEV-004~008 → JMeter「开放 API 签名调用」事务控制器 + 冒烟脚本 `jmeter/smoke.sh`
- TC-SYS-007 → JMeter 上传下载采样器
- 全部 TC-* → `jmeter/materin-ops-test.jmx` 覆盖核心 API 正反向断言
