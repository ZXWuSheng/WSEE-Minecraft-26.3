import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


def main():
    env = os.environ
    candidates = sorted(path for path in Path("build/libs").glob(env["JAR_PATTERN"])
                        if path.is_file() and not path.stem.endswith(("-sources", "-javadoc", "-dev")))
    if len(candidates) != 1:
        raise SystemExit(f"Expected exactly one distributable mod JAR, got {candidates}")
    source, = candidates
    if not source.stat().st_size:
        raise SystemExit("The mod JAR is empty")
    destination = Path(env["RUNNER_TEMP"]) / "ci-dist"
    destination.mkdir()
    shutil.copy2(source, destination / source.name)
    with source.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    (destination / "SHA256SUMS").write_text(f"{digest}  {source.name}\n", encoding="utf-8")
    version = source.stem.partition("-")[2].removesuffix("-NeoForge")
    run_url = f"{env['GITHUB_SERVER_URL']}/{env['GITHUB_REPOSITORY']}/actions/runs/{env['GITHUB_RUN_ID']}"
    info = {
        "projectName": env["MOD_NAME"], "versionName": version, "jar": source.name,
        "sha256": digest, "runNumber": env["GITHUB_RUN_NUMBER"], "runUrl": run_url,
        "branch": env.get("GITHUB_HEAD_REF") or env["GITHUB_REF_NAME"], "commit": env["GITHUB_SHA"],
        "commitMessage": subprocess.check_output(["git", "log", "-1", "--format=%B"], encoding="utf-8").strip(),
    }
    (destination / "build-info.json").write_text(json.dumps(info, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    with open(env["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write(f"artifact_name={env['MOD_NAME']}-{env['GITHUB_RUN_ID']}-{env['GITHUB_RUN_ATTEMPT']}\n")


if __name__ == "__main__":
    main()
