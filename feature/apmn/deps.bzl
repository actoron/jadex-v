
def _maven_to_bazel(coord):
    parts = coord.split(":")
    group = parts[0].replace(".", "_")
    artifact = parts[1].replace("-", "_")
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    "net.bytebuddy:byte-buddy:1.17.8"
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]
