# -*- coding: utf-8 -*-
import hashlib, hmac, json, time, urllib.request, urllib.error, uuid

BASE = 'http://localhost/api'
results = []

def check(name, cond, detail=''):
    results.append((name, 'PASS' if cond else 'FAIL', detail))

def http(method, url, headers=None, body=None):
    data = body.encode() if isinstance(body, str) else body
    req = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read())
        except Exception:
            return e.code, {}

def admin_token():
    _, d = http('POST', f'{BASE}/auth/login', {'Content-Type': 'application/json'},
                json.dumps({'username': 'admin', 'password': '123456'}))
    return d['data']['accessToken']

def sign_headers(ak, sk, method, path, body=''):
    ts = str(int(time.time() * 1000))
    nonce = str(uuid.uuid4())
    bh = hashlib.sha256(body.encode()).hexdigest()
    ss = '\n'.join([ak, method, path, ts, nonce, bh])
    return {
        'X-App-Key': ak, 'X-Timestamp': ts, 'X-Nonce': nonce,
        'X-Signature': hmac.new(sk.encode(), ss.encode(), hashlib.sha256).hexdigest(),
        'Content-Type': 'application/json',
    }

TOKEN = admin_token()
AUTH = {'Authorization': f'Bearer {TOKEN}'}

# ---------- 准备：创建 APP + 开放两个接口 + 授权 ----------
_, d = http('POST', f'{BASE}/developer/apps', {**AUTH, 'Content-Type': 'application/json'},
            json.dumps({'name': 'AUTH-VERIFY', 'description': '鉴权专项校验'}))
app = d['data']
AK, SK, APP_ID = app['appKey'], app['appSecret'], app['id']
check('APP 创建（ak_/sk_ 前缀）', AK.startswith('ak_') and SK.startswith('sk_'), AK)

_, d = http('GET', f'{BASE}/developer/apis?pageSize=500', AUTH)
apis = {f"{a['method']} {a['path']}": a for a in d['data']['list']}
get_prod = apis.get('GET /requirement/products')
post_prod = apis.get('POST /requirement/products')
check('接口清单存在目标接口', bool(get_prod and post_prod), 'GET/POST /requirement/products')

for api in (get_prod, post_prod):
    http('PUT', f"{BASE}/developer/apis/{api['id']}", {**AUTH, 'Content-Type': 'application/json'},
         json.dumps({'status': 1}))
http('PUT', f'{BASE}/developer/auth/apps/{APP_ID}/apis', {**AUTH, 'Content-Type': 'application/json'},
     json.dumps([get_prod['id'], post_prod['id']]))

# ---------- 1. 正向：授权接口签名调用（GET，空 body） ----------
st, d = http('GET', f'{BASE}/requirement/products', sign_headers(AK, SK, 'GET', '/requirement/products'))
check('1 正向 GET 签名调用', st == 200 and d.get('code') == 0, f'http={st} code={d.get("code")}')

# ---------- 2. 正向：POST 带 body 签名（body hash 参与） ----------
body = json.dumps({'name': 'APP创建产品-校验', 'code': f'app-{uuid.uuid4().hex[:8]}'}, ensure_ascii=False)
st, d = http('POST', f'{BASE}/requirement/products', sign_headers(AK, SK, 'POST', '/requirement/products', body), body)
check('2 正向 POST 带体签名（创建产品）', st == 200 and d.get('code') == 0, f'code={d.get("code")}')

# body 被篡改（签名用 body A 发送 body B）
bad = body.replace('校验', '篡改xxx')
st, d = http('POST', f'{BASE}/requirement/products', sign_headers(AK, SK, 'POST', '/requirement/products', body), bad)
check('3 body 篡改拒绝', d.get('code') == 401, f'code={d.get("code")}')

# ---------- 4. 未授权接口 ----------
st, d = http('GET', f'{BASE}/requirement/stories', sign_headers(AK, SK, 'GET', '/requirement/stories'))
check('4 未授权接口 403', d.get('code') == 403, f'code={d.get("code")}')

# ---------- 5. 错误签名 ----------
h = sign_headers(AK, SK, 'GET', '/requirement/products')
h['X-Signature'] = '0' * 64
st, d = http('GET', f'{BASE}/requirement/products', h)
check('5 错误签名 401', d.get('code') == 401, f'code={d.get("code")}')

# ---------- 6. nonce 重放 ----------
h = sign_headers(AK, SK, 'GET', '/requirement/products')
st1, _ = http('GET', f'{BASE}/requirement/products', h)
st2, d2 = http('GET', f'{BASE}/requirement/products', h)
check('6 nonce 重放第2次 401', d2.get('code') == 401 and st1 == 200, f'first={st1} replay code={d2.get("code")}')

# ---------- 7. 过期时间戳 ----------
h = sign_headers(AK, SK, 'GET', '/requirement/products')
h['X-Timestamp'] = str(int(time.time() * 1000) - 10 * 60 * 1000)
# 重算签名（时间戳参与签名，直接改头会导致签名不匹配；应构造时用旧 ts）
def sign_headers_with_ts(ak, sk, method, path, ts, body=''):
    nonce = str(uuid.uuid4())
    bh = hashlib.sha256(body.encode()).hexdigest()
    ss = '\n'.join([ak, method, path, ts, nonce, bh])
    return {'X-App-Key': ak, 'X-Timestamp': ts, 'X-Nonce': nonce,
            'X-Signature': hmac.new(sk.encode(), ss.encode(), hashlib.sha256).hexdigest()}
st, d = http('GET', f'{BASE}/requirement/products',
             sign_headers_with_ts(AK, SK, 'GET', '/requirement/products', str(int(time.time() * 1000) - 10 * 60 * 1000)))
check('7 过期时间戳 401', d.get('code') == 401, f'code={d.get("code")}')

# ---------- 8. 未知 AppKey ----------
st, d = http('GET', f'{BASE}/requirement/products', sign_headers_with_ts('ak_unknown', SK, 'GET', '/requirement/products', str(int(time.time() * 1000))))
check('8 未知 AppKey 401', d.get('code') == 401, f'code={d.get("code")}')

# ---------- 9. 禁用 APP ----------
http('PUT', f'{BASE}/developer/apps/{APP_ID}', {**AUTH, 'Content-Type': 'application/json'}, json.dumps({'status': 0}))
st, d = http('GET', f'{BASE}/requirement/products', sign_headers(AK, SK, 'GET', '/requirement/products'))
check('9 禁用 APP 403', d.get('code') == 403, f'code={d.get("code")}')
http('PUT', f'{BASE}/developer/apps/{APP_ID}', {**AUTH, 'Content-Type': 'application/json'}, json.dumps({'status': 1}))
st, d = http('GET', f'{BASE}/requirement/products', sign_headers(AK, SK, 'GET', '/requirement/products'))
check('10 重新启用后恢复', d.get('code') == 0, f'code={d.get("code")}')

# ---------- 11. 缺签名头 ----------
st, d = http('GET', f'{BASE}/requirement/products', {'X-App-Key': AK})
check('11 缺签名头 401', d.get('code') == 401, f'code={d.get("code")}')

# ---------- 12. Secret 重置后旧 Secret 失效 ----------
_, d = http('PUT', f'{BASE}/developer/apps/{APP_ID}/secret', AUTH)
new_sk = d['data']['appSecret']
st, d = http('GET', f'{BASE}/requirement/products', sign_headers(AK, SK, 'GET', '/requirement/products'))
check('12a 旧 Secret 401', d.get('code') == 401, f'code={d.get("code")}')
st, d = http('GET', f'{BASE}/requirement/products', sign_headers(AK, new_sk, 'GET', '/requirement/products'))
check('12b 新 Secret 成功', d.get('code') == 0, f'code={d.get("code")}')

# ---------- 13. APP 调用带权限注解的写接口（@SaCheckPermission 虚拟身份放行） ----------
body = json.dumps({'name': 'APP-写接口-校验', 'code': f'app2-{uuid.uuid4().hex[:8]}'})
st, d = http('POST', f'{BASE}/requirement/products', sign_headers(AK, new_sk, 'POST', '/requirement/products', body), body)
check('13 APP 调用权限注解接口', d.get('code') == 0, f'code={d.get("code")}')

# ---------- 清理 ----------
http('DELETE', f'{BASE}/developer/apps/{APP_ID}', AUTH)

print()
for name, status, detail in results:
    print(f'{status}  {name:<28} {detail}')
passed = sum(1 for _, s, _ in results if s == 'PASS')
print(f'\n合计: {passed}/{len(results)} PASS')
