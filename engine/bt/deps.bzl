
def _normalize(s):
    return s.replace(".", "_").replace("-", "_")

def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = _normalize(parts[0])
    artifact = _normalize(parts[1])
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'org.ow2.asm:asm:9.9',  # for event scanning
	'org.ow2.asm:asm-commons:9.9',
    'org.mockito:mockito-core:5.2.0',  # only for testing
    'org.mockito:mockito-inline:5.2.0', # only for testing
    'org.mockito:mockito-junit-jupiter:5.12.0',  # only for testing
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]
