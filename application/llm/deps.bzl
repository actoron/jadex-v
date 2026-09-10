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
    'dev.langchain4j:langchain4j:1.18.1',
    'dev.langchain4j:langchain4j-core:1.18.1',
    'dev.langchain4j:langchain4j-open-ai:1.18.1',
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]  