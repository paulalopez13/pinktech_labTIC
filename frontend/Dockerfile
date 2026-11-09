FROM node:24-alpine AS build
WORKDIR /app
ENV NODE_USE_SYSTEM_CA=1
COPY package*.json ./
RUN npm ci --fetch-retries=5 --fetch-timeout=300000 --fetch-retry-mintimeout=1000 --fetch-retry-maxtimeout=10000
COPY . .
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
