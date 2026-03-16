FROM eclipse-temurin:21-jdk AS base

ENV DEBIAN_FRONTEND=noninteractive \
    GRADLE_USER_HOME=/var/lib/gradle \
    ARTIFACT_DEST=/artifacts

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        git \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 raindrop \
    && useradd --system --uid 10001 --gid raindrop --create-home --home-dir /home/raindrop raindrop \
    && mkdir -p "$GRADLE_USER_HOME" "$ARTIFACT_DEST"

WORKDIR /app
COPY . /app
RUN find . -maxdepth 2 -type f -name gradlew -exec chmod +x {} + \
    && chmod +x scripts/docker-build.sh \
    && chown -R raindrop:raindrop /app "$GRADLE_USER_HOME" "$ARTIFACT_DEST"

FROM base AS builder

ARG GITHUB_FINE_GRAIN_TOKEN=""

ENV GITHUB_FINE_GRAIN_TOKEN=${GITHUB_FINE_GRAIN_TOKEN}

RUN scripts/docker-build.sh

FROM base AS runner

USER 10001:10001

HEALTHCHECK --interval=30s --timeout=10s --start-period=20s --retries=3 \
    CMD ["sh", "-c", "command -v java >/dev/null && command -v git >/dev/null && test -x /app/scripts/docker-build.sh"]

ENTRYPOINT ["/app/scripts/docker-build.sh"]
