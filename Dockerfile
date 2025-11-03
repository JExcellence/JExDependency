FROM eclipse-temurin:21-jdk AS base

ENV DEBIAN_FRONTEND=noninteractive \
    GRADLE_USER_HOME=/var/lib/gradle \
    ARTIFACT_DEST=/artifacts

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        git \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p "$GRADLE_USER_HOME" "$ARTIFACT_DEST"

WORKDIR /app
COPY . /app
RUN find . -maxdepth 2 -type f -name gradlew -exec chmod +x {} + \
    && chmod +x scripts/docker-build.sh

FROM base AS builder

ARG GITHUB_FINE_GRAIN_TOKEN=""

ENV GITHUB_FINE_GRAIN_TOKEN=${GITHUB_FINE_GRAIN_TOKEN}

RUN scripts/docker-build.sh

FROM base AS runner

ENTRYPOINT ["/app/scripts/docker-build.sh"]
