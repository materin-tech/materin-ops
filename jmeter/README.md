# JMeter API 自动化/压测（Docker 运行）

## 一键运行（Docker，无需本机安装 JMeter）

```bash
# 1. 下载 JMeter binary（清华镜像，已验证 5.6.3）
curl -L -o /tmp/jmeter.zip https://mirrors.tuna.tsinghua.edu.cn/apache/jmeter/binaries/apache-jmeter-5.6.3.zip
unzip -q -o /tmp/jmeter.zip -d /tmp

# 2. Docker 运行（挂载 binary + 用例目录，host.docker.internal 打宿主 nginx）
cd materin-ops
docker run --rm --add-host=host.docker.internal:host-gateway \
  -v /tmp/apache-jmeter-5.6.3:/jmeter -v "$(pwd)/jmeter":/tests -w /tests \
  docker.m.daocloud.io/library/eclipse-temurin:21-jre \
  /jmeter/bin/jmeter.sh -n -t /tests/materin-ops-test.jmx -l /tests/result.jtl \
  -Jhost=host.docker.internal -Jport=80 -Jthreads=5 -Jloops=5 -Jrampup=3

# 3. 参数说明
#    -Jthreads  并发线程数（默认 1，回归用；压测建议 5~20）
#    -Jloops    每线程循环数（默认 3）
#    -Jhost/-Jport  目标地址（默认 localhost:80；容器网络内可用 materin-ops-backend:8080）

# 4. 生成 HTML 报告
docker run --rm -v /tmp/apache-jmeter-5.6.3:/jmeter -v "$(pwd)/jmeter":/tests -w /tests \
  docker.m.daocloud.io/library/eclipse-temurin:21-jre \
  /jmeter/bin/jmeter.sh -g /tests/result.jtl -o /tests/report
```

## 脚本
| 文件 | 用途 |
|---|---|
| `materin-ops-test.jmx` | 5 个采样器：01 登录→02 工单列表→03 创建工单→04 未读通知→**05 APP 签名调用**（JSR223/Groovy：自动取建 APP + 开放授权接口 + HMAC-SHA256 签名 + 断言 code=0）；线程/循环/目标全部 `-J` 参数化 |
| `smoke.sh` | 冒烟测试 12 项（登录/工单全生命周期/资源/通知/HMAC 签名正反向），`bash smoke.sh`，PASS=12 FAIL=0 为过 |

## 踩坑记录（复用时注意）
1. raw body 的 collectionProp 必须叫 `Arguments.arguments`（不是 HTTPsampler.Arguments.arguments），否则 body 为空 → 后端 500。
2. ThreadGroup 固定循环必须 `continue_forever=false`，否则无限跑。
3. `LoopController.loops` 用 stringProp 可携带 `__P()` 函数，intProp 不行。
4. 容器访问宿主：`--add-host=host.docker.internal:host-gateway`；直连 compose 网络则 `-Jhost=materin-ops-backend -Jport=8080`（无 nginx gzip 干扰）。

## 踩坑记录（续）
5. JSR223 里 `-J` 参数是**属性**：用 `props.get('host')`，不是 `vars.get()`；`${port}` 字面量不会替换。
6. JSR223 Sampler 必须挂在 **ThreadGroup 的 hashTree** 下（挂到某个 sampler 的子 hashTree 会被当无效子元素静默忽略）。
7. Groovy 里 `conn.outputStream.write('...'.getBytes('UTF-8'))` 会报 PosterOutputStream 分派错误，用 `c.outputStream.bytes = payload.getBytes('UTF-8')`。
8. **签名口径**：签名串用后端路径 `/requirement/products`（不带 /api），实际请求 URL 需带 `/api` 网关前缀——两者不能混。

## 最新结果（2026-10-11）
- 功能回归（1×1）：5/5 PASS（含 APP 签名调用）
- 压测（5×5）：125 采样 0 错误率，TPS 44.3/s，Avg 19ms
