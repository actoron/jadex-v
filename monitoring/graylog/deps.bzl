def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = parts[0].replace(".", "_")
    artifact = parts[1].replace("-", "_")
    return "@maven//:" + group + "_" + artifact

MAVEN_DEPS = [
    'org.graylog2:gelfj:1.1.16',
	'org.graylog2:gelfclient:1.5.1'
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]