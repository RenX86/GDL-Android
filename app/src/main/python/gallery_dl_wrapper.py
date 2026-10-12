import os
import sys
import json
import logging

_initialized = False
_active_engine_dir = None

def _apply_engine_path(engine_dir: str):
    global _active_engine_dir
    if _active_engine_dir and _active_engine_dir in sys.path:
        sys.path.remove(_active_engine_dir)

    if engine_dir and os.path.exists(engine_dir):
        if engine_dir not in sys.path:
            sys.path.insert(0, engine_dir)
        _active_engine_dir = engine_dir
    else:
        _active_engine_dir = None

def set_engine_path(engine_dir: str = None) -> str:
    """Set custom engine path and reload gallery-dl module dynamically."""
    _apply_engine_path(engine_dir)
    # Evict cached gallery_dl submodules from sys.modules
    modules_to_del = [k for k in sys.modules if k == "gallery_dl" or k.startswith("gallery_dl.")]
    for m in modules_to_del:
        del sys.modules[m]

    try:
        import gallery_dl
        from gallery_dl import output
        output.initialize_logging(logging.INFO)
        output.configure_logging(logging.INFO)
        return getattr(gallery_dl, "__version__", "unknown")
    except Exception as e:
        return f"error: {str(e)}"

def get_engine_version() -> str:
    try:
        import gallery_dl
        return getattr(gallery_dl, "__version__", "unknown")
    except Exception as e:
        return f"error: {str(e)}"

def initialize(files_dir: str, download_dir: str, engine_dir: str = None):
    global _initialized
    if engine_dir:
        _apply_engine_path(engine_dir)

    from gallery_dl import config, output

    # Route Python logs to Android Logcat
    output.initialize_logging(logging.INFO)
    output.configure_logging(logging.INFO)

    # Fix paths so gallery-dl doesn't crash trying to expand '~'
    os.environ["HOME"] = files_dir
    os.environ["XDG_CONFIG_HOME"] = files_dir

    config.clear()
    config.set(("extractor",), "base-directory", download_dir)
    config.set(("extractor",), "path-restrict", "unix")
    # We purposefully DO NOT set the "directory" config. 
    # By default, gallery-dl's built-in behavior is perfectly mapped to create "Site/Username" for almost all sites.
    config.set(("extractor",), "postprocessors", [])  # Disable exec/ffmpeg

    _initialized = True

def set_cookies(cookie_file_path: str):
    """Tell gallery-dl to use a Netscape-format cookies.txt file."""
    from gallery_dl import config
    if os.path.exists(cookie_file_path):
        config.set(("extractor",), "cookies", cookie_file_path)

def set_user_agent(user_agent: str):
    """Set a custom User-Agent to match the WebView session."""
    from gallery_dl import config
    if user_agent:
        config.set(("downloader",), "http", {"headers": {"User-Agent": user_agent}})

def set_archive(archive_file_path: str):
    """Tell gallery-dl to track downloads in a sqlite3 archive to prevent duplicates."""
    from gallery_dl import config
    config.set(("extractor",), "archive", archive_file_path)

def clear_archive():
    """Remove the archive setting so gallery-dl won't deduplicate."""
    from gallery_dl import config
    config.set(("extractor",), "archive", None)

def get_info(url: str) -> str:
    if not _initialized:
        raise RuntimeError("Call initialize() first")

    from gallery_dl import job
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

    from gallery_dl import job
    try:
        ret = job.DownloadJob(url).run()
        return json.dumps({"status": "ok", "return_code": ret})
    except Exception as e:
        return json.dumps({"status": "error", "message": str(e)})
