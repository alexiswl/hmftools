FROM openjdk:8-jdk-buster

ARG GRIPSS_VERSION="1.0"

ARG HMFTOOLS_RELEASES_URL="https://github.com/hartwigmedical/hmftools/releases/download/"

ARG GRIPSS_DIR="gripss-v${GRIPSS_VERSION}"

ARG GRIPSS_FILE="gripss-${GRIPSS_VERSION}.jar"

ARG GRIPSS_RELEASE_URL="${HMFTOOLS_RELEASES_URL}/${GRIPSS_DIR}/${GRIPSS_FILE}"

RUN mkdir -p /opt/gripss && \
      wget --output-document "/opt/gripss/${GRIPSS_FILE}" "${GRIPSS_RELEASE_URL}" && \
      ln -s "/opt/gripss/${GRIPSS_FILE}" "/opt/gripss/gripss.jar"

ENTRYPOINT []
