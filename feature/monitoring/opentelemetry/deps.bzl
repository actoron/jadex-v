def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = parts[0].replace(".", "_")
    artifact = parts[1].replace("-", "_")
    return "@maven//:" + group + "_" + artifact

MAVEN_DEPS = [
    'io.opentelemetry:opentelemetry-api:1.44.1',
    'io.opentelemetry:opentelemetry-sdk:1.44.1',
    'io.opentelemetry:opentelemetry-exporter-otlp:1.44.1',
    'io.opentelemetry:opentelemetry-sdk-common:1.44.1',
    'io.opentelemetry:opentelemetry-sdk-logs:1.44.1',
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]
