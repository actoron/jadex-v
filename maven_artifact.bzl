load("@rules_java//java:defs.bzl", "JavaInfo")
load("//:deploy_config.bzl", "JADEX_VERSION")  # ← aus env.sh generiert

POM_INFO = {
    "name": "jadex-v",
    "description": "Jadex V agent framework",
    "url": "https://github.com/actoron/jadex-v",
    "scm_url": "https://github.com/actoron/jadex-v.git",
    "license": {
        "name": "GPL-3.0",
        "url": "https://gnu.org/licenses/gpl-3.0",
    },
    "developers": [
        {
            "id": "alex",
            "name": "Alexander Pokahr",
            "email": "ap@actoron.com",
            "org": "Actoron GmbH",
            "org_url": "https://actoron.com/",
        },
        {
            "id": "lars",
            "name": "Lars Braubach",
            "email": "lb@actoron.com",
            "org": "Actoron GmbH",
            "org_url": "https://actoron.com/",
        },
        {
            "id": "kai",
            "name": "Kai Jander",
            "email": "kj@actoron.com",
            "org": "Actoron GmbH",
            "org_url": "https://actoron.com/",
        },
    ],
}

# ---------------------------------------------------------------------------
# Hilfsfunktionen
# ---------------------------------------------------------------------------

def _derive_artifact_name(ctx):
    """Label-Name ohne _publish-Suffix."""
    name = ctx.label.name
    if name.endswith("_publish"):
        name = name[:-8]
    return name

def _derive_group(ctx):
    """
    Leitet groupId aus dem Package-Pfad ab.
    //util/common:common_publish  →  generated.util
    //common:common_publish       →  generated
    """
    pkg = ctx.label.package          # "util/common" oder "common"
    parts = pkg.split("/")
    if len(parts) > 1:
        return "generated." + ".".join(parts[:-1])
    return "generated"

# ---------------------------------------------------------------------------
# GAV-Auflösung
# ---------------------------------------------------------------------------

def internal_dep_to_gav(label):
    #pkg = label.package              # "util/common"
    #artifact = pkg.split("/")[-1]    # "common"

    artifact = label.name

    version = JADEX_VERSION or "5.0.0-beta1"

    return "{}:{}:{}".format("org.activecomponents.jadex", artifact, version)

def path_to_gav(path):
    """Maven-Jar-Pfad im Cache → GAV."""
    marker = "/maven2/"
    idx = path.find(marker)
    if idx < 0:
        return None

    rel = path[idx + len(marker):]
    parts = rel.split("/")

    # org/eclipse/jetty/jetty-server/11.0.26/jetty-server-11.0.26.jar
    if len(parts) < 4:
        return None

    artifact = parts[-3]
    version = parts[-2]
    group = ".".join(parts[:-3])

    if not group:
        return None

    return "{}:{}:{}".format(group, artifact, version)

def maven_dep_to_gav(dep):
    """Versucht GAV aus dem JavaInfo-Pfad eines externen Deps zu lesen."""
    if JavaInfo not in dep:
        return None
    for f in dep[JavaInfo].compile_jars.to_list():
        gav = path_to_gav(f.path)
        if gav:
            return gav
    return None

def resolve_deps(ctx):
    """Gibt alle Dep-GAVs als dict zurück (GAV → True)."""
    deps = {}
    for dep in ctx.attr.deps:
        gav = maven_dep_to_gav(dep)
        if not gav:
            gav = internal_dep_to_gav(dep.label)
        if gav:
            deps[gav] = True
    return deps

# ---------------------------------------------------------------------------
# POM-Generierung
# ---------------------------------------------------------------------------

def generate_pom(ctx, java_info):
    group    = _derive_group(ctx)
    artifact = _derive_artifact_name(ctx)
    version  = JADEX_VERSION or "5.0.0-beta1"
    deps     = resolve_deps(ctx)

    xml = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<project xmlns="http://maven.apache.org/POM/4.0.0"',
        '         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"',
        '         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0',
        '                             https://maven.apache.org/xsd/maven-4.0.0.xsd">',
        "  <modelVersion>4.0.0</modelVersion>",

        "  <groupId>{}</groupId>".format(group),
        "  <artifactId>{}</artifactId>".format(artifact),
        "  <version>{}</version>".format(version),

        "  <name>{}</name>".format(POM_INFO["name"]),
        "  <description>{}</description>".format(POM_INFO["description"]),
        "  <url>{}</url>".format(POM_INFO["url"]),
    ]

    # ---------------- SCM ----------------
    xml += [
        "  <scm>",
        "    <url>{}</url>".format(POM_INFO["scm_url"]),
        "  </scm>",
    ]

    # ---------------- LICENSE ----------------
    lic = POM_INFO["license"]
    xml += [
        "  <licenses>",
        "    <license>",
        "      <name>{}</name>".format(lic["name"]),
        "      <url>{}</url>".format(lic["url"]),
        "      <distribution>repo</distribution>",
        "    </license>",
        "  </licenses>",
    ]

    # ---------------- DEVELOPERS ----------------
    xml.append("  <developers>")
    for d in POM_INFO["developers"]:
        xml += [
            "    <developer>",
            "      <id>{}</id>".format(d["id"]),
            "      <name>{}</name>".format(d["name"]),
            "      <email>{}</email>".format(d["email"]),
            "      <organization>{}</organization>".format(d["org"]),
            "      <organizationUrl>{}</organizationUrl>".format(d["org_url"]),
            "    </developer>",
        ]
    xml.append("  </developers>")

    # ---------------- DEPENDENCIES ----------------
    if deps:
        xml.append("  <dependencies>")
        for gav in sorted(deps.keys()):
            parts = gav.split(":")
            if len(parts) != 3:
                continue
            xml += [
                "    <dependency>",
                "      <groupId>{}</groupId>".format(parts[0]),
                "      <artifactId>{}</artifactId>".format(parts[1]),
                "      <version>{}</version>".format(parts[2]),
                "    </dependency>",
            ]
        xml.append("  </dependencies>")

    xml.append("</project>")
    return "\n".join(xml)

# ---------------------------------------------------------------------------
# Rule-Implementierung
# ---------------------------------------------------------------------------

def _maven_publish_impl(ctx):
    java_info = ctx.attr.target[JavaInfo]
    name = _derive_artifact_name(ctx)

    # --- Sources JAR ---
    sources = java_info.transitive_source_jars
    srcjar = ctx.actions.declare_file(name + "-sources.jar")
    ctx.actions.run_shell(
        inputs = sources,
        outputs = [srcjar],
        arguments = [srcjar.path] + [f.path for f in sources.to_list()],
        command = """
set -e
OUT="$1"; shift
tmp=$(mktemp -d)
for f in "$@"; do cp "$f" "$tmp/"; done
jar cf "$OUT" -C "$tmp" .
rm -rf "$tmp"
""",
    )

    # --- Javadoc Placeholder ---
    javadoc = ctx.actions.declare_file(name + "-javadoc.jar")
    ctx.actions.run_shell(
        outputs = [javadoc],
        arguments = [javadoc.path],
        command = """
set -e
OUT="$1"
tmp=$(mktemp -d)
cat > "$tmp/README.txt" <<EOF
Placeholder — no Javadoc generated.
EOF
jar cf "$OUT" -C "$tmp" .
rm -rf "$tmp"
""",
    )

    # --- POM ---
    pom = ctx.actions.declare_file(name + ".pom")
    ctx.actions.write(
        output = pom,
        content = generate_pom(ctx, java_info),
    )

    return DefaultInfo(
        files = depset([srcjar, javadoc, pom]),
    )

maven_publish_jars = rule(
    implementation = _maven_publish_impl,
    attrs = {
        "target": attr.label(
            providers = [JavaInfo],
            mandatory = True,
        ),
        "deps": attr.label_list(    # ← explizit für POM-Deps
            providers = [JavaInfo],
        ),
    },
)