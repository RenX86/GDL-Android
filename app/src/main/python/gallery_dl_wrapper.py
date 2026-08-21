import os
import json
import logging
from gallery_dl import config, job, output

# Route Python logs to Android Logcat
output.initialize_logging(logging.INFO)
output.configure_logging(logging.INFO)

_initialized = False

def initialize(files_dir: str, download_dir: str):
    global _initialized
    # Fix paths so gallery-dl doesn't crash trying to expand '~'
    os.environ["HOME"] = files_dir
    os.environ["XDG_CONFIG_HOME"] = files_dir

    config.clear()
    config.set(("extractor",), "base-directory", download_dir)
    config.set(("extractor",), "path-restrict", "unix")
    config.set(("extractor",), "directory", ["{category}", "{subcategory}"])
    config.set(("extractor",), "postprocessors", [])  # Disable exec/ffmpeg

    _initialized = True

def get_info(url: str) -> str:
    if not _initialized:
        raise RuntimeError("Call initialize() first")

    results = []
    class InfoJob(job.DataJob):
        def handle_url(self, url, kwdict, fallback=None):
            results.append({
                "url": url,
                "filename": kwdict.get("filename", ""),
                "extension": kwdict.get("extension", ""),
                "category": kwdict.get("category", ""),
            })

    try:
        InfoJob(url).run()
    except Exception as e:
        return json.dumps({"error": str(e)})

    return json.dumps({"count": len(results), "items": results[:20]})

def download(url: str) -> str:
    if not _initialized:
        raise RuntimeError("Call initialize() first")

    try:
        ret = job.DownloadJob(url).run()
        return json.dumps({"status": "ok", "return_code": ret})
    except Exception as e:
        return json.dumps({"status": "error", "message": str(e)})
