
def _normalize(s):
    return s.replace(".", "_").replace("-", "_")

def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = _normalize(parts[0])
    artifact = _normalize(parts[1])
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'org.eclipse.jetty:jetty-server:11.0.26',
    'org.eclipse.jetty:jetty-servlet:11.0.26',
    'org.eclipse.jetty:jetty-util:11.0.26',
    'org.eclipse.jetty.websocket:websocket-jetty-api:11.0.26',
    'org.eclipse.jetty.websocket:websocket-jetty-server:11.0.26',
    'org.glassfish.tyrus:tyrus-client:2.2.2',
    'com.fasterxml.jackson.core:jackson-databind:2.15.2',
    'com.fasterxml.jackson.core:jackson-core:2.15.2',
    'jakarta.servlet:jakarta.servlet-api:6.1.0'  
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]
