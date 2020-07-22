FROM openjdk:8-jdk-buster

ARG LINX_VERSION="1.10"

ARG HMFTOOLS_RELEASES_URL="https://github.com/hartwigmedical/hmftools/releases/download/"

ARG LINX_DIR="sv-linx-v${LINX_VERSION}"

ARG LINX_FILE="sv-linx_v${LINX_VERSION}.jar"

ARG LINX_RELEASE_URL="${HMFTOOLS_RELEASES_URL}/${LINX_DIR}/${LINX_FILE}"

RUN mkdir -p /opt/linx && \
      wget --output-document "/opt/linx/${LINX_FILE}" "${LINX_RELEASE_URL}" && \
      ln -s "/opt/linx/${LINX_FILE}" "/opt/linx/linx.jar"

ENTRYPOINT []
