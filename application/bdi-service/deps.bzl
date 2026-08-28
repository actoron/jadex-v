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
    'com.badlogicgames.gdx:gdx:1.12.1',
    'com.badlogicgames.gdx:gdx-backend-lwjgl3:1.12.1',
    'com.badlogicgames.gdx:gdx-platform:1.12.1:natives-desktop',
]

DEPS = [_maven_to_bazel(artifact) for artifact in MAVEN_DEPS]  
