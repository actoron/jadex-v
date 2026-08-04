def _normalize(s):
    return s.replace(".", "_").replace("-", "_")

def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = _normalize(parts[0])
    artifact = _normalize(parts[1])
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'jakarta.ws.rs:jakarta.ws.rs-api:4.0.0',
    'dev.langchain4j:langchain4j:1.15.1',
    'dev_langchain4j_langchain4j_core:1.15.1',
    'dev.langchain4j:langchain4j-http-client-jdk:1.15.1',
    'dev.langchain4j:langchain4j-ollama:1.15.1',
    'dev.langchain4j:langchain4j-google-ai-gemini:1.15.1',
    'dev.langchain4j:langchain4j-mistral-ai:1.15.1',
    'dev.langchain4j:langchain4j-open-ai:1.15.1',
    'dev.langchain4j:langchain4j-anthropic:1.15.1',
    'org.testcontainers:testcontainers:2.0.3',
    'org.testcontainers:testcontainers-ollama:2.0.3',
    'com.github.docker-java:docker-java-api:3.7.1',
    'com.eclipsesource.minimal-json:minimal-json:0.9.5',
    'com.cronutils:cron-utils:9.2.1',
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]	

