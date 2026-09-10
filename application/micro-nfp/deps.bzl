def _normalize(s):
    return s.replace(".", "_").replace("-", "_")

def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = _normalize(parts[0])
    artifact = _normalize(parts[1])
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'org.apache.logging.log4j:log4j-core:3.0.0-beta2',
    'org.graylog2:gelfj:1.1.16',
    'org.graylog2:gelfclient:1.5.1',
    'org.fluentd:fluent-logger:0.3.4',
    'org.slf4j:slf4j-jdk14:1.8.0-beta4',
    'org.jfree:jfreechart:1.5.4'
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]  
