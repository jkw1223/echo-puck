const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const { createMedia } = require("./media");
const { WebSocketServer, WebSocket } = require("ws");
const WEATHER_ZIP = process.env.PUCK_WEATHER_ZIP || '35587';
let weatherText = '☾  --°';
let activeWss = null;
async function refreshWeather() {
  try {
    const g = await fetch(`https://geocoding-api.open-meteo.com/v1/search?name=${WEATHER_ZIP}&count=1&language=en&format=json`).then(r=>r.json());
    const p=g.results?.[0]; if(!p) throw Error('ZIP not found');
    const w=await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${p.latitude}&longitude=${p.longitude}&current=temperature_2m,weather_code,is_day&temperature_unit=fahrenheit`).then(r=>r.json());
    const c=w.current; const codes={0:'Clear',1:'Mostly clear',2:'Partly cloudy',3:'Overcast',45:'Foggy',48:'Foggy',51:'Drizzle',53:'Drizzle',55:'Drizzle',61:'Rain',63:'Rain',65:'Heavy rain',71:'Snow',73:'Snow',75:'Heavy snow',80:'Showers',81:'Showers',82:'Heavy showers',95:'Thunderstorms',96:'Thunderstorms',99:'Thunderstorms'};
    weatherText=`${c.is_day ? '☀' : '☾'}  ${Math.round(c.temperature_2m)}°  ${codes[c.weather_code]||'Current'}`;
    console.log(JSON.stringify({event:'weather_updated',zip:WEATHER_ZIP,text:weatherText}));
    if (activeWss) for (const socket of activeWss.clients) if (socket.readyState === WebSocket.OPEN) socket.send(JSON.stringify({type:'weather',weatherText}));
  } catch(e) { console.log(JSON.stringify({event:'weather_error',zip:WEATHER_ZIP,message:e.message})); }
}
function loadEnvLocal() {
  const file = path.join(__dirname, '.env.local');
  if (!fs.existsSync(file)) return;
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const m = /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$/.exec(line);
    if (!m || process.env[m[1]] !== undefined) continue;
    process.env[m[1]] = m[2].replace(/^['"]|['"]$/g, '');
  }
}

function createRelay({ heartbeatLeaseMs = 45000, sweepMs = 5000, replyPcm = fs.readFileSync(path.join(__dirname, "fixtures/test-reply.pcm")), provider = null } = {}) {
  const devices = new Map();
  const media = createMedia({replyPcm, provider, turnTtlMs:provider?240000:60000});
  const server = http.createServer((req, res) => {
    if (media.handle(req, res)) return;
    if (req.method !== "GET" || req.url !== "/health") {
      res.writeHead(404); res.end("not found\n"); return;
    }
    const body = JSON.stringify({ ok: true, service: "puck-relay", protocol: 1,
      time: new Date().toISOString(), replyMode: process.env.PUCK_REPLY_MODE || (provider ? "chatgpt_supervised" : "fixture"), connections: wss.clients.size,
      devices: [...devices.values()].map(d => ({ ...d, ageMs: Date.now() - d.lastHeartbeatAt })) });
    res.writeHead(200, { "content-type": "application/json", "content-length": Buffer.byteLength(body) });
    res.end(body);
  });
  const wss = new WebSocketServer({ server, path: "/puck", maxPayload: 16384 });
  activeWss = wss;
  wss.on("connection", (socket, request) => {
    const connectedAt = Date.now();
    socket.mediaToken = media.register(() => socket.readyState === WebSocket.OPEN);
    console.log(JSON.stringify({ event: "connected", address: request.socket.remoteAddress }));
    socket.send(JSON.stringify({ type: "hello", protocol: 1, service: "puck-relay", replyMode: process.env.PUCK_REPLY_MODE || (provider ? "chatgpt_supervised" : "fixture"), mediaToken: socket.mediaToken, weatherText }));
    socket.on("message", (data, isBinary) => {
      let m;
      try { if (isBinary) throw Error("binary"); m = JSON.parse(data.toString()); }
      catch { socket.close(1008, "Expected JSON heartbeat"); return; }
      if (!m || m.type !== "heartbeat" || m.protocol !== 1 || !Number.isSafeInteger(m.id) || m.id < 1 ||
          typeof m.session !== "string" || !m.session.length || m.session.length > 128 ||
          typeof m.deviceId !== "string" || !m.deviceId.length || m.deviceId.length > 128 ||
          typeof m.device !== "string" || m.device.length > 128 ||
          typeof m.model !== "string" || m.model.length > 128 ||
          typeof m.android !== "string" || m.android.length > 32) {
        socket.close(1008, "Invalid heartbeat"); return;
      }
      const previous = devices.get(socket);
      if (previous && (previous.session !== m.session || previous.deviceId !== m.deviceId || m.id <= previous.sequence)) {
        socket.close(1008, "Heartbeat identity or sequence changed"); return;
      }
      devices.set(socket, { deviceId: m.deviceId, session: m.session, device: m.device,
        model: m.model, android: m.android, sequence: m.id, connectedAt, lastHeartbeatAt: Date.now() });
      media.identify(socket.mediaToken, m.session);
      socket.send(JSON.stringify({ type: "heartbeat_ack", protocol: 1, id: m.id, session: m.session }));
      console.log(JSON.stringify({ event: "heartbeat", device: m.device, id: m.id }));
    });
    socket.on("error", err => console.log(JSON.stringify({ event: "socket_error", message: err.message })));
    socket.on("close", code => {
      media.drop(socket.mediaToken);
      devices.delete(socket);
      console.log(JSON.stringify({ event: "disconnected", code }));
    });
    socket.connectedAt = connectedAt;
  });
  const sweep = setInterval(() => {
    media.sweep();
    for (const socket of wss.clients) {
      if (Date.now() - (devices.get(socket)?.lastHeartbeatAt || socket.connectedAt) > heartbeatLeaseMs) {
        media.drop(socket.mediaToken);
        devices.delete(socket);
        socket.terminate();
      }
    }
  }, sweepMs);
  sweep.unref();
  function close() {
    clearInterval(sweep);
    for (const socket of wss.clients) {
      media.drop(socket.mediaToken);
      if (socket.readyState === WebSocket.OPEN) socket.close(1001, "Relay stopping");
    }
    const force = setTimeout(() => { for (const socket of wss.clients) socket.terminate(); }, 1000);
    force.unref();
    return new Promise(resolve => wss.close(() => server.close(() => { clearTimeout(force); resolve(); })));
  }
  return { server, wss, close };
}
if (require.main === module) {
  loadEnvLocal();
  let provider=null,operator=null;
  if(process.env.PUCK_REPLY_MODE==='chatgpt_supervised'){
    const {createBrowserQueue,startOperator}=require('./browser-queue');
    provider=createBrowserQueue();operator=startOperator(provider,{port:Number(process.env.PUCK_OPERATOR_PORT||8790)});
  }
  if(process.env.PUCK_REPLY_MODE==='openai'){
    const {createOpenAIProvider}=require('./openai-provider');
    provider=createOpenAIProvider();
  }
  if(process.env.PUCK_REPLY_MODE==='realtime'){
    const {createRealtimeProvider}=require('./realtime-provider');
    provider=createRealtimeProvider();
  }
  const relay = createRelay({provider});
  const port = Number(process.env.PORT || 8787);
  relay.server.listen(port, "0.0.0.0", () => console.log(`puck-relay listening on 0.0.0.0:${port}`));
  refreshWeather(); setInterval(refreshWeather, 15*60*1000).unref();
  process.once("SIGTERM", () => {activeWss=null;operator?.close();relay.close();});
  process.once("SIGINT", () => {activeWss=null;operator?.close();relay.close();});
}
module.exports = { createRelay };
