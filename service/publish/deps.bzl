
def _normalize(s):
    return s.replace(".", "_").replace("-", "_")

def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = _normalize(parts[0])
    artifact = _normalize(parts[1])
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'com.eclipsesource.minimal-json:minimal-json:0.9.5',
	'jakarta.websocket:jakarta.websocket-api:2.3.0-M2',
    'jakarta.websocket:jakarta.websocket-client-api:2.3.0-M2',
	'jakarta.ws.rs:jakarta.ws.rs-api:4.0.0',
	'jakarta.servlet:jakarta.servlet-api:6.1.0',
    'org.glassfish.tyrus:tyrus-project:2.2.2',
    'org.glassfish.tyrus:tyrus-client:2.2.2',
    'org.glassfish.tyrus:tyrus-container-grizzly-client:2.2.2',
    'io.rest-assured:rest-assured:6.0.0', # todo: make test dep
    'org.hamcrest:hamcrest:3.0', # todo: make test dep
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]