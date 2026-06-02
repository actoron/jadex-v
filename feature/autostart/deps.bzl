def _normalize(s):
    return s.replace(".", "_").replace("-", "_")

def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = _normalize(parts[0])
    artifact = _normalize(parts[1])
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'com.google.auto.service:auto-service:1.1.1',
    'com.google.auto.service:auto-service-annotations:1.1.1',
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]