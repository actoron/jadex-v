#!/usr/bin/env bash
set -euxo pipefail

BAZEL_OUT="${1:-$(bazel info bazel-bin)}"
RELEASE="${2:-release}"
STAGING="$RELEASE/staging"

echo "=== bazel-bin ==="
echo "$BAZEL_OUT" 

echo "=== poms ==="
find -L "$BAZEL_OUT" -name "*.pom" 2>/dev/null | head -20 || true

echo "=== jars ==="
find -L "$BAZEL_OUT" -name "*.jar" 2>/dev/null | head -20 || true

mkdir -p "$RELEASE"

rm -f "$RELEASE/bundle.zip"

rm -rf "$STAGING"
mkdir -p "$STAGING"

echo "Creating Maven staging repository..."
echo "Source:  $BAZEL_OUT"
echo "Target:  $STAGING"

extract_xml() {
  local file="$1"
  local tag="$2"

  xmllint --xpath \
    "string(/*[local-name()='project']/*[local-name()='$tag'])" \
    "$file"
}

#
# Parent POM (hardcoded, wie vorher bei Gradle nur fuer jadex-v als Ganzes)
#
first_pom=$(find -L "$BAZEL_OUT" -name "*.pom" | head -n 1 || true)
if [[ -z "$first_pom" ]]; then
  echo "ERROR: no *.pom found under $BAZEL_OUT"
  exit 1
fi

PARENT_GROUP="org.activecomponents.jadex"
PARENT_ARTIFACT_ID="jadex-v"
PARENT_VERSION=$(extract_xml "$first_pom" "version")

echo
echo "Generating parent POM: $PARENT_GROUP:$PARENT_ARTIFACT_ID:$PARENT_VERSION"

parent_group_path="${PARENT_GROUP//./\/}"
parent_target="$STAGING/$parent_group_path/$PARENT_ARTIFACT_ID/$PARENT_VERSION"
mkdir -p "$parent_target"

cat > "$parent_target/${PARENT_ARTIFACT_ID}-${PARENT_VERSION}.pom" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>${PARENT_GROUP}</groupId>
  <artifactId>${PARENT_ARTIFACT_ID}</artifactId>
  <version>${PARENT_VERSION}</version>
  <packaging>pom</packaging>

  <name>jadex-v</name>
  <description>Jadex V agent framework</description>
  <url>https://github.com/actoron/jadex-v</url>

  <scm>
    <url>https://github.com/actoron/jadex-v.git</url>
  </scm>

  <licenses>
    <license>
      <name>GPL-3.0</name>
      <url>https://gnu.org/licenses/gpl-3.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>

  <developers>
    <developer>
      <id>alex</id>
      <name>Alexander Pokahr</name>
      <email>ap@actoron.com</email>
      <organization>Actoron GmbH</organization>
      <organizationUrl>https://actoron.com/</organizationUrl>
    </developer>
    <developer>
      <id>lars</id>
      <name>Lars Braubach</name>
      <email>lb@actoron.com</email>
      <organization>Actoron GmbH</organization>
      <organizationUrl>https://actoron.com/</organizationUrl>
    </developer>
    <developer>
      <id>kai</id>
      <name>Kai Jander</name>
      <email>kj@actoron.com</email>
      <organization>Actoron GmbH</organization>
      <organizationUrl>https://actoron.com/</organizationUrl>
    </developer>
  </developers>
</project>
EOF


find -L "$BAZEL_OUT" -name "*.pom" | while read -r pom
do
  echo
  echo "Processing $pom"

  group=$(extract_xml "$pom" "groupId")
  artifact=$(extract_xml "$pom" "artifactId")
  version=$(extract_xml "$pom" "version")

  if [[ -z "$group" || -z "$artifact" || -z "$version" ]]; then
    echo "ERROR: Missing GAV in $pom"
    exit 1
  fi

  echo "  GAV: $group:$artifact:$version"

  group_path="${group//./\/}"

  target="$STAGING/$group_path/$artifact/$version"
  mkdir -p "$target"

  source_dir=$(dirname "$pom")

  #
  # Main jar
  #
  jar=$(find "$source_dir" \
      -maxdepth 1 \
      \( -name "lib${artifact}.jar" -o -name "${artifact}.jar" \) \
      | head -n 1 || true)

  if [[ -n "$jar" ]]; then
    cp "$jar" "$target/${artifact}-${version}.jar"
  else
    echo "  WARNING: no main jar found"
  fi


  #
  # Sources
  #
  sources=$(find "$source_dir" \
    -maxdepth 1 \
    -name "${artifact}-sources.jar" \
    | head -n 1 || true)

  if [[ -n "$sources" ]]; then
    cp "$sources" \
      "$target/${artifact}-${version}-sources.jar"
  fi


  #
  # Javadoc
  #
  javadoc=$(find "$source_dir" \
    -maxdepth 1 \
    -name "${artifact}-javadoc.jar" \
    | head -n 1 || true)

  if [[ -n "$javadoc" ]]; then
    cp "$javadoc" \
      "$target/${artifact}-${version}-javadoc.jar"
  fi


  #
  # POM
  #
  cp "$pom" "$target/${artifact}-${version}.pom"

done


echo
echo "Staging finished:"
find "$STAGING" -type f | sort