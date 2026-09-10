from http.server import SimpleHTTPRequestHandler, HTTPServer
from pathlib import Path
import os

workspace = os.environ["BUILD_WORKSPACE_DIRECTORY"]

web_root = Path(workspace) / "feature" / "configurator"

print("RUNFILES_DIR:", os.environ.get("RUNFILES_DIR"))
print("cwd:", os.getcwd())
print("web root:", web_root)

os.chdir(web_root)

print("Serving:", web_root)

HTTPServer(("localhost", 8000), SimpleHTTPRequestHandler).serve_forever()

