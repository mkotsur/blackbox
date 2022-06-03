# DISCLAIMER: This Docker file is for demo/dev purposes only!

# Install dependencies only when needed
FROM node:14-alpine AS deps
ENV NEXT_TELEMETRY_DISABLED 1

RUN apk --no-cache --virtual build-dependencies add \
    python3 \
    make \
    g++
RUN apk add --update --no-cache libc6-compat
WORKDIR .
COPY package.json yarn.lock ./
RUN yarn install


FROM node:14-alpine AS runner
ENV NEXT_TELEMETRY_DISABLED 1
ARG BB_BACKEND_URL  

WORKDIR .
COPY . .
COPY --from=deps node_modules ./node_modules
COPY --from=deps package.json ./

EXPOSE 3000

CMD ["yarn", "dev"]