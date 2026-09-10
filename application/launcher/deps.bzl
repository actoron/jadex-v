def _normalize(s):
    return s.replace(".", "_").replace("-", "_")

def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = _normalize(parts[0])
    artifact = _normalize(parts[1])
    if len(parts) == 4:
        classifier = _normalize(parts[3])
        return "@maven//:" + group + "_" + artifact + "_" + classifier
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'com.formdev:flatlaf:3.7.1',
    'org.openani.jsystemthemedetector:jSystemThemeDetector:3.9',
    'org.slf4j:slf4j-simple:2.0.18',
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]  
