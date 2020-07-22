FROM openjdk:8-jdk-buster

ARG COBALT_VERSION="1.8"

ARG HMFTOOLS_RELEASES_URL="https://github.com/hartwigmedical/hmftools/releases/download/"

ARG COBALT_DIR="cobalt-v${COBALT_VERSION}"

ARG COBALT_FILE="cobalt-${COBALT_VERSION}.jar"

ARG COBALT_RELEASE_URL="${HMFTOOLS_RELEASES_URL}/${COBALT_DIR}/${COBALT_FILE}"

RUN mkdir -p /opt/cobalt && \
      wget --output-document "/opt/cobalt/${COBALT_FILE}" "${COBALT_RELEASE_URL}" && \
      ln -s "/opt/cobalt/${COBALT_FILE}" "/opt/cobalt/cobalt.jar"

ENTRYPOINT []
