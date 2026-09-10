# Gradle Cleanup Script

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT="."

echo "Cleaning Gradle artifacts under: $ROOT"

# Remove Gradle project files
find "$ROOT" \( \
    -name ".gradle" -o \
    -name "build" -o \
    -name "out" -o \
    -name "bin" \
\) -type d -prune -print -exec rm -rf {} +

# Remove Gradle wrapper files
find "$ROOT" \( \
    -name "gradlew" -o \
    -name "gradlew.bat" -o \
    -name "settings.gradle" -o \
    -name "settings.gradle.kts" -o \
    -name "build.gradle" -o \
    -name "build.gradle.kts" \
\) -type f -print -delete

# Remove gradle wrapper directories
find "$ROOT" -name "gradle" -type d -prune -print -exec rm -rf {} +

# Optional: remove IntelliJ Gradle metadata
find "$ROOT" \( \
    -name ".idea" -o \
    -name "*.iml" \
\) -print

cat <<EOF

Done.

Removed:
  - .gradle directories
  - build directories
  - bin directories
  - gradlew / gradlew.bat
  - build.gradle / build.gradle.kts
  - settings.gradle / settings.gradle.kts
  - gradle wrapper directories

NOT removed automatically:
  - ~/.gradle cache
  - IntelliJ metadata (.idea)

If you also want to delete the global Gradle cache:

  rm -rf ~/.gradle

Example usage:

  chmod +x cleanup-gradle.sh
  ./cleanup-gradle.sh
  ./cleanup-gradle.sh /path/to/project

EOF
```

## Safer preview mode

To preview what would be deleted before actually deleting:

```bash
find . \( \
    -name ".gradle" -o \
    -name "build" -o \
    -name "build.gradle" -o \
    -name "settings.gradle" \
\)
```

