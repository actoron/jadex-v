def _maven_to_bazel(coord):
    """Convert Maven coordinate to Bazel target.
    Example: org.ow2.asm:asm:9.9 -> @maven//:org_ow2_asm_asm
    """
    parts = coord.split(":")
    group = parts[0].replace(".", "_")
    artifact = parts[1].replace("-", "_")
    return "@maven//:" + group + "_" + artifact
    
MAVEN_DEPS = [
    'org.ow2.asm:asm:9.9',
    'org.ow2.asm:asm-tree:9.9',
    'org.ow2.asm:asm-util:9.9',
    'org.ow2.asm:asm-commons:9.9',
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]