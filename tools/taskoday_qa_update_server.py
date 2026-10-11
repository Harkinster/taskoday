#!/usr/bin/env python3
"""Loopback-only HTTPS manifest/APK server for the Android update QA emulator."""

from __future__ import annotations

import argparse
import hashlib
import json
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import ssl
import time


PACKAGE = "com.harkinster.taskoday"
SIGNER_SHA256 = "4783e3e7f20c4bfec5e6b22fb0e3cfcaeb0dfddca3ebc380929c7fb1243a666b"

def make_manifest(apk: Path, available: bool, digest: str) -> dict:
    if not available:
        return {
            "schemaVersion": 1,
            "available": False,
            "packageName": PACKAGE,
            "versionCode": None,
            "versionName": None,
            "apkUrl": None,
            "sha256": None,
            "sizeBytes": None,
            "mandatory": False,
            "minSupportedVersionCode": None,
            "changelog": [],
            "publishedAt": None,
            "signerSha256": None,
        }
    return {
        "schemaVersion": 1,
        "available": True,
        "packageName": PACKAGE,
        "versionCode": 7,
        "versionName": "1.0-beta06-qa",
        "apkUrl": f"https://10.0.2.2:8443/taskoday-test/releases/v7-{digest[:16]}/taskoday-1.0-beta06-qa.apk",
        "sha256": digest,
        "sizeBytes": apk.stat().st_size,
        "mandatory": False,
        "minSupportedVersionCode": 6,
        "changelog": ["Build de test local : validation du parcours de mise à jour Android."],
        "publishedAt": datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"),
        "signerSha256": SIGNER_SHA256,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--certificate", required=True, type=Path)
    parser.add_argument("--private-key", required=True, type=Path)
    parser.add_argument("--state-file", required=True, type=Path, help="Contains 'available' or 'unavailable'; can be changed during the test")
    parser.add_argument("--throttle-bps", type=int, default=0, help="Optional local transfer limit for progress UI tests")
    args = parser.parse_args()
    if not args.apk.is_file() or not args.certificate.is_file() or not args.private_key.is_file():
        raise SystemExit("APK or QA TLS identity file is missing")
    if not args.state_file.is_file():
        raise SystemExit("State file is missing; create it with 'unavailable' or 'available'")
    if args.throttle_bps < 0:
        raise SystemExit("--throttle-bps must be non-negative")
    digest = hashlib.sha256(args.apk.read_bytes()).hexdigest()
    release_path = f"/taskoday-test/releases/v7-{digest[:16]}/taskoday-1.0-beta06-qa.apk"

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, fmt: str, *values: object) -> None:
            print("QA HTTPS: " + fmt % values, flush=True)

        def send_bytes(self, status: int, content_type: str, body: bytes) -> None:
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store, max-age=0")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.end_headers()
            if self.command != "HEAD":
                self.wfile.write(body)

        def send_apk(self) -> None:
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Length", str(args.apk.stat().st_size))
            self.send_header("Cache-Control", "no-store, max-age=0")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.end_headers()
            if self.command == "HEAD":
                return
            sent = 0
            with args.apk.open("rb") as apk_file:
                while True:
                    block = apk_file.read(64 * 1024)
                    if not block:
                        break
                    self.wfile.write(block)
                    sent += len(block)
                    if args.throttle_bps:
                        time.sleep(len(block) / args.throttle_bps)

        def do_HEAD(self) -> None:
            self.route()

        def do_GET(self) -> None:
            self.route()

        def route(self) -> None:
            if self.path == "/taskoday-test/version.json":
                state = args.state_file.read_text(encoding="ascii").strip()
                if state not in ("available", "unavailable"):
                    self.send_bytes(500, "text/plain; charset=utf-8", b"Invalid QA state")
                    return
                manifest = make_manifest(args.apk, state == "available", digest)
                self.send_bytes(200, "application/json; charset=utf-8", json.dumps(manifest, ensure_ascii=False).encode("utf-8"))
            elif self.path == release_path and args.state_file.read_text(encoding="ascii").strip() == "available":
                self.send_apk()
            else:
                self.send_bytes(404, "text/plain; charset=utf-8", b"Not found")

    server = ThreadingHTTPServer(("127.0.0.1", 8443), Handler)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(str(args.certificate), str(args.private_key))
    server.socket = context.wrap_socket(server.socket, server_side=True)
    print("Serving QA-only HTTPS on 127.0.0.1:8443 (Android emulator gateway 10.0.2.2)", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
