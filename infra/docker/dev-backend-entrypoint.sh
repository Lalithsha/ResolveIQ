#!/bin/sh
set -u

service_name="${DEV_SERVICE_NAME:?DEV_SERVICE_NAME must identify the Maven service module}"
poll_seconds="${DEV_POLL_SECONDS:-10}"
hot_reload="${DEV_HOT_RELOAD:-false}"
verify_image_cache="${DEV_VERIFY_IMAGE_CACHE:-false}"
app_pid=""
bootstrap_jar="/opt/resolveiq-dev-jars/${service_name}.jar"

fingerprint() {
  {
    find "/workspace/${service_name}/src/main" "/workspace/common-contracts/src/main" "/workspace/common-security/src/main" \
      -type f -exec sha256sum {} + 2>/dev/null
    sha256sum /workspace/pom.xml "/workspace/${service_name}/pom.xml" \
      /workspace/common-contracts/pom.xml /workspace/common-security/pom.xml 2>/dev/null
  } | sort | sha256sum | cut -d ' ' -f 1
}

build_service() {
  echo "[dev-reload] Compiling ${service_name} and required modules..."
  ./mvnw -q -pl ":${service_name}" -am package -Dmaven.test.skip=true -Djacoco.skip=true
}

start_service() {
  echo "[dev-reload] Starting ${service_name}..."
  lazy_initialization="${DEV_LAZY_INITIALIZATION:-true}"
  # Docker Desktop exposes several host CPUs to every container. Starting all
  # services with the JVM defaults creates far more JIT/GC threads than this
  # local stack needs and makes the services fight each other for CPU. These
  # defaults favor fast startup and a bounded heap; JAVA_OPTS remains an escape
  # hatch for profiling or service-specific overrides.
  java ${JAVA_OPTS:--XX:ActiveProcessorCount=2 -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xms64m -Xmx512m} \
    -Dspring.main.lazy-initialization="${lazy_initialization}" \
    -Dspring.data.jpa.repositories.bootstrap-mode=deferred \
    -jar "/tmp/${service_name}-running.jar" &
  app_pid=$!
}

prepare_service_jar() {
  built_jar="$(find "/workspace/${service_name}/target" -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)"
  if [ -z "${built_jar}" ]; then
    echo "[dev-reload] No executable jar found for ${service_name}."
    return 1
  fi
  cp "${built_jar}" "/tmp/${service_name}-next.jar"
}

activate_service_jar() {
  stop_service
  mv "/tmp/${service_name}-next.jar" "/tmp/${service_name}-running.jar"
  start_service
}

stop_service() {
  if [ -n "${app_pid}" ] && kill -0 "${app_pid}" 2>/dev/null; then
    kill "${app_pid}"
    wait "${app_pid}" 2>/dev/null || true
  fi
  app_pid=""
}

shutdown() {
  stop_service
  exit 0
}

trap shutdown INT TERM
cd /workspace

cached_fingerprint=""
if [ -f "/opt/resolveiq-dev-jars/${service_name}.fingerprint" ]; then
  cached_fingerprint="$(cat "/opt/resolveiq-dev-jars/${service_name}.fingerprint")"
fi

current_fingerprint=""
if [ "${verify_image_cache}" = "true" ] || [ ! -f "${bootstrap_jar}" ]; then
  current_fingerprint="$(fingerprint)"
fi

if [ -f "${bootstrap_jar}" ] && { [ "${verify_image_cache}" != "true" ] || [ "${cached_fingerprint}" = "${current_fingerprint}" ]; }; then
  echo "[dev-reload] Starting ${service_name} from the image build cache."
  cp "${bootstrap_jar}" "/tmp/${service_name}-running.jar"
  start_service
else
  echo "[dev-reload] Image cache is absent or stale; compiling ${service_name}."
  until build_service; do
    echo "[dev-reload] Build failed; retrying after a source change."
    failed_fingerprint="$(fingerprint)"
    while [ "$(fingerprint)" = "${failed_fingerprint}" ]; do sleep "${poll_seconds}"; done
  done

  if prepare_service_jar; then
    activate_service_jar
  else
    exit 1
  fi
fi

if [ "${hot_reload}" != "true" ]; then
  wait "${app_pid}"
  exit $?
fi

last_fingerprint="$(fingerprint)"

while true; do
  sleep "${poll_seconds}"
  current_fingerprint="$(fingerprint)"

  if [ "${current_fingerprint}" != "${last_fingerprint}" ]; then
    echo "[dev-reload] Source change detected for ${service_name}."
    if build_service && prepare_service_jar; then
      activate_service_jar
      last_fingerprint="$(fingerprint)"
    else
      echo "[dev-reload] Build failed; keeping the last successful process running."
      last_fingerprint="${current_fingerprint}"
    fi
  elif [ -n "${app_pid}" ] && ! kill -0 "${app_pid}" 2>/dev/null; then
    echo "[dev-reload] ${service_name} stopped unexpectedly; restarting it."
    wait "${app_pid}" 2>/dev/null || true
    start_service || true
  fi
done
