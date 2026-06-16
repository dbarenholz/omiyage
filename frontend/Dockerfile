# dependencies/build stage
FROM oven/bun:1-alpine AS build
WORKDIR /app
COPY package.json bun.lockb* svelte.config.js tsconfig.json vite.config.ts ./
RUN bun install --frozen-lockfile
COPY . .
RUN bun run build

# production dependencies stage
FROM oven/bun:1-alpine AS prod-deps
WORKDIR /app
COPY package.json bun.lockb* ./
RUN bun install --production --frozen-lockfile

# runtime stage
FROM oven/bun:1-alpine AS runtime
WORKDIR /app
COPY --from=prod-deps /app/node_modules ./node_modules
COPY --from=build /app/build ./build

ENV NODE_ENV=production
ENV PORT=3000
EXPOSE 3000

LABEL org.opencontainers.image.source="https://github.com/dbarenholz/omiyage" \
      org.opencontainers.image.title="Omiyage Frontend" \
      org.opencontainers.image.description="Frontend for the Omiyage wish-list application" \
      org.opencontainers.image.authors="dbarenholz"

CMD ["bun", "/app/build/index.js"]