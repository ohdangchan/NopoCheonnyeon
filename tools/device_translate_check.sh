#!/bin/bash
# 폰에서 가게 상세 → AI 번역(English) 소요 시간을 잰다. 사용: bash tools/device_translate_check.sh
# 전제: 앱이 설치돼 있고 폰이 연결됨, AI 노포 도우미 기록의 첫 추천 가게(좌표는 S9+ 1080x2220 기준).
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
export MSYS_NO_PATHCONV=1
"$ADB" shell am force-stop com.nopo.cheonnyeon
"$ADB" shell am start -n com.nopo.cheonnyeon/.MainActivity > /dev/null
sleep 7
"$ADB" shell input tap 971 205   # 검색창 AI 버튼
sleep 3
"$ADB" shell input tap 300 1105  # 첫 추천 가게 카드
sleep 4
"$ADB" shell input tap 144 1304  # AI 번역
sleep 2
"$ADB" logcat -c
"$ADB" shell input tap 150 1442  # English
T0=$(date +%s)
for i in $(seq 1 45); do
  sleep 2
  "$ADB" shell uiautomator dump /sdcard/u.xml > /dev/null 2>&1
  if "$ADB" shell cat /sdcard/u.xml | grep -q "번역했어요"; then
    echo "번역 완료: $(( $(date +%s) - T0 ))초"
    break
  fi
  if "$ADB" shell cat /sdcard/u.xml | grep -q "가져오지 못했어요"; then
    echo "실패(문구 정상 표시): $(( $(date +%s) - T0 ))초"
    break
  fi
done
"$ADB" logcat -d -v time Nopo:W '*:S' | cut -c1-250 | tail -4
"$ADB" exec-out screencap -p > "$TEMP/shots/tr_check.png"
echo done
