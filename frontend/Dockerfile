# build
FROM node:20 as build
WORKDIR /app
COPY . .
RUN npm install
RUN npm run build

# producción
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html