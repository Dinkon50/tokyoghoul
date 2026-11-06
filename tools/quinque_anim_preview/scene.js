'use strict';
// WebGL preview scene: player + quinque, third person views and first person view.
const QA = window.QuinqueAnimSys;
const QUINQUES = {
  kura: { mesh: 'quinque_kura', style: 'blade', flip: false },
  narukami: { mesh: 'quinque_narukami', style: 'blade', flip: false, ranged: true },
  jason: { mesh: 'quinque_jason', style: 'scythe', flip: true },
  ixa: { mesh: 'quinque_ixa', style: 'spear', flip: false },
};
let gl, prog, tex = {}, meshes = {}, ANIMS = null, COMPILED = {};

const VS = `attribute vec3 p; attribute vec2 uv; attribute vec3 n; uniform mat4 vp; varying vec2 vuv; varying vec3 vn; varying float vy;
void main(){ gl_Position = vp * vec4(p,1.0); vuv = uv; vn = n; vy = p.y; }`;
const FS = `precision mediump float; uniform sampler2D tx; uniform int mode; uniform vec3 tint; varying vec2 vuv; varying vec3 vn; varying float vy;
void main(){
  vec4 c;
  if (mode == 1) { c = vec4(tint,1.0); }
  else { c = texture2D(tx, vuv); if (c.a < 0.1) discard; }
  vec3 nn = normalize(vn); if (!gl_FrontFacing) nn = -nn;
  vec3 l0 = normalize(vec3(0.2,1.0,-0.7)); vec3 l1 = normalize(vec3(-0.2,1.0,0.7));
  float li = min(1.0, 0.4 + 0.6*max(0.0,dot(nn,l0)) + 0.6*max(0.0,dot(nn,l1)));
  if (mode == 2) li = 1.0;
  gl_FragColor = vec4(c.rgb*li, 1.0);
}`;

function persp(fovDeg, aspect, near, far) {
  const f = 1 / Math.tan(fovDeg * Math.PI / 360);
  return [f/aspect,0,0,0, 0,f,0,0, 0,0,(far+near)/(near-far),-1, 0,0,2*far*near/(near-far),0];
}
function lookAt(eye, at, up) {
  const z = norm(sub(eye, at)), x = norm(cross(up, z)), y = cross(z, x);
  return [x[0],y[0],z[0],0, x[1],y[1],z[1],0, x[2],y[2],z[2],0, -dot(x,eye),-dot(y,eye),-dot(z,eye),1];
}
function sub(a,b){return [a[0]-b[0],a[1]-b[1],a[2]-b[2]];}
function dot(a,b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
function cross(a,b){return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];}
function norm(a){const l=Math.hypot(a[0],a[1],a[2])||1;return [a[0]/l,a[1]/l,a[2]/l];}

function loadImage(src) { return new Promise(r => { const i = new Image(); i.onload = () => r(i); i.src = src; }); }
function makeTex(img) {
  const t = gl.createTexture(); gl.bindTexture(gl.TEXTURE_2D, t);
  gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, img);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  return t;
}
async function init(animJson) {
  const cv = document.getElementById('c');
  gl = cv.getContext('webgl', { preserveDrawingBuffer: true, antialias: true });
  const mk = (type, src) => { const s = gl.createShader(type); gl.shaderSource(s, src); gl.compileShader(s); if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw gl.getShaderInfoLog(s); return s; };
  prog = gl.createProgram(); gl.attachShader(prog, mk(gl.VERTEX_SHADER, VS)); gl.attachShader(prog, mk(gl.FRAGMENT_SHADER, FS)); gl.linkProgram(prog);
  gl.useProgram(prog);
  tex.skin = makeTex(await loadImage('assets/ccg.png'));
  tex.ghoul = makeTex(await loadImage('assets/ghoul.png'));
  tex.palette = makeTex(await loadImage('assets/palette.png'));
  for (const q of Object.values(QUINQUES)) for (const name of [q.mesh, q.mesh + '_open']) {
    const r = await fetch('assets/' + name + '.bin'); if (!r.ok) continue;
    meshes[name] = new Float32Array(await r.arrayBuffer());
  }
  setAnims(animJson);
  return 'ok';
}
function setAnims(json) {
  ANIMS = json; COMPILED = {};
  for (const style in json.styles) for (const mv in json.styles[style]) COMPILED[style + ':' + mv] = QA.compile(json.styles[style][mv]);
}

// ---------------------------------------------------------------- geometry batches
function batch() { return { d: [] }; }
function pushTri(b, a, c, e) { for (const v of [a, c, e]) b.d.push(...v); }
function addModelPart(b, ps, part) {
  ps.push(); part.translateAndRotate(ps);
  const m = ps.m;
  for (const poly of part.cubes) {
    const q = poly.q.map(v => { const w = apply(m, [v[0]/16, v[1]/16, v[2]/16]); return [w[0], w[1], w[2], v[3]/64, v[4]/64]; });
    const n = norm(apply(m, [...poly.n, 0]));
    const A = [...q[0], ...n], B = [...q[1], ...n], C = [...q[2], ...n], D = [...q[3], ...n];
    pushTri(b, A, B, C); pushTri(b, A, C, D);
  }
  ps.pop();
}
function addMesh(b, m, data) {
  for (let i = 0; i < data.length; i += 8) {
    const w = apply(m, [data[i], data[i+1], data[i+2]]);
    const n = apply(m, [data[i+5], data[i+6], data[i+7], 0]);
    b.d.push(w[0], w[1], w[2], data[i+3], data[i+4], n[0], n[1], n[2]);
  }
}
function addBox(b, x0, y0, z0, x1, y1, z1) {
  const f = (p, n) => p.map(v => [...v, 0, 0, ...n]);
  const faces = [
    [[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]], [[x1,y0,z0],[x0,y0,z0],[x0,y1,z0],[x1,y1,z0]],
    [[x0,y1,z0],[x0,y1,z1],[x1,y1,z1],[x1,y1,z0]], [[x0,y0,z0],[x1,y0,z0],[x1,y0,z1],[x0,y0,z1]],
    [[x1,y0,z1],[x1,y0,z0],[x1,y1,z0],[x1,y1,z1]], [[x0,y0,z0],[x0,y0,z1],[x0,y1,z1],[x0,y1,z0]]];
  const ns = [[0,0,1],[0,0,-1],[0,1,0],[0,-1,0],[1,0,0],[-1,0,0]];
  faces.forEach((fc, i) => { const q = f(fc, ns[i]); pushTri(b, q[0], q[1], q[2]); pushTri(b, q[0], q[2], q[3]); });
}
function draw(b, vp, texture, mode, tint) {
  if (!b.d.length) return;
  const buf = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, buf);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(b.d), gl.STREAM_DRAW);
  const loc = n => gl.getAttribLocation(prog, n);
  gl.enableVertexAttribArray(loc('p')); gl.vertexAttribPointer(loc('p'), 3, gl.FLOAT, false, 32, 0);
  gl.enableVertexAttribArray(loc('uv')); gl.vertexAttribPointer(loc('uv'), 2, gl.FLOAT, false, 32, 12);
  gl.enableVertexAttribArray(loc('n')); gl.vertexAttribPointer(loc('n'), 3, gl.FLOAT, false, 32, 20);
  gl.uniformMatrix4fv(gl.getUniformLocation(prog, 'vp'), false, new Float32Array(vp));
  gl.uniform1i(gl.getUniformLocation(prog, 'mode'), mode || 0);
  gl.uniform3fv(gl.getUniformLocation(prog, 'tint'), new Float32Array(tint || [1, 1, 1]));
  if (texture) gl.bindTexture(gl.TEXTURE_2D, texture);
  gl.drawArrays(gl.TRIANGLES, 0, b.d.length / 8);
  gl.deleteBuffer(buf);
}
function ground(vp, cx, cz) {
  const g = batch(), g2 = batch();
  for (let x = -8; x < 8; x++) for (let z = -8; z < 8; z++) {
    const bb = ((x + z) & 1) ? g : g2; addBox(bb, x, -0.02, z, x + 1, 0, z + 1);
  }
  draw(g, vp, null, 1, [0.36, 0.55, 0.30]); draw(g2, vp, null, 1, [0.40, 0.60, 0.33]);
}
function dummy(vp) {
  const b = batch(); addBox(b, -0.3, 0, 2.3, 0.3, 1.8, 2.7); draw(b, vp, null, 1, [0.55, 0.35, 0.3]);
}

// ---------------------------------------------------------------- animation application (mirror of Java)
function lerp(a, b, w) { return a + (b - a) * w; }
function decomposeZYX(m) {
  const b = Math.asin(Math.max(-1, Math.min(1, -m[2])));
  return { x: Math.atan2(m[6], m[10]), y: b, z: Math.atan2(m[1], m[0]) };
}
function setRot(part, e) { part.xRot = e.x; part.yRot = e.y; part.zRot = e.z; }
// item chain in arm-local space (blocks) for the third person right hand
function handChain(quinque, wrist) {
  const ps = new PoseStack();
  ps.rotX(-90 * DEG); ps.rotY(180 * DEG); ps.translate(1 / 16, 0.125, -0.625);
  applyDisplay(ps, ITEM_DISPLAY.thirdperson_righthand);
  ps.translate(-0.5, -0.5, -0.5);
  quinqueRenderer(ps, 1.0, quinque.flip, wrist);
  return ps.m;
}
function applyThird(m, s, quinque, crouch) {
  const w = s.weight, P = s.pose, side = 1;
  m.attackTime = 0;
  const baseArm = [0, (m.rightArm.xRot * 0.5 - Math.PI / 10) / DEG, 0];
  const arm = [0, 1, 2].map(i => lerp(baseArm[i], P.arm[i], w));
  const body = P.body[0] * w * DEG * side;
  m.body.yRot = body;
  const sn = Math.sin(body), cs = Math.cos(body);
  m.rightArm.z = sn * 5; m.rightArm.x = -cs * 5; m.leftArm.z = -sn * 5; m.leftArm.x = cs * 5;
  const R = mul(RY(body), mul(RY(arm[0] * DEG * side), mul(RX(arm[1] * DEG), RY(arm[2] * DEG * side))));
  setRot(m.rightArm, decomposeZYX(R));
  m.rightLeg.xRot = lerp(m.rightLeg.xRot, P.legs[0] * DEG, w);
  m.leftLeg.xRot = lerp(m.leftLeg.xRot, P.legs[1] * DEG, w);
  const wrist = P.wrist.map(v => v * w);
  if (s.grip) {
    const armY = crouch ? 5 : 2;
    const Marm = mul(T(m.rightArm.x / 16, armY / 16, m.rightArm.z / 16), R);
    const H = mul(Marm, handChain(quinque, wrist));
    const sh = [m.leftArm.x, armY, m.leftArm.z];
    let best = null;
    for (let i = 0; i <= 24; i++) {
      const d = s.grip[0] + (s.grip[1] - s.grip[0]) * i / 24;
      const p = apply(H, [0, d, 0]).map(v => v * 16);
      const dist = Math.hypot(p[0] - sh[0], p[1] - sh[1], p[2] - sh[2]);
      const err = Math.abs(dist - 9.5);
      if (!best || err < best.err) best = { err, p };
    }
    const dir = norm(sub(best.p, sh));
    const pitch = -Math.acos(Math.max(-1, Math.min(1, dir[1])));
    const yaw = Math.atan2(-dir[0], -dir[2]);
    const e = decomposeZYX(mul(RY(yaw), RX(pitch)));
    m.leftArm.xRot = lerp(m.leftArm.xRot, e.x, w); m.leftArm.yRot = lerp(m.leftArm.yRot, e.y, w); m.leftArm.zRot = lerp(m.leftArm.zRot, e.z, w);
  }
  return wrist;
}
function fpTransform(s) {
  return ps => {
    ps.translate(0.56, -0.52, -0.72);
    if (!s) return;
    const w = s.weight, p = s.pose.fp_pos.map(v => v * w), r = s.pose.fp_rot.map(v => v * w);
    ps.translate(p[0], p[1], p[2]);
    ps.rotY(r[1] * DEG); ps.rotX(r[0] * DEG); ps.rotZ(r[2] * DEG);
  };
}
function sampleMove(qname, move, t, duration) {
  const q = QUINQUES[qname];
  let key = q.style + ':' + move;
  if (move === 'strong' && q.ranged && COMPILED[q.style + ':strong_bolt']) key = q.style + ':strong_bolt';
  const c = COMPILED[key]; if (!c) return null;
  const rate = move === 'flurry' ? c.length / (2 * ({kura: 2, narukami: 3, jason: 5, ixa: 3}[qname])) : 1;
  return QA.sample(c, t, duration, rate);
}

// job: {view, quinque, move, t, duration, size, open, label}
function render(job) {
  const cv = document.getElementById('c'); const W = job.w || 360, H = job.h || 360;
  cv.width = W; cv.height = H; gl.viewport(0, 0, W, H);
  gl.enable(gl.DEPTH_TEST); gl.disable(gl.CULL_FACE);
  gl.clearColor(0.62, 0.75, 0.95, 1); gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
  const q = QUINQUES[job.quinque];
  const s = job.move ? sampleMove(job.quinque, job.move, job.t, job.duration || 0) : null;
  const meshName = job.open && meshes[q.mesh + '_open'] ? q.mesh + '_open' : q.mesh;
  if (job.view === 'fp') {
    const vpWorld = mul(persp(job.fov || 70, W / H, 0.05, 100), lookAt([0, 1.62, 0], [0, 1.62 - (job.pitch || 0) * 0.02, 1], [0, 1, 0]));
    ground(vpWorld); dummy(vpWorld);
    gl.clear(gl.DEPTH_BUFFER_BIT);
    // hand: camera space; Minecraft camera looks down -Z with +X right
    const vpHand = persp(70, W / H, 0.05, 100);
    const M = firstPersonItem(fpTransform(s), q);
    const b = batch(); addMesh(b, M, meshes[meshName]); draw(b, vpHand, tex.palette, 0);
    return cv.toDataURL('image/png');
  }
  const views = {
    front: { eye: [-2.6, 1.7, 3.4], at: [0, 1.1, 0.4] },
    side: { eye: [-4.4, 1.3, 0.6], at: [0, 1.15, 0.6] },
    back: { eye: [0.9, 2.6, -4.2], at: [0, 1.3, 1.0] },
    top: { eye: [0.01, 6.5, 0.6], at: [0, 1.0, 0.6] },
    left: { eye: [2.8, 1.6, 2.6], at: [0, 1.15, 0.5] },
    kback: { eye: [0.0, 1.7, -4.6], at: [0, 1.6, 0] },
    kback34: { eye: [-3.0, 2.2, -3.6], at: [0, 1.6, 0] },
    kside: { eye: [-4.8, 1.8, 0.0], at: [0, 1.6, 0] },
    kfront34: { eye: [-3.2, 1.9, 3.6], at: [0, 1.6, 0] },
    kf5: { eye: [0.0, 3.0, -4.0], at: [0, 1.7, 0.5] },
    close: { eye: [-1.2, 1.5, 2.2], at: [0, 1.2, 0.3] },
  };
  const v = views[job.view] || views.front;
  const vp = mul(persp(job.fov || 50, W / H, 0.05, 100), lookAt(v.eye, v.at, job.view === 'top' ? [0, 0, 1] : [0, 1, 0]));
  ground(vp); dummy(vp);
  const model = playerModel(job.skin !== 'ghoul');
  let wrist = null;
  setupAnim(model, { age: (job.t || 0) + 20, customPose: s ? (mm => { wrist = applyThird(mm, s, q, false); }) : null });
  const root = entityRoot(0);
  const b = batch();
  for (const name of ['head', 'hat', 'body', 'jacket', 'rightArm', 'rightSleeve', 'leftArm', 'leftSleeve', 'rightLeg', 'rightPants', 'leftLeg', 'leftPants'])
    addModelPart(b, root, model[name]);
  draw(b, vp, job.skin === 'ghoul' ? tex.ghoul : tex.skin, 0);
  const M = thirdPersonItem(root, model, q, wrist);
  if (!job.noQuinque) { const qb = batch(); addMesh(qb, M, meshes[meshName]); draw(qb, vp, tex.palette, 0); }
  if (job.kagune && KAGUNE[job.kagune]) {
    const k = KAGUNE[job.kagune], kb = batch(), kps = kaguneMatrix(root, model);
    for (const bone of k.geo.roots) geoAddBone(kb, kps, bone);
    draw(kb, vp, k.tex, job.glow ? 2 : 0);
  }
  return cv.toDataURL('image/png');
}
const KAGUNE = {};
async function loadKagune(name) {
  const g = await (await fetch('assets/' + name + '.geo.json?' + Math.random())).json();
  KAGUNE[name] = { geo: geoLoad(g), tex: makeTex(await loadImage('assets/' + name + '.png?' + Math.random())) };
  return 'ok';
}
window.loadKagune = loadKagune;
window.init = init; window.render = render; window.setAnims = setAnims;
