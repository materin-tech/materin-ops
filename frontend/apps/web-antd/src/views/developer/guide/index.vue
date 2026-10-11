<script lang="ts" setup>
import { ref } from 'vue';

import { Page } from '@vben/common-ui';

import { Card, Tabs, TabPane, Typography } from 'ant-design-vue';

import { $t } from '#/locales';

const activeKey = ref('flow');

const CODE_CURL = `# 1. 准备签名参数（AppKey/AppSecret 从「开发者中心-应用管理」获取）
APP_KEY="ak_xxxxxxxx"
APP_SECRET="sk_xxxxxxxx"
METHOD="GET"
PATH="/requirement/products"        # 签名用后端路径，不带网关前缀 /api
TS=$(date +%s%3N)                   # 毫秒时间戳，允许 ±5 分钟偏差
NONCE=$(uuidgen)                    # 一次性随机串，10 分钟内不可重复
BODY=""                             # 请求体原文；无 body 传空串
BODY_HASH=$(printf '%s' "$BODY" | openssl dgst -sha256 -hex | sed 's/^.* //')

# 2. 计算签名：HMAC-SHA256(AppSecret, 六行签名串)
SIGN_STRING=$(printf '%s\n%s\n%s\n%s\n%s\n%s' "$APP_KEY" "$METHOD" "$PATH" "$TS" "$NONCE" "$BODY_HASH")
SIGNATURE=$(printf '%s' "$SIGN_STRING" | openssl dgst -sha256 -hmac "$APP_SECRET" -hex | sed 's/^.* //')

# 3. 携带四个请求头调用
curl -s "https://your-host/api\${PATH}" \\
  -H "X-App-Key: $APP_KEY" \\
  -H "X-Timestamp: $TS" \\
  -H "X-Nonce: $NONCE" \\
  -H "X-Signature: $SIGNATURE"`;

const CODE_PY = `import hashlib, hmac, time, uuid, requests

BASE = "https://your-host/api"
APP_KEY = "ak_xxxxxxxx"
APP_SECRET = "sk_xxxxxxxx"

def call(method: str, path: str, body: str = ""):
    ts = str(int(time.time() * 1000))
    nonce = str(uuid.uuid4())
    body_hash = hashlib.sha256(body.encode()).hexdigest()
    sign_string = "\\n".join([APP_KEY, method.upper(), path, ts, nonce, body_hash])
    signature = hmac.new(APP_SECRET.encode(), sign_string.encode(), hashlib.sha256).hexdigest()
    return requests.request(
        method, BASE + path,
        headers={
            "X-App-Key": APP_KEY,
            "X-Timestamp": ts,
            "X-Nonce": nonce,
            "X-Signature": signature,
        },
        data=body or None,
    )

resp = call("GET", "/requirement/products")
print(resp.json())  # {"code": 0, "message": "success", "data": [...]}`;

const CODE_JAVA = `// Java 21 + 标准库
String appKey = "ak_xxxxxxxx";
String appSecret = "sk_xxxxxxxx";
String method = "GET";
String path = "/requirement/products";
String ts = String.valueOf(System.currentTimeMillis());
String nonce = UUID.randomUUID().toString();
String bodyHash = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest("".getBytes(StandardCharsets.UTF_8)));

String signString = String.join("\\n", appKey, method, path, ts, nonce, bodyHash);
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
String signature = HexFormat.of().formatHex(mac.doFinal(signString.getBytes(StandardCharsets.UTF_8)));

HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create("https://your-host/api" + path))
        .header("X-App-Key", appKey)
        .header("X-Timestamp", ts)
        .header("X-Nonce", nonce)
        .header("X-Signature", signature)
        .GET().build();`;

</script>

<template>
  <Page :title="$t('guide.title')" :description="$t('guide.subtitle')">
    <Card>
      <Tabs v-model:activeKey="activeKey">
        <TabPane key="flow" :tab="$t('guide.tabs.flow')">
          <div class="flex flex-col gap-4 leading-7">
            <Typography.Title :level="4">{{ $t('guide.flow.title') }}</Typography.Title>
            <ol class="list-decimal pl-6">
              <li>{{ $t('guide.flow.step1') }}</li>
              <li>{{ $t('guide.flow.step2') }}</li>
              <li>{{ $t('guide.flow.step3') }}</li>
              <li>{{ $t('guide.flow.step4') }}</li>
              <li>{{ $t('guide.flow.step5') }}</li>
            </ol>
            <Typography.Title :level="4">{{ $t('guide.flow.headersTitle') }}</Typography.Title>
            <table class="w-full border text-sm">
              <thead class="bg-gray-100 dark:bg-gray-800">
                <tr>
                  <th class="border p-2 text-left">Header</th>
                  <th class="border p-2 text-left">{{ $t('guide.flow.headerDesc') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr><td class="border p-2">X-App-Key</td><td class="border p-2">{{ $t('guide.flow.h1') }}</td></tr>
                <tr><td class="border p-2">X-Timestamp</td><td class="border p-2">{{ $t('guide.flow.h2') }}</td></tr>
                <tr><td class="border p-2">X-Nonce</td><td class="border p-2">{{ $t('guide.flow.h3') }}</td></tr>
                <tr><td class="border p-2">X-Signature</td><td class="border p-2">{{ $t('guide.flow.h4') }}</td></tr>
              </tbody>
            </table>
            <Typography.Title :level="4">{{ $t('guide.flow.signTitle') }}</Typography.Title>
            <pre class="rounded bg-gray-900 p-4 text-xs leading-6 text-gray-100">{{ `签名串（六行，\\n 分隔）：
AppKey
HTTP Method（大写）
接口路径（后端路径，不含网关 /api 前缀）
时间戳（毫秒）
Nonce
请求体 SHA-256（无 body 传空串的哈希）

X-Signature = HMAC-SHA256(AppSecret, 签名串) 的 hex 小写` }}</pre>
          </div>
        </TabPane>
        <TabPane key="curl" tab="cURL">
          <pre class="overflow-x-auto rounded bg-gray-900 p-4 text-xs leading-6 text-gray-100">{{ CODE_CURL }}</pre>
        </TabPane>
        <TabPane key="python" tab="Python">
          <pre class="overflow-x-auto rounded bg-gray-900 p-4 text-xs leading-6 text-gray-100">{{ CODE_PY }}</pre>
        </TabPane>
        <TabPane key="java" tab="Java">
          <pre class="overflow-x-auto rounded bg-gray-900 p-4 text-xs leading-6 text-gray-100">{{ CODE_JAVA }}</pre>
        </TabPane>
        <TabPane key="errors" :tab="$t('guide.tabs.errors')">
          <div class="flex flex-col gap-3 leading-7">
            <p><b>401</b> — {{ $t('guide.errors.e401') }}</p>
            <p><b>403</b> — {{ $t('guide.errors.e403') }}</p>
            <p>{{ $t('guide.errors.tip') }}</p>
          </div>
        </TabPane>
      </Tabs>
    </Card>
  </Page>
</template>
