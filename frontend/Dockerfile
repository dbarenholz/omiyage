# Build stage
FROM oven/bun:1-alpine AS build
WORKDIR /app
COPY package.json ./
RUN bun install
COPY . .
RUN bun run build

# Runtime stage
FROM oven/bun:1-alpine
WORKDIR /app
COPY --from=build /app/build ./build
COPY --from=build /app/package.json .
COPY --from=build /app/node_modules ./node_modules
ENV PORT=3000
ENV NODE_ENV=production
EXPOSE 3000
CMD ["bun", "run", "start"]
