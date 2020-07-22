FROM openjdk:8-jdk-buster

ARG PURPLE_VERSION="2.44"

ARG HMFTOOLS_RELEASES_URL="https://github.com/hartwigmedical/hmftools/releases/download/"

ARG PURPLE_DIR="purple-v${PURPLE_VERSION}"

ARG PURPLE_FILE="purple-${PURPLE_VERSION}.jar"

ARG PURPLE_RELEASE_URL="${HMFTOOLS_RELEASES_URL}/${PURPLE_DIR}/${PURPLE_FILE}"

RUN mkdir -p /opt/purple && \
      wget --output-document "/opt/purple/${PURPLE_FILE}" "${PURPLE_RELEASE_URL}" && \
      ln -s "/opt/purple/${PURPLE_FILE}" "/opt/purple/purple.jar"

ENTRYPOINT []
