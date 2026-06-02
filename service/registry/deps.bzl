
def _normalize(s):
    return s.replace(".", "_").replace("-", "_")

def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = _normalize(parts[0])
    artifact = _normalize(parts[1])
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'jakarta.ws.rs:jakarta.ws.rs-api:4.0.0',
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]
