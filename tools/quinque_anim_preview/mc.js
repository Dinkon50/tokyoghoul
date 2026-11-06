// Minecraft 1.20.1 rendering math, replicated for previewing quinque animations.
// Column-major 4x4 matrices, PoseStack semantics (every op post-multiplies).
'use strict';
const DEG = Math.PI / 180;
function I() { return [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1]; }
function mul(a, b) {
  const o = new Array(16);
  for (let c = 0; c < 4; c++) for (let r = 0; r < 4; r++) {
    let s = 0; for (let k = 0; k < 4; k++) s += a[k*4+r] * b[c*4+k]; o[c*4+r] = s;
  }
  return o;
}
function T(x, y, z) { const m = I(); m[12] = x; m[13] = y; m[14] = z; return m; }
function S(x, y, z) { const m = I(); m[0] = x; m[5] = y; m[10] = z; return m; }
function RX(a) { const c = Math.cos(a), s = Math.sin(a); return [1,0,0,0, 0,c,s,0, 0,-s,c,0, 0,0,0,1]; }
function RY(a) { const c = Math.cos(a), s = Math.sin(a); return [c,0,-s,0, 0,1,0,0, s,0,c,0, 0,0,0,1]; }
function RZ(a) { const c = Math.cos(a), s = Math.sin(a); return [c,s,0,0, -s,c,0,0, 0,0,1,0, 0,0,0,1]; }
function apply(m, p) {
  const x = p[0], y = p[1], z = p[2], w = p.length > 3 ? p[3] : 1;
  return [m[0]*x+m[4]*y+m[8]*z+m[12]*w, m[1]*x+m[5]*y+m[9]*z+m[13]*w, m[2]*x+m[6]*y+m[10]*z+m[14]*w];
}
class PoseStack {
  constructor(m) { this.s = [m || I()]; }
  get m() { return this.s[this.s.length - 1]; }
  set m(v) { this.s[this.s.length - 1] = v; }
  push() { this.s.push(this.m.slice()); }
  pop() { this.s.pop(); }
  translate(x, y, z) { this.m = mul(this.m, T(x, y, z)); }
  scale(x, y, z) { this.m = mul(this.m, S(x, y, z)); }
  rotX(a) { this.m = mul(this.m, RX(a)); }
  rotY(a) { this.m = mul(this.m, RY(a)); }
  rotZ(a) { this.m = mul(this.m, RZ(a)); }
  // Quaternionf.rotationZYX(z, y, x) = Rz * Ry * Rx
  rotZYX(z, y, x) { this.m = mul(this.m, mul(RZ(z), mul(RY(y), RX(x)))); }
  // Quaternionf.rotationXYZ(x, y, z) = Rx * Ry * Rz
  rotXYZ(x, y, z) { this.m = mul(this.m, mul(RX(x), mul(RY(y), RZ(z)))); }
}

// ---------------------------------------------------------------- ModelPart
class Part {
  constructor(x, y, z) { this.x = x; this.y = y; this.z = z; this.xRot = 0; this.yRot = 0; this.zRot = 0; this.cubes = []; }
  copyFrom(o) { this.x = o.x; this.y = o.y; this.z = o.z; this.xRot = o.xRot; this.yRot = o.yRot; this.zRot = o.zRot; }
  translateAndRotate(ps) {
    ps.translate(this.x / 16, this.y / 16, this.z / 16);
    if (this.xRot || this.yRot || this.zRot) ps.rotZYX(this.zRot, this.yRot, this.xRot);
  }
}
// ModelPart.Cube box UV (1.20.1)
function cube(u, v, ox, oy, oz, dx, dy, dz, grow, mirror) {
  let x0 = ox - grow, y0 = oy - grow, z0 = oz - grow;
  let x1 = ox + dx + grow, y1 = oy + dy + grow, z1 = oz + dz + grow;
  if (mirror) { const t = x1; x1 = x0; x0 = t; }
  const v7 = [x0, y0, z0], v0 = [x1, y0, z0], v1 = [x1, y1, z0], v2 = [x0, y1, z0];
  const v3 = [x0, y0, z1], v4 = [x1, y0, z1], v5 = [x1, y1, z1], v6 = [x0, y1, z1];
  const f4 = u, f5 = u + dz, f6 = u + dz + dx, f7 = u + dz + dx + dx, f8 = u + dz + dx + dz, f9 = u + dz + dx + dz + dx;
  const f10 = v, f11 = v + dz, f12 = v + dz + dy;
  const polys = [];
  function poly(vs, u1, v1_, u2, v2_, n) {
    let q = [[...vs[0], u2, v1_], [...vs[1], u1, v1_], [...vs[2], u1, v2_], [...vs[3], u2, v2_]];
    if (mirror) { q = q.reverse(); n = [-n[0], n[1], n[2]]; }
    polys.push({ q, n });
  }
  poly([v4, v3, v7, v0], f5, f10, f6, f11, [0, -1, 0]);
  poly([v1, v2, v6, v5], f6, f11, f7, f10, [0, 1, 0]);
  poly([v7, v3, v6, v2], f4, f11, f5, f12, [-1, 0, 0]);
  poly([v0, v7, v2, v1], f5, f11, f6, f12, [0, 0, -1]);
  poly([v4, v0, v1, v5], f6, f11, f8, f12, [1, 0, 0]);
  poly([v3, v4, v5, v6], f8, f11, f9, f12, [0, 0, 1]);
  return polys;
}
function playerModel(slim) {
  const p = {};
  p.head = new Part(0, 0, 0); p.head.cubes = cube(0, 0, -4, -8, -4, 8, 8, 8, 0);
  p.hat = new Part(0, 0, 0); p.hat.cubes = cube(32, 0, -4, -8, -4, 8, 8, 8, 0.5);
  p.body = new Part(0, 0, 0); p.body.cubes = cube(16, 16, -4, 0, -2, 8, 12, 4, 0);
  p.jacket = new Part(0, 0, 0); p.jacket.cubes = cube(16, 32, -4, 0, -2, 8, 12, 4, 0.25);
  const aw = slim ? 3 : 4, ay = slim ? 2.5 : 2;
  p.rightArm = new Part(-5, ay, 0); p.rightArm.cubes = cube(40, 16, slim ? -2 : -3, -2, -2, aw, 12, 4, 0);
  p.rightSleeve = new Part(-5, ay, 0); p.rightSleeve.cubes = cube(40, 32, slim ? -2 : -3, -2, -2, aw, 12, 4, 0.25);
  p.leftArm = new Part(5, ay, 0); p.leftArm.cubes = cube(32, 48, -1, -2, -2, aw, 12, 4, 0);
  p.leftSleeve = new Part(5, ay, 0); p.leftSleeve.cubes = cube(48, 48, -1, -2, -2, aw, 12, 4, 0.25);
  p.rightLeg = new Part(-1.9, 12, 0); p.rightLeg.cubes = cube(0, 16, -2, 0, -2, 4, 12, 4, 0);
  p.rightPants = new Part(-1.9, 12, 0); p.rightPants.cubes = cube(0, 32, -2, 0, -2, 4, 12, 4, 0.25);
  p.leftLeg = new Part(1.9, 12, 0); p.leftLeg.cubes = cube(16, 48, -2, 0, -2, 4, 12, 4, 0);
  p.leftPants = new Part(1.9, 12, 0); p.leftPants.cubes = cube(0, 48, -2, 0, -2, 4, 12, 4, 0.25);
  p.slim = slim;
  return p;
}

// HumanoidModel.setupAnim for a player standing still (limbSwing 0), holding an item in the main hand.
// customPose: function(model, arm) like Forge IArmPoseTransformer (or null for ArmPose.ITEM).
function setupAnim(m, opts) {
  const age = opts.age || 0, walk = opts.walk || 0, limb = opts.limb || 0;
  m.head.xRot = (opts.headPitch || 0) * DEG; m.head.yRot = 0; m.head.zRot = 0;
  m.body.yRot = 0; m.body.zRot = 0;
  m.rightArm.z = 0; m.rightArm.x = -5; m.leftArm.z = 0; m.leftArm.x = 5;
  m.rightArm.xRot = Math.cos(limb * 0.6662 + Math.PI) * 2 * walk * 0.5;
  m.leftArm.xRot = Math.cos(limb * 0.6662) * 2 * walk * 0.5;
  m.rightArm.yRot = m.leftArm.yRot = 0; m.rightArm.zRot = m.leftArm.zRot = 0;
  m.rightLeg.xRot = Math.cos(limb * 0.6662) * 1.4 * walk;
  m.leftLeg.xRot = Math.cos(limb * 0.6662 + Math.PI) * 1.4 * walk;
  m.rightLeg.yRot = m.leftLeg.yRot = 0; m.rightLeg.zRot = m.leftLeg.zRot = 0;
  m.attackTime = 0;
  // poseLeftArm (EMPTY) then poseRightArm (ITEM or custom)
  if (opts.customPose) opts.customPose(m, 'right');
  else { m.rightArm.xRot = m.rightArm.xRot * 0.5 - Math.PI / 10; m.rightArm.yRot = 0; }
  // not crouching
  m.body.xRot = 0; m.rightLeg.z = 0.1; m.leftLeg.z = 0.1; m.rightLeg.y = 12; m.leftLeg.y = 12;
  m.head.y = 0; m.body.y = 0; m.leftArm.y = 2; m.rightArm.y = 2;
  // bobModelPart
  const bob = (part, s) => { part.zRot += s * (Math.cos(age * 0.09) * 0.05 + 0.05); part.xRot += s * Math.sin(age * 0.067) * 0.05; };
  bob(m.rightArm, 1); bob(m.leftArm, -1);
  m.hat.copyFrom(m.head); m.jacket.copyFrom(m.body);
  m.rightSleeve.copyFrom(m.rightArm); m.leftSleeve.copyFrom(m.leftArm);
  m.rightPants.copyFrom(m.rightLeg); m.leftPants.copyFrom(m.leftLeg);
}

// ---------------------------------------------------------------- items
const ITEM_DISPLAY = {
  thirdperson_righthand: { rot: [0, -90, 55], tr: [0, 4.0, 0.5], sc: 0.85 },
  firstperson_righthand: { rot: [0, -90, 25], tr: [1.13, 3.2, 1.13], sc: 0.68 },
};
function applyDisplay(ps, d) {
  ps.translate(d.tr[0] / 16, d.tr[1] / 16, d.tr[2] / 16);
  ps.rotXYZ(d.rot[0] * DEG, d.rot[1] * DEG, d.rot[2] * DEG);
  ps.scale(d.sc, d.sc, d.sc);
}
const GRIP = 3.5 / 16;
// QuinqueRenderer.renderByItem up to the mesh (wrist = extra rotation at the grip, third person only)
function quinqueRenderer(ps, scale, flip, wrist) {
  ps.translate(GRIP, GRIP, 0.5);
  ps.rotZ(-45 * DEG);
  if (wrist) ps.rotZYX(wrist[2] * DEG, wrist[1] * DEG, wrist[0] * DEG);
  ps.scale(scale, scale, scale);
  if (flip) ps.rotY(Math.PI);
}
// ItemInHandLayer + ItemRenderer (third person right hand) starting from model space
function thirdPersonItem(ps, model, quinque, wrist) {
  ps.push();
  const arm = model.rightArm;
  if (model.slim) { arm.x += 0.5; arm.translateAndRotate(ps); arm.x -= 0.5; } else arm.translateAndRotate(ps);
  ps.rotX(-90 * DEG); ps.rotY(180 * DEG);
  ps.translate(1 / 16, 0.125, -0.625);
  applyDisplay(ps, ITEM_DISPLAY.thirdperson_righthand);
  ps.translate(-0.5, -0.5, -0.5);
  quinqueRenderer(ps, 1.0, quinque.flip, wrist);
  const m = ps.m; ps.pop(); return m;
}
// ItemInHandRenderer first person (camera space), with an optional Forge hand transform
function firstPersonItem(handTransform, quinque) {
  const ps = new PoseStack();
  if (handTransform) handTransform(ps);
  else ps.translate(0.56, -0.52, -0.72);
  applyDisplay(ps, ITEM_DISPLAY.firstperson_righthand);
  ps.translate(-0.5, -0.5, -0.5);
  quinqueRenderer(ps, 0.5, quinque.flip, null);
  return ps.m;
}
// LivingEntityRenderer: world -> model space for a player at origin facing +Z (bodyRot 0)
function entityRoot(bodyRotDeg) {
  const ps = new PoseStack();
  ps.rotY((180 - bodyRotDeg) * DEG);
  ps.scale(-1, -1, 1);
  ps.scale(0.9375, 0.9375, 0.9375);
  ps.translate(0, -1.501, 0);
  return ps;
}
if (typeof module !== 'undefined') module.exports = { DEG, I, mul, T, S, RX, RY, RZ, apply, PoseStack, Part, playerModel, setupAnim, thirdPersonItem, firstPersonItem, entityRoot, quinqueRenderer, applyDisplay, ITEM_DISPLAY, GRIP };
