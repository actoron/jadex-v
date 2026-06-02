def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = parts[0].replace(".", "_")
    artifact = parts[1].replace("-", "_")
    return "@maven//:" + group + "_" + artifact

MAVEN_DEPS = [
    'org.apache.logging.log4j:log4j-core:3.0.0-beta2',
    'org.apache.logging.log4j:log4j-api:3.0.0-beta2', 
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]

