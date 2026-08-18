#!/bin/sh

echo "Generando env.js..."

cat <<EOF > /usr/share/nginx/html/assets/env.js
window.__env = {
  apiUrl: "${API_URL}",
  wsUrl: "${WS_URL}"
};
EOF

echo "env.js generado:"
cat /usr/share/nginx/html/assets/env.js

nginx -g "daemon off;"