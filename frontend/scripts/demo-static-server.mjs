import { createServer, request } from 'node:http'
import { readFile, stat } from 'node:fs/promises'
import { dirname, extname, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

// Local verification only. Public deployments need an authenticated HTTPS reverse proxy.
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../dist')
await stat(resolve(root, 'index.html'))
const types = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json', '.svg': 'image/svg+xml',
  '.png': 'image/png', '.jpg': 'image/jpeg', '.webp': 'image/webp', '.ico': 'image/x-icon',
  '.woff': 'font/woff', '.woff2': 'font/woff2' }
createServer(async (incoming, outgoing) => {
  const url = new URL(incoming.url, 'http://127.0.0.1:20003')
  if (url.pathname === '/api' || url.pathname.startsWith('/api/')) {
    const upstream = request({ hostname: '127.0.0.1', port: 10002, path: url.pathname + url.search,
      method: incoming.method, headers: { ...incoming.headers, host: '127.0.0.1:10002' } }, response => {
      outgoing.writeHead(response.statusCode, response.headers)
      response.pipe(outgoing)
    })
    upstream.setTimeout(65000, () => upstream.destroy(new Error('Upstream timeout')))
    upstream.on('error', () => {
      if (outgoing.headersSent) return outgoing.destroy()
      outgoing.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' })
      outgoing.end(JSON.stringify({ code: 502, msg: '本地后端10002不可用，请检查启动状态' }))
    })
    incoming.on('aborted', () => upstream.destroy())
    incoming.pipe(upstream)
    return
  }
  if (!['GET', 'HEAD'].includes(incoming.method)) {
    outgoing.writeHead(405, { Allow: 'GET, HEAD' }); outgoing.end(); return
  }
  try {
    const path = decodeURIComponent(url.pathname)
    let file = resolve(root, '.' + (path === '/' ? '/index.html' : path))
    if (!file.startsWith(root + sep)) throw new Error('Invalid path')
    if (!extname(file)) file = resolve(root, 'index.html')
    const content = await readFile(file)
    outgoing.writeHead(200, { 'Content-Type': types[extname(file)] || 'application/octet-stream',
      'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' })
    outgoing.end(incoming.method === 'HEAD' ? undefined : content)
  } catch {
    outgoing.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' }); outgoing.end('Not found')
  }
}).listen(20003, '127.0.0.1', () => console.log('Static demo: http://127.0.0.1:20003 → API 127.0.0.1:10002'))
