#!/usr/bin/env python3
"""
TDD 강제 훅 — PostToolUse Write
Java Service/Controller 또는 React Page 컴포넌트가 구현 파일로 작성될 때
대응하는 테스트 파일이 없으면 경고를 출력한다.
"""
import sys
import json
import os

def main():
    try:
        data = json.load(sys.stdin)
    except Exception:
        return

    tool_name = data.get("tool_name", "")
    if tool_name != "Write":
        return

    f = data.get("tool_input", {}).get("file_path", "")
    if not f:
        return

    # Backend: Service 또는 Controller (src/main/java 내부)
    if "/src/main/java/" in f and (f.endswith("Service.java") or f.endswith("Controller.java")):
        test_f = f.replace("/src/main/java/", "/src/test/java/").replace(".java", "Test.java")
        if not os.path.exists(test_f):
            name = os.path.basename(f)
            test_name = name.replace(".java", "Test.java")
            print(f"⚠️  TDD: {name} 작성됨 — 테스트 파일이 없습니다")
            print(f"    → {test_name} 를 먼저 작성하세요 (Red → Green → Refactor)")
            print(f"    → /implement 스킬 참조")
        return

    # Frontend: pages/ 디렉토리의 컴포넌트 (테스트 파일 자체는 제외)
    if "/src/pages/" in f and f.endswith(".tsx") and not f.endswith(".test.tsx"):
        test_f = f.replace(".tsx", ".test.tsx")
        if not os.path.exists(test_f):
            name = os.path.basename(f)
            test_name = name.replace(".tsx", ".test.tsx")
            print(f"⚠️  TDD: {name} 작성됨 — {test_name} 가 없습니다")
            print(f"    → 컴포넌트와 함께 테스트 파일도 작성하세요")

if __name__ == "__main__":
    main()
