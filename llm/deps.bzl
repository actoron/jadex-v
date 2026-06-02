def _normalize(s):
    return s.replace(".", "_").replace("-", "_")

def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = _normalize(parts[0])
    artifact = _normalize(parts[1])
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'jakarta.ws.rs:jakarta.ws.rs-api:4.0.0',
    'dev_langchain4j_langchain4j_core:1.14.1',
    'dev.langchain4j:langchain4j:1.14.1',
    'dev.langchain4j:langchain4j-ollama:1.14.1',
    'dev.langchain4j:langchain4j-open-ai:1.14.1',
    'org.testcontainers:testcontainers:2.0.3',
    'org.testcontainers:testcontainers-ollama:2.0.3',
    'com.github.docker-java:docker-java-api:3.7.1',
    'com.eclipsesource.minimal-json:minimal-json:0.9.5',
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]	