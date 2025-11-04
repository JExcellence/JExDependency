#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARTIFACT_DEST="${ARTIFACT_DEST:-/artifacts}"
mkdir -p "$ARTIFACT_DEST"

echo "[docker-build] Artifact destination: $ARTIFACT_DEST"

declare -A GRADLE_WRAPPERS=()
declare -A GRADLE_TASK_CACHE=()

GRADLE_COMMON_ARGS=(--no-daemon)
SKIP_JAVADOC="${SKIP_JAVADOC:-true}"
SKIP_JAVADOC_JAR="${SKIP_JAVADOC_JAR:-false}"
SKIP_JAVADOC_JAR_ARGS=()

if [[ "$SKIP_JAVADOC" == "true" ]]; then
    echo "[docker-build] Skipping Javadoc tasks for all Gradle builds"
    GRADLE_COMMON_ARGS+=("-x" "javadoc")
fi

if [[ "$SKIP_JAVADOC_JAR" == "true" ]]; then
    SKIP_JAVADOC_JAR_ARGS=("-x" "javadocJar")
fi

has_gradle_task() {
    local project_dir=$1
    local wrapper=$2
    local task=$3
    local cache_key="${project_dir}::${task}"

    if [[ -n "${GRADLE_TASK_CACHE[$cache_key]:-}" ]]; then
        if [[ "${GRADLE_TASK_CACHE[$cache_key]}" == "1" ]]; then
            return 0
        fi
        return 1
    fi

    if (cd "$project_dir" && "$wrapper" help --task "$task" >/dev/null 2>&1); then
        GRADLE_TASK_CACHE[$cache_key]=1
        return 0
    fi

    GRADLE_TASK_CACHE[$cache_key]=0
    return 1
}

resolve_gradle_wrapper() {
    local module=$1
    local module_dir="$ROOT_DIR/$module"
    local candidate="$module_dir/gradlew"

    if [[ -n "${GRADLE_WRAPPERS[$module]:-}" ]]; then
        printf '%s' "${GRADLE_WRAPPERS[$module]}"
        return 0
    fi

    if [[ ! -d "$module_dir" ]]; then
        echo "[docker-build] Module directory not found: $module_dir" >&2
        exit 1
    fi

    if [[ -f "$candidate" ]]; then
        chmod +x "$candidate"
        GRADLE_WRAPPERS[$module]="$candidate"
        echo "[docker-build] Using Gradle wrapper for $module: $candidate" >&2
        printf '%s' "$candidate"
        return 0
    fi

    echo "[docker-build] Gradle wrapper not found for module '$module'. Expected at $candidate" >&2
    exit 1
}

run_gradle() {
    local module=$1
    shift

    local wrapper
    wrapper=$(resolve_gradle_wrapper "$module")

    local module_dir="$ROOT_DIR/$module"
    local args=("${GRADLE_COMMON_ARGS[@]}")

    if [[ "$SKIP_JAVADOC" == "true" && ${#SKIP_JAVADOC_JAR_ARGS[@]} -gt 0 ]]; then
        if has_gradle_task "$module_dir" "$wrapper" "javadocJar"; then
            args+=("${SKIP_JAVADOC_JAR_ARGS[@]}")
        fi
    fi

    (cd "$ROOT_DIR/$module" && "$wrapper" "${args[@]}" "$@")
}

require_cmd() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Required command '$1' is not available" >&2
        exit 1
    fi
}

for cmd in curl git; do
    require_cmd "$cmd"
done

configure_fine_grained_token() {
    local token="${GITHUB_FINE_GRAIN_TOKEN:-${GITHUB_TOKEN:-}}"
    if [[ -z "$token" ]]; then
        echo "[docker-build] GitHub fine-grained access token not provided. Proceeding without authenticated requests."
        return 0
    fi

    export GITHUB_TOKEN="$token"
    git config --global url."https://$GITHUB_TOKEN:@github.com/".insteadOf "https://github.com/"
    echo "[docker-build] Configured GitHub authentication with fine-grained access token."
}

configure_fine_grained_token

prepare_jeconfig_repo() {
    local repo_url="${JE_CONFIG_REPO_URL:-https://github.com/Antimatter-Zone/JEConfig.git}"
    local repo_ref="${JE_CONFIG_REPO_REF:-main}"
    local checkout_dir="${JE_CONFIG_CHECKOUT_DIR:-$ROOT_DIR/.external/JEConfig}"

    if [[ ! -d "$checkout_dir" ]]; then
        echo "[docker-build] Cloning JEConfig repository from $repo_url" >&2
        mkdir -p "$(dirname "$checkout_dir")"
        git clone "$repo_url" "$checkout_dir" >&2
    else
        echo "[docker-build] Updating existing JEConfig repository at $checkout_dir" >&2
        (cd "$checkout_dir" && git fetch --tags >&2 && git fetch origin >&2)
    fi

    echo "[docker-build] Checking out JEConfig reference $repo_ref" >&2
    (cd "$checkout_dir" && git checkout -f "$repo_ref" >&2)
    if (cd "$checkout_dir" && git rev-parse --verify "origin/$repo_ref" >/dev/null 2>&1); then
        (cd "$checkout_dir" && git reset --hard "origin/$repo_ref" >&2)
    else
        (cd "$checkout_dir" && git reset --hard "$repo_ref" >&2)
    fi

    printf '%s' "$checkout_dir"
}

publish_jeconfig_to_maven_local() {
    local checkout_dir
    checkout_dir=$(prepare_jeconfig_repo)

    local wrapper="$checkout_dir/gradlew"
    if [[ ! -x "$wrapper" ]]; then
        chmod +x "$wrapper"
    fi

    local jeconfig_args=("${GRADLE_COMMON_ARGS[@]}")
    if [[ "$SKIP_JAVADOC" == "true" && ${#SKIP_JAVADOC_JAR_ARGS[@]} -gt 0 ]]; then
        if has_gradle_task "$checkout_dir" "$wrapper" "javadocJar"; then
            jeconfig_args+=("${SKIP_JAVADOC_JAR_ARGS[@]}")
        else
            echo "[docker-build] javadocJar task not found for JEConfig; skipping exclusion flag"
        fi
    fi

    echo "[docker-build] Publishing JEConfig artifacts to mavenLocal"
    (cd "$checkout_dir" && "$wrapper" "${jeconfig_args[@]}" publishToMavenLocal)
}

publish_jeconfig_to_maven_local

extract_version() {
    local file=$1
    local version
    version=$(sed -n 's/^version\s*=\s*"\(.*\)"/\1/p' "$file" | head -n1)
    if [[ -z "$version" ]]; then
        echo "Failed to determine version from $file" >&2
        exit 1
    fi
    printf '%s' "$version"
}

RCORE_VERSION=$(extract_version "$ROOT_DIR/RCore/build.gradle.kts")
RDQ_VERSION=$(extract_version "$ROOT_DIR/RDQ/build.gradle.kts")
JEXE_VERSION=$(extract_version "$ROOT_DIR/JExEconomy/build.gradle.kts")

REQUIRED_FILES=(
    "$ARTIFACT_DEST/RCore-Premium-${RCORE_VERSION}.jar"
    "$ARTIFACT_DEST/RDQ-Free-${RDQ_VERSION}.jar"
    "$ARTIFACT_DEST/RDQ-Premium-${RDQ_VERSION}.jar"
    "$ARTIFACT_DEST/JExEconomy-${JEXE_VERSION}.jar"
)

all_present=true
for file in "${REQUIRED_FILES[@]}"; do
    if [[ ! -f "$file" ]]; then
        all_present=false
        break
    fi
done

if [[ "$all_present" == true ]]; then
    echo "[docker-build] All artifacts already exist for requested versions. Skipping build."
    exit 0
fi

echo "[docker-build] Preparing local Maven dependencies..."
run_gradle "JExDependency" "-Pjexdependency.disableExternalJavadocLinks=true" "publishToMavenLocal"
run_gradle "JExCommand" "publishToMavenLocal"
run_gradle "JExTranslate" "publishToMavenLocal"
run_gradle "RPlatform" "publishToMavenLocal"
run_gradle "JExEconomy" "publishToMavenLocal"
run_gradle "RCore" "publishLocal"
run_gradle "RDQ" "publishLocal"

echo "[docker-build] Building shaded artifacts..."
run_gradle "RCore" \
    ":rcore-premium:shadowJar" \
    ":rcore-free:shadowJar"
run_gradle "RDQ" \
    ":rdq-free:shadowJar" \
    ":rdq-premium:shadowJar"
run_gradle "JExEconomy" "shadowJar"

copy_artifact() {
    local source=$1
    local target=$2
    if [[ ! -f "$source" ]]; then
        echo "[docker-build] Expected artifact not found: $source" >&2
        exit 1
    fi
    cp "$source" "$target"
    echo "[docker-build] Copied $(basename "$target")"
}

copy_artifact "$ROOT_DIR/RCore/rcore-premium/build/libs/RCore-premium-${RCORE_VERSION}.jar" \
    "$ARTIFACT_DEST/RCore-Premium-${RCORE_VERSION}.jar"
copy_artifact "$ROOT_DIR/RDQ/rdq-free/build/libs/RDQ-free-${RDQ_VERSION}.jar" \
    "$ARTIFACT_DEST/RDQ-Free-${RDQ_VERSION}.jar"
copy_artifact "$ROOT_DIR/RDQ/rdq-premium/build/libs/RDQ-premium-${RDQ_VERSION}.jar" \
    "$ARTIFACT_DEST/RDQ-Premium-${RDQ_VERSION}.jar"
copy_artifact "$ROOT_DIR/JExEconomy/build/libs/JExEconomy-${JEXE_VERSION}.jar" \
    "$ARTIFACT_DEST/JExEconomy-${JEXE_VERSION}.jar"

echo "[docker-build] Build complete. Artifacts available in $ARTIFACT_DEST"
