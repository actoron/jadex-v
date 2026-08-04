def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = parts[0].replace(".", "_")
    artifact = parts[1].replace("-", "_")
    return "@maven//:" + group + "_" + artifact

MAVEN_DEPS = [
	'org.fluentd:fluent-logger:0.3.4'
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]