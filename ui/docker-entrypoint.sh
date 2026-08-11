#!/bin/sh
set -eu

cat > /usr/share/nginx/html/env.js <<EOF
window.__env = {
  apiUrl: "${API_URL:-http://localhost:8080}"
};
EOF

exec nginx -g 'daemon off;'
