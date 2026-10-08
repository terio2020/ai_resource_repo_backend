#!/usr/bin/env bash
set -euo pipefail
repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
python3 - "$repo_dir/deploy.sh" <<'PYTEST'
import os
import subprocess
import sys
import tempfile
from pathlib import Path

source = Path(sys.argv[1]).read_text()
selection = source.split("select_server() {", 1)[1].split("\nselect_server\n", 1)[0]
selection = "select_server() {" + selection
invocation = source.split("  docker run -d", 1)[1].split('  echo "Container started"', 1)[0]
invocation = "docker run -d" + invocation
with tempfile.TemporaryDirectory() as directory:
    arguments_file = Path(directory) / "arguments"
    for target in ("aws", "aliyun", "server1"):
        script = ("set -eu\nRUNTIME_DOCKER_ARGS=''\nRUNTIME_JAVA_ARGS=''\nTARGET=" + target
                  + "\n" + selection + "\nselect_server\n"
                  + "docker() { printf '%s\\0' \"$@\" > \"$ARGS_FILE\"; }\n"
                  + invocation)
        subprocess.run(["bash", "-c", script], check=True,
                       env={**os.environ, "ARGS_FILE": str(arguments_file)})
        args = arguments_file.read_bytes().decode().split("\0")[:-1]
        assert args[:2] == ["run", "-d"], args
        assert args.count("--log-opt") == 2, args
        assert "max-size=10m" in args and "max-file=3" in args, args
        assert args[-3:] == ["-jar", "app.jar", "--spring.profiles.active=prod"], args
        if target == "aws":
            assert "--memory=768m" in args and "--memory-swap=1g" in args, args
            assert args[args.index("java") + 1:args.index("-jar")] == [
                "-Xms64m", "-Xmx320m", "-XX:ReservedCodeCacheSize=64m"], args
        else:
            assert not any(arg.startswith("--memory") for arg in args), args
            assert args[args.index("java") + 1] == "-jar", args
print("PASS: rendered AWS runtime limits and all target log rotation; other JVM defaults preserved")
PYTEST
