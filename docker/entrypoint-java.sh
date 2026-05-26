#!/bin/sh
set -e

JAVA_OPTS="${JAVA_OPTS:-}"
JAR="${APP_JAR:-/app/app.jar}"

if [ -f /skywalking-agent/skywalking-agent.jar ] && [ "${SW_AGENT_ENABLED:-true}" != "false" ]; then
  if [ -n "${SW_AGENT_NAME}" ]; then
    COLLECTOR="${SW_AGENT_COLLECTOR_BACKEND_SERVICES:-skywalking-oap:11800}"
    JAVA_OPTS="-javaagent:/skywalking-agent/skywalking-agent.jar \
-Dskywalking.agent.service_name=${SW_AGENT_NAME} \
-Dskywalking.collector.backend_service=${COLLECTOR} \
${JAVA_OPTS}"
    echo "[entrypoint] SkyWalking agent enabled: ${SW_AGENT_NAME} -> ${COLLECTOR}"
  fi
fi

exec java ${JAVA_OPTS} -jar "${JAR}"
