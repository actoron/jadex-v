def _normalize(s):
    return s.replace(".", "_").replace("-", "_")

def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = _normalize(parts[0])
    artifact = _normalize(parts[1])
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'dev.langchain4j:langchain4j:1.15.1',
    'dev_langchain4j:langchain4j-core:1.15.1',
    'org.quartz-scheduler:quartz:2.5.2',
    'com.cronutils:cron-utils:9.2.1',
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]
 