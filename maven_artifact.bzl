load("@rules_java//java:defs.bzl", "JavaInfo")

def _maven_publish_impl(ctx):
    java_info = ctx.attr.target[JavaInfo]

    # Basisname ableiten
    name = ctx.label.name
    if name.endswith("_publish"):
        name = name[:-8]

    # ------------------------------------------------------------
    # SOURCES JAR
    # ------------------------------------------------------------
    sources = java_info.transitive_source_jars

    srcjar = ctx.actions.declare_file(name + "-sources.jar")

    ctx.actions.run_shell(
        inputs = sources,
        outputs = [srcjar],
        arguments = [srcjar.path] + [f.path for f in sources.to_list()],
        command = """
set -e

OUT="$1"
shift

tmp=$(mktemp -d)

for f in "$@"; do
    cp "$f" "$tmp/"
done

jar cf "$OUT" -C "$tmp" .

rm -rf "$tmp"
""",
    )

    # ------------------------------------------------------------
    # JAVADOC PLACEHOLDER
    # ------------------------------------------------------------
    javadoc = ctx.actions.declare_file(name + "-javadoc.jar")

    ctx.actions.run_shell(
        outputs = [javadoc],
        arguments = [javadoc.path],
        command = """
set -e

OUT="$1"

tmp=$(mktemp -d)

cat > "$tmp/README.txt" <<EOF
This artifact contains no generated Javadoc.
It is a placeholder for Maven Central compatibility.
EOF

jar cf "$OUT" -C "$tmp" .

rm -rf "$tmp"
""",
    )

    # ------------------------------------------------------------
    # POM
    # ------------------------------------------------------------
    pom = ctx.actions.declare_file(name + ".pom")

    pom_xml = generate_pom(ctx, java_info)

    ctx.actions.write(
        output = pom,
        content = pom_xml,
    )

    return DefaultInfo(
        files = depset([
            srcjar,
            javadoc,
            pom,
        ]),
    )

maven_publish_jars = rule(
    implementation = _maven_publish_impl,
    attrs = {
        "target": attr.label(
            providers = [JavaInfo],
            mandatory = True,
        ),
    },
)

def generate_pom(ctx, java_info):
    deps = resolve_deps(ctx, java_info)

    xml = []

    xml.append('<?xml version="1.0" encoding="UTF-8"?>')
    xml.append('<project xmlns="http://maven.apache.org/POM/4.0.0"')
    xml.append('         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"')
    xml.append('         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0')
    xml.append('                             https://maven.apache.org/xsd/maven-4.0.0.xsd">')

    xml.append("  <modelVersion>4.0.0</modelVersion>")
    xml.append("  <groupId>generated</groupId>")
    xml.append("  <artifactId>{}</artifactId>".format(ctx.label.name))
    xml.append("  <version>1.0.0</version>")

    xml.append("  <dependencies>")

    for gav in sorted(deps.keys()):
        parts = gav.split(":")

        if len(parts) != 3:
            continue

        group = parts[0]
        artifact = parts[1]
        version = parts[2]

        xml.append("    <dependency>")
        xml.append("      <groupId>{}</groupId>".format(group))
        xml.append("      <artifactId>{}</artifactId>".format(artifact))
        xml.append("      <version>{}</version>".format(version))
        xml.append("    </dependency>")

    xml.append("  </dependencies>")
    xml.append("</project>")

    return "\n".join(xml)

def resolve_deps(ctx, java_info):
    deps = {}

    # external maven deps
    for f in java_info.transitive_runtime_jars.to_list():
        gav = path_to_gav(f.path)
        if gav:
            deps[gav] = True

    # internal deps fallback (no graph traversal)
    for f in java_info.transitive_compile_time_jars.to_list():
        gav = path_to_gav(f.path)
        if gav:
            deps[gav] = True

    return deps

def path_to_gav(path):
    marker = "/maven2/"

    idx = path.find(marker)

    if idx < 0:
        return None

    rel = path[idx + len(marker):]

    parts = rel.split("/")

    # Erwartet:
    # org/eclipse/jetty/jetty-server/11.0.26/jetty-server-11.0.26.jar

    if len(parts) < 4:
        return None

    artifact = parts[-3]
    version = parts[-2]

    group_parts = parts[:-3]

    if not group_parts:
        return None

    group = ".".join(group_parts)

    return "{}:{}:{}".format(
        group,
        artifact,
        version,
    )