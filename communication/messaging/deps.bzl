
def _normalize(s):
    return s.replace(".", "_").replace("-", "_")

def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = _normalize(parts[0])
    artifact = _normalize(parts[1])
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'org.bouncycastle:bcprov-jdk18on:1.80',
    'org.bouncycastle:bcpkix-jdk18on:1.80',
    'net.java.dev.jna:jna:5.13.0',
    'net.java.dev.jna:jna-platform:5.13.0',
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]
