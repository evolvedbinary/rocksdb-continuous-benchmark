FROM bellsoft/liberica-openjre-alpine-musl:11

LABEL "maintainer"="${project.organization.name}"
LABEL "org.opencontainers.image.authors"="${project.organization.url}"
LABEL "org.opencontainers.image.documentation"="${project.url}"
LABEL "org.opencontainers.image.vendor"="${project.organization.name}"
LABEL "org.opencontainers.image.licenses"="Apache License 2.0 / GNU General Public License, version 2"
LABEL "org.opencontainers.image.title"="${project.name}"
LABEL "org.opencontainers.image.description"="${project.description}"
LABEL "org.opencontainers.image.source"="${project.url}"

COPY bin/runner.sh /continuous-benchmark/runner/bin/runner.sh
COPY lib /continuous-benchmark/runner/lib

ENV JAVA_OPTS \
  -Dfile.encoding=UTF8 \
  -XX:+UseStringDeduplication \
  -XX:+ExitOnOutOfMemoryError

ENTRYPOINT [ "/continuous-benchmark/runner/bin/runner.sh" ]