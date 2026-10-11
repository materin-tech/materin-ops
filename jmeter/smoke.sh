#!/bin/bash
# materin-ops 冒烟测试：登录/工单 CRUD/资源/通知/开放 API 签名（TC 主链路）
set -e
BASE=${BASE:-http://localhost/api}
pass=0; fail=0
check() { # name, actual, expected_substring
  if echo "$2" | grep -q "$3"; then pass=$((pass+1)); echo "PASS $1"; else fail=$((fail+1)); echo "FAIL $1 → $2"; fi
}

# 1. 登录
TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"123456"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
check "登录" "$TOKEN" "-"

# 2. 工单创建→受理→流转→关闭
R=$(curl -s -X POST $BASE/ticket/tickets -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"title":"SMOKE-'$RANDOM'","type":3,"priority":2}')
ID=$(echo "$R" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
check "工单创建" "$R" '"code":0'
check "工单受理" "$(curl -s -X PUT $BASE/ticket/tickets/$ID/accept -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{}')" '"code":0'
check "流转1→2" "$(curl -s -X PUT $BASE/ticket/tickets/$ID/status/2 -H "Authorization: Bearer $TOKEN")" '"code":0'
check "流转2→3" "$(curl -s -X PUT $BASE/ticket/tickets/$ID/status/3 -H "Authorization: Bearer $TOKEN")" '"code":0'
check "工单关闭" "$(curl -s -X PUT $BASE/ticket/tickets/$ID/close -H "Authorization: Bearer $TOKEN")" '"code":0'
check "工单删除" "$(curl -s -X DELETE $BASE/ticket/tickets/$ID -H "Authorization: Bearer $TOKEN")" '"code":0'

# 3. 资源上传/下载
echo "smoke-$(date +%s)" > /tmp/smoke.txt
R=$(curl -s -X POST $BASE/resource/files/upload -H "Authorization: Bearer $TOKEN" -F "file=@/tmp/smoke.txt")
FID=$(echo "$R" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
check "资源上传" "$R" '"code":0'
check "资源下载" "$(curl -s $BASE/resource/files/$FID/download -H "Authorization: Bearer $TOKEN")" "smoke-"

# 4. 通知广播+未读数
check "通知发送" "$(curl -s -X POST $BASE/notice/send -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"title":"SMOKE","content":"smoke","userIds":[1]}')" '"code":0'
check "未读数" "$(curl -s $BASE/notice/unread-count -H "Authorization: Bearer $TOKEN")" '"code":0'

# 5. 开放 API HMAC 签名（创建 APP→授权 products→签名调用）
APP=$(curl -s -X POST $BASE/developer/apps -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"name":"SMOKE-APP"}')
AK=$(echo "$APP" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['appKey'])")
SK=$(echo "$APP" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['appSecret'])")
AID=$(echo "$APP" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
API_ID=$(curl -s "$BASE/developer/apis?pageSize=500" -H "Authorization: Bearer $TOKEN" | python3 -c "
import sys,json
for a in json.load(sys.stdin)['data']['list']:
    if a['path']=='/requirement/products' and a['method']=='GET': print(a['id']); break")
curl -s -X PUT $BASE/developer/apis/$API_ID -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"status":1}' >/dev/null
curl -s -X PUT $BASE/developer/auth/apps/$AID/apis -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "[$API_ID]" >/dev/null
APP_CALL=$(python3 - "$AK" "$SK" <<'PY'
import hashlib, hmac, json, sys, time, urllib.request, uuid
ak, sk = sys.argv[1], sys.argv[2]
ts = str(int(time.time()*1000)); nonce = str(uuid.uuid4())
ss = chr(10).join([ak, 'GET', '/requirement/products', ts, nonce, hashlib.sha256(b'').hexdigest()])
sig = hmac.new(sk.encode(), ss.encode(), hashlib.sha256).hexdigest()
req = urllib.request.Request('http://localhost/api/requirement/products',
    headers={'X-App-Key': ak, 'X-Timestamp': ts, 'X-Nonce': nonce, 'X-Signature': sig})
print(json.loads(urllib.request.urlopen(req).read())['code'])
PY
)
check "APP签名调用" "$APP_CALL" "^0$" 
# 清理 APP
curl -s -X DELETE $BASE/developer/apps/$AID -H "Authorization: Bearer $TOKEN" >/dev/null

echo "----"
echo "冒烟结果：PASS=$pass FAIL=$fail"
[ $fail -eq 0 ]
