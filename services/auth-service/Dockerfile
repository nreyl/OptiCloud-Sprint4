FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json tsconfig.json nest-cli.json ./
RUN npm install
COPY src ./src
RUN npx nest build

FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
COPY package.json ./
RUN npm install --omit=dev
COPY --from=builder /app/dist ./dist
EXPOSE 3000
CMD ["node", "dist/main.js"]
