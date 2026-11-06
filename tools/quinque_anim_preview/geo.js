'use strict';
// GeckoLib 4 geo.json loader + renderer (bones with pivots/rotations, cubes with per-face or box UV).
// Mirrors BakedModelFactory (x negated for pivots/origins, rotations (-x, -y, z)) and GeoRenderer.renderRecursively.
function geoLoad(json) {
  const geo = json['minecraft:geometry'][0];
  const tw = geo.description.texture_width, th = geo.description.texture_height;
  const bones = {}, roots = [];
  for (const b of geo.bones) {
    const p = b.pivot || [0, 0, 0], r = b.rotation || [0, 0, 0];
    bones[b.name] = { name: b.name, parent: b.parent, pivot: [-p[0], p[1], p[2]], rot: [-r[0] * DEG, -r[1] * DEG, r[2] * DEG],
      cubes: (b.cubes || []).map(c => cubeOf(c, tw, th)), children: [] };
  }
  for (const b of geo.bones) { if (b.parent && bones[b.parent]) bones[b.parent].children.push(bones[b.name]); else roots.push(bones[b.name]); }
  return { roots, tw, th };
}
function cubeOf(c, tw, th) {
  const o = c.origin, s = c.size, inf = c.inflate || 0;
  const x0 = -(o[0] + s[0]) - inf, x1 = -o[0] + inf, y0 = o[1] - inf, y1 = o[1] + s[1] + inf, z0 = o[2] - inf, z1 = o[2] + s[2] + inf;
  const p = c.pivot || [0, 0, 0], r = c.rotation || [0, 0, 0];
  return { box: [x0, y0, z0, x1, y1, z1], pivot: [-p[0], p[1], p[2]], rot: [-r[0] * DEG, -r[1] * DEG, r[2] * DEG], uv: c.uv, size: s, tw, th };
}
// face quads in GeckoLib vertex order is not critical here (uniform swatches); keep sensible orientation
function faceUV(c, face) {
  let u, v, w, h;
  if (Array.isArray(c.uv)) { // box uv
    const [U, V] = c.uv, [sx, sy, sz] = c.size.map(Math.floor);
    const m = { north: [U + sz, V + sz, sx, sy], south: [U + sz + sx + sz, V + sz, sx, sy], east: [U, V + sz, sz, sy],
      west: [U + sz + sx, V + sz, sz, sy], up: [U + sz, V, sx, sz], down: [U + sz + sx, V, sx, sz] }[face];
    [u, v, w, h] = m;
  } else {
    const f = c.uv && c.uv[face]; if (!f) return null;
    [u, v] = f.uv; [w, h] = f.uv_size;
  }
  return [u / c.tw, v / c.th, (u + w) / c.tw, (v + h) / c.th];
}
function geoAddBone(b, ps, bone) {
  ps.push();
  ps.translate(bone.pivot[0] / 16, bone.pivot[1] / 16, bone.pivot[2] / 16);
  if (bone.rot[2]) ps.rotZ(bone.rot[2]); if (bone.rot[1]) ps.rotY(bone.rot[1]); if (bone.rot[0]) ps.rotX(bone.rot[0]);
  ps.translate(-bone.pivot[0] / 16, -bone.pivot[1] / 16, -bone.pivot[2] / 16);
  for (const c of bone.cubes) {
    ps.push();
    ps.translate(c.pivot[0] / 16, c.pivot[1] / 16, c.pivot[2] / 16);
    if (c.rot[2]) ps.rotZ(c.rot[2]); if (c.rot[1]) ps.rotY(c.rot[1]); if (c.rot[0]) ps.rotX(c.rot[0]);
    ps.translate(-c.pivot[0] / 16, -c.pivot[1] / 16, -c.pivot[2] / 16);
    const m = ps.m, [x0, y0, z0, x1, y1, z1] = c.box.map(v => v / 16);
    const F = {
      north: [[x1, y1, z0], [x0, y1, z0], [x0, y0, z0], [x1, y0, z0]], south: [[x0, y1, z1], [x1, y1, z1], [x1, y0, z1], [x0, y0, z1]],
      east: [[x0, y1, z0], [x0, y1, z1], [x0, y0, z1], [x0, y0, z0]], west: [[x1, y1, z1], [x1, y1, z0], [x1, y0, z0], [x1, y0, z1]],
      up: [[x0, y1, z0], [x1, y1, z0], [x1, y1, z1], [x0, y1, z1]], down: [[x0, y0, z1], [x1, y0, z1], [x1, y0, z0], [x0, y0, z0]] };
    for (const face in F) {
      const uv = faceUV(c, face); if (!uv) continue;
      const q = F[face].map(p => apply(m, p));
      const n = norm(cross(sub(q[1], q[0]), sub(q[2], q[0])));
      const t = [[uv[0], uv[1]], [uv[2], uv[1]], [uv[2], uv[3]], [uv[0], uv[3]]];
      const V = q.map((p, i) => [...p, t[i][0], t[i][1], ...n]);
      pushTri(b, V[0], V[1], V[2]); pushTri(b, V[0], V[2], V[3]);
    }
    ps.pop();
  }
  for (const ch of bone.children) geoAddBone(b, ps, ch);
  ps.pop();
}
// KaguneLayer: after body.translateAndRotate -> translate(0, 1.501, 0) -> scale(-1, -1, 1) -> bones
function kaguneMatrix(root, model) {
  const ps = new PoseStack(root.m.slice());
  model.body.translateAndRotate(ps);
  ps.translate(0, 1.501, 0); ps.scale(-1, -1, 1);
  return ps;
}
window.geoLoad = geoLoad; window.geoAddBone = geoAddBone; window.kaguneMatrix = kaguneMatrix;
