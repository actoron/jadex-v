def _normalize(s):
    return s.replace(".", "_").replace("-", "_")

def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = _normalize(parts[0])
    artifact = _normalize(parts[1])
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'com.eclipsesource.minimal-json:minimal-json:0.9.5',
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]
