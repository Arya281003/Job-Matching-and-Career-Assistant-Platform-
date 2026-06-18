#!/usr/bin/env sh
set -eu

mkdir -p /tmp/nginx-client-body /tmp/nginx-proxy /tmp/nginx-fastcgi /tmp/nginx-uwsgi /tmp/nginx-scgi

if [ -z "${MYSQL_URL:-}" ]; then
  echo "MYSQL_URL is not set. Add it as a Hugging Face Space secret."
fi

if [ -z "${MONGODB_URI:-}" ]; then
  echo "MONGODB_URI is not set. Add it as a Hugging Face Space secret."
fi

/opt/venv/bin/python -m uvicorn app.main:app --app-dir /app/nlp-service --host 127.0.0.1 --port 8001 &
NLP_PID="$!"

java -jar /app/backend/app.jar &
BACKEND_PID="$!"

nginx -g "daemon off;" &
NGINX_PID="$!"

trap 'kill "$NLP_PID" "$BACKEND_PID" "$NGINX_PID" 2>/dev/null || true' INT TERM

wait "$NGINX_PID"
