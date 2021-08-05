FROM bellsoft/liberica-openjre-alpine-musl:11

LABEL "maintainer"="${project.organization.name}"
LABEL "org.opencontainers.image.authors"="${project.organization.url}"
LABEL "org.opencontainers.image.documentation"="${project.url}"
LABEL "org.opencontainers.image.vendor"="${project.organization.name}"
LABEL "org.opencontainers.image.licenses"="Apache License 2.0 / GNU General Public License, version 2"
LABEL "org.opencontainers.image.title"="${project.name}"
LABEL "org.opencontainers.image.description"="${project.description}"
LABEL "org.opencontainers.image.source"="${project.url}"

COPY bin/github-webhook.sh /continuous-benchmark/github-webhook/bin/github-webhook.sh
COPY lib /continuous-benchmark/github-webhook/lib

ENV JAVA_OPTS \
  -Dfile.encoding=UTF8 \
  -XX:+UseStringDeduplication \
  -XX:+ExitOnOutOfMemoryError

ENTRYPOINT [ "/continuous-benchmark/github-webhook/bin/github-webhook.sh" ]