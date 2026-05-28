# -------- BUILD --------
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm install

#RUN npm install -g @angular/cli

COPY . .

RUN npm run build -- --configuration production 

# -------- RUNTIME --------
FROM nginx:alpine

# Copiar build Angular
COPY --from=build /app/dist/frontend-svua/browser /usr/share/nginx/html

# 👇 TEMPLATE (IMPORTANTE)
COPY src/assets/env.template.js /usr/share/nginx/html/assets/env.js

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]

EXPOSE 80

# 👇 REEMPLAZO DINÁMICO
CMD ["/bin/sh", "-c", "envsubst < /usr/share/nginx/html/assets/env.js > /usr/share/nginx/html/assets/env.js && nginx -g 'daemon off;'"]