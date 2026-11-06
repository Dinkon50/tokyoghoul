// usage: node run.js jobs.json outdir   (jobs: [{name, view, quinque, move, t, ...}])
const http = require('http'), fs = require('fs'), path = require('path');
const { chromium } = require('/opt/npm-tools/node_modules/playwright-core');
const root = __dirname;
const [jobsFile, outDir] = process.argv.slice(2);
const types = { '.html': 'text/html', '.js': 'text/javascript', '.png': 'image/png', '.bin': 'application/octet-stream', '.json': 'application/json' };
const server = http.createServer((req, res) => {
  const f = path.join(root, decodeURIComponent(req.url.split('?')[0]));
  fs.readFile(f, (e, d) => { if (e) { res.writeHead(404); res.end(); return; } res.writeHead(200, { 'Content-Type': types[path.extname(f)] || 'application/octet-stream' }); res.end(d); });
});
server.listen(0, async () => {
  const port = server.address().port;
  const browser = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
  const page = await browser.newPage();
  page.on('console', m => console.log('[page]', m.text()));
  page.on('pageerror', e => console.log('[pageerror]', e.message));
  await page.goto(`http://127.0.0.1:${port}/index.html`);
  const anim = JSON.parse(fs.readFileSync(process.env.ANIM || path.join(root, 'quinque_animations.json'), 'utf8'));
  await page.evaluate(a => window.init(a), anim);
  const jobs = JSON.parse(fs.readFileSync(jobsFile, 'utf8'));
  for (const k of [...new Set(jobs.map(j => j.kagune).filter(Boolean))]) await page.evaluate(n => window.loadKagune(n), k);
  fs.mkdirSync(outDir, { recursive: true });
  for (const job of jobs) {
    const url = await page.evaluate(j => window.render(j), job);
    fs.writeFileSync(path.join(outDir, job.name + '.png'), Buffer.from(url.split(',')[1], 'base64'));
  }
  await browser.close(); server.close();
});
