#!/usr/bin/env python3
"""Small Puck 0 media bridge.

The Show sends JSON messages over WebSocket:
  {"type":"audio","format":"wav","data":"<base64>"}
  {"type":"image","format":"jpeg","data":"<base64>"}

This first bridge deliberately keeps the ChatGPT web adapter behind one
function.  It saves an evidence copy, reports the request, and returns the
adapter's audio bytes to the Show.  Set PUCK_WEB_ADAPTER to a module exposing
async handle(media_type, path) -> bytes | None when browser integration is
ready.  No OpenAI credential is handled here.
"""
import asyncio, base64, importlib, json, os, pathlib, time
from aiohttp import web

ROOT = pathlib.Path(os.environ.get("PUCK_BRIDGE_DIR", "./puck-bridge-data"))
ROOT.mkdir(parents=True, exist_ok=True)
ADAPTER = os.environ.get("PUCK_WEB_ADAPTER")

async def health(_):
    return web.json_response({"ok": True, "service": "puck-web-bridge", "adapter": ADAPTER or "none"})

async def media(request):
    try:
        body = await request.json()
        kind = body["type"]
        raw = base64.b64decode(body["data"], validate=True)
        ext = {"audio": ".wav", "image": ".jpg"}.get(kind)
        if not ext or len(raw) > 25 * 1024 * 1024:
            raise ValueError("unsupported type or payload too large")
    except (KeyError, ValueError, json.JSONDecodeError, base64.binascii.Error) as e:
        raise web.HTTPBadRequest(text=str(e))
    path = ROOT / f"{time.time_ns()}-{kind}{ext}"
    path.write_bytes(raw)
    reply = None
    if ADAPTER:
        adapter = importlib.import_module(ADAPTER)
        reply = await adapter.handle(kind, path)
    return web.json_response({"accepted": True, "type": kind, "bytes": len(raw),
                              "adapter": bool(ADAPTER),
                              "audio_b64": base64.b64encode(reply).decode() if reply else None})

async def ws(request):
    socket = web.WebSocketResponse(max_msg_size=30 * 1024 * 1024)
    await socket.prepare(request)
    await socket.send_json({"type": "ready", "protocol": "puck-web-bridge/0.1"})
    async for msg in socket:
        if msg.type != web.WSMsgType.TEXT:
            continue
        try:
            result = await media(type("Req", (), {"json": lambda s: asyncio.sleep(0, result=json.loads(msg.data))})())
            await socket.send_str(result.text)
        except web.HTTPException as e:
            await socket.send_json({"accepted": False, "error": e.text})
    return socket

app = web.Application()
app.add_routes([web.get("/health", health), web.post("/media", media), web.get("/ws", ws)])

if __name__ == "__main__":
    web.run_app(app, host=os.environ.get("PUCK_BIND", "0.0.0.0"), port=int(os.environ.get("PUCK_PORT", "8787")))
