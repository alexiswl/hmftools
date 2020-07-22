FROM openjdk:8-jdk-buster

ARG AMBER_VERSION="3.3"

ARG HMFTOOLS_RELEASES_URL="https://github.com/hartwigmedical/hmftools/releases/download/"

ARG AMBER_DIR="amber-v${AMBER_VERSION}"

ARG AMBER_FILE="amber-${AMBER_VERSION}.jar"

ARG AMBER_RELEASE_URL="${HMFTOOLS_RELEASES_URL}/${AMBER_DIR}/${AMBER_FILE}"

RUN mkdir -p /opt/amber && \
      wget --output-document "/opt/amber/${AMBER_FILE}" "${AMBER_RELEASE_URL}" && \
      ln -s "/opt/amber/${AMBER_FILE}" "/opt/amber/amber.jar"

ENTRYPOINT []
