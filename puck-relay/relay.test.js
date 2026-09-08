const { test } = require('node:test');
const assert = require('node:assert/strict');
const { once } = require('node:events');
const { WebSocket } = require('ws');
const { createRelay } = require('./index');
async function setup(t, options) {
  const relay = createRelay(options);
  relay.server.listen(0, '127.0.0.1');
  await once(relay.server, 'listening');
  const port = relay.server.address().port;
  t.after(() => relay.close());
  return { ...relay, port };
}
async function connect(port) {
  const ws = new WebSocket(`ws://127.0.0.1:${port}/puck`);
  const greeting = once(ws, 'message');
  await once(ws, 'open');
  assert.equal(JSON.parse((await greeting)[0]).type, 'hello');
  return ws;
}
const heartbeat = id => ({type:'heartbeat', protocol:1, id, session:'test-session', deviceId:'test-device', device:'checkers', model:'Echo Show 5', android:'11'});
test('acknowledges repeated heartbeats and withdraws disconnected device', async t => {
  const {port} = await setup(t);
  const ws = await connect(port);
  for (const id of [1, 2, 3]) {
    const ack = once(ws, 'message'); ws.send(JSON.stringify(heartbeat(id)));
    assert.deepEqual(JSON.parse((await ack)[0]), {type:'heartbeat_ack', protocol:1, id, session:'test-session'});
  }
  const health = await (await fetch(`http://127.0.0.1:${port}/health`)).json();
  assert.equal(health.devices.length, 1); assert.equal(health.devices[0].sequence, 3);
  const closed = once(ws, 'close'); ws.close(); await closed;
  const after = await (await fetch(`http://127.0.0.1:${port}/health`)).json();
  assert.equal(after.devices.length, 0);
});
test('rejects malformed, unsupported and out-of-order messages', async t => {
  const {port} = await setup(t);
  for (const message of ['{', 'null', JSON.stringify({...heartbeat(1),protocol:2}), JSON.stringify({...heartbeat(1),deviceId:''})]) {
    const ws = await connect(port); const closed = once(ws,'close'); ws.send(message);
    assert.equal((await closed)[0], 1008);
  }
  const ws = await connect(port); const ack = once(ws,'message');
  ws.send(JSON.stringify(heartbeat(2))); await ack;
  const closed = once(ws,'close'); ws.send(JSON.stringify(heartbeat(1)));
  assert.equal((await closed)[0],1008);
});
test('expires a silent client lease', async t => {
  const {port} = await setup(t, {heartbeatLeaseMs:80,sweepMs:10});
  const ws = await connect(port);
  const closed = once(ws,'close');
  const ack = once(ws,'message'); ws.send(JSON.stringify(heartbeat(1))); await ack;
  await closed;
  assert.equal((await (await fetch(`http://127.0.0.1:${port}/health`)).json()).devices.length,0);
});
test('graceful shutdown sends going-away close', async t => {
  const relay = await setup(t); const ws = await connect(relay.port);
  const closed = once(ws,'close'); await relay.close();
  assert.equal((await closed)[0],1001);
});
