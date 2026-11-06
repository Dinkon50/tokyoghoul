// Quinque animation system. Java port: client/quinque/QuinqueAnim.java (keep both in sync).
'use strict';
(function (root) {
  const DEG = Math.PI / 180;
  const CHANNELS = { arm: 3, body: 1, wrist: 3, legs: 2, fp_pos: 3, fp_rot: 3 };
  const REST = { arm: [0, -18, 0], body: [0], wrist: [0, 0, 0], legs: [0, 0], fp_pos: [0, 0, 0], fp_rot: [0, 0, 0] };

  // Steffen monotone cubic: no overshoot, stops on extremes, smooth elsewhere.
  function tangents(ts, ys, loop, period) {
    const n = ts.length, m = new Array(n).fill(0);
    for (let i = 0; i < n; i++) {
      let i0 = i - 1, i1 = i + 1, t0, t1, y0, y1;
      if (i0 < 0) { if (!loop) continue; t0 = ts[n - 2] - period; y0 = ys[n - 2]; } else { t0 = ts[i0]; y0 = ys[i0]; }
      if (i1 >= n) { if (!loop) continue; t1 = ts[1] + period; y1 = ys[1]; } else { t1 = ts[i1]; y1 = ys[i1]; }
      const h0 = ts[i] - t0, h1 = t1 - ts[i];
      if (h0 <= 0 || h1 <= 0) continue;
      const d0 = (ys[i] - y0) / h0, d1 = (y1 - ys[i]) / h1;
      const p = (d0 * h1 + d1 * h0) / (h0 + h1);
      m[i] = (Math.sign(d0) + Math.sign(d1)) * Math.min(Math.abs(d0), Math.abs(d1), 0.5 * Math.abs(p));
    }
    return m;
  }

  function compile(move) {
    const keys = move.keys;
    const out = { length: move.length, loop: !!move.loop, fadeIn: move.fade_in != null ? move.fade_in : 2,
      fadeOut: move.fade_out != null ? move.fade_out : 3, grip: move.grip || null,
      anchor: move.anchor || 0, align: move.align || 0, ts: keys.map(k => k.t),
      ease: keys.map(k => k.ease || 'smooth'), ch: {} };
    for (const name in CHANNELS) {
      const dims = CHANNELS[name], series = [];
      let last = REST[name];
      const vals = keys.map(k => { if (k[name] != null) last = Array.isArray(k[name]) ? k[name] : [k[name]]; return last; });
      for (let d = 0; d < dims; d++) {
        const ys = vals.map(v => v[d]);
        series.push({ ys, ms: tangents(out.ts, ys, out.loop, out.length) });
      }
      out.ch[name] = series;
    }
    return out;
  }

  function sampleSeries(c, s, t) {
    const ts = c.ts, n = ts.length;
    if (t <= ts[0]) return s.ys[0];
    if (t >= ts[n - 1]) return s.ys[n - 1];
    let i = 0; while (i < n - 2 && t > ts[i + 1]) i++;
    const h = ts[i + 1] - ts[i], u = (t - ts[i]) / h, y0 = s.ys[i], y1 = s.ys[i + 1];
    const ease = c.ease[i];
    if (ease === 'linear') return y0 + (y1 - y0) * u;
    if (ease === 'fast') { const f = 1 - Math.pow(1 - u, 3); return y0 + (y1 - y0) * f; }
    if (ease === 'hold') return y0;
    const u2 = u * u, u3 = u2 * u;
    return (2*u3 - 3*u2 + 1) * y0 + (u3 - 2*u2 + u) * h * s.ms[i] + (-2*u3 + 3*u2) * y1 + (u3 - u2) * h * s.ms[i + 1];
  }

  function smooth(x) { x = Math.max(0, Math.min(1, x)); return x * x * (3 - 2 * x); }

  // pose values at time t (ticks) and blend weight
  // rate: loop speed (flurry: length / (2 * hit interval)); anchor (move time) lines up with align (real ticks)
  function sample(c, t, duration, rate) {
    const len = duration > 0 ? duration : c.length;
    if (t < 0 || t >= len) return null;
    let lt;
    if (c.loop) { lt = ((t - c.align) * (rate || 1) + c.anchor) % c.length; if (lt < 0) lt += c.length; }
    else lt = Math.min(t, c.length);
    const pose = {};
    for (const name in c.ch) pose[name] = c.ch[name].map(s => sampleSeries(c, s, lt));
    const w = smooth(c.fadeIn > 0 ? t / c.fadeIn : 1) * smooth(c.fadeOut > 0 ? (len - t) / c.fadeOut : 1);
    return { pose, weight: w, grip: c.grip };
  }

  root.QuinqueAnimSys = { CHANNELS, REST, compile, sample, DEG };
})(typeof window !== 'undefined' ? window : module.exports);
