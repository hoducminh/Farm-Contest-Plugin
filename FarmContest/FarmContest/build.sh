#!/usr/bin/env bash
# ============================================================
# build.sh — Build FarmContest plugin với Java 21 + Maven
# Sử dụng: ./build.sh
# File JAR kết quả: target/FarmContest-<version>.jar
# ============================================================

set -e

JAVA21_HOME="/nix/store/3ilfkn8kxd9f6g5hgr0wpbnhghs4mq2m-openjdk-21.0.7+6"

echo "=== [1/3] Kiểm tra môi trường ==="
export JAVA_HOME="$JAVA21_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -version
echo ""

echo "=== [2/3] Đang build (mvn clean package) ==="
mvn clean package -q
echo ""

echo "=== [3/3] Tìm file JAR ==="
JAR=$(find target -name "FarmContest-*.jar" ! -name "*original*" | head -1)
if [ -n "$JAR" ]; then
  echo "✅ Build thành công!"
  echo "📦 File JAR: $(pwd)/$JAR"
  ls -lh "$JAR"
else
  echo "❌ Không tìm thấy file JAR trong target/"
  exit 1
fi
