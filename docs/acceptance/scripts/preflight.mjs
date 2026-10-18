import { existsSync, mkdirSync, writeFileSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { resolve, dirname } from 'node:path'

const root = fileURLToPath(new URL('../../../', import.meta.url))
const output = process.argv[2] ? resolve(process.argv[2]) : null
const commandExists = name => {
  const result = spawnSync(process.platform === 'win32' ? 'where.exe' : 'which', [name], { encoding: 'utf8', windowsHide: true, timeout: 3000 })
  return { availableInPath: result.status === 0 }
}
const probes = [
  { name: 'backend-knowledge-overview', url: 'http://localhost:10002/api/knowledge/overview', method: 'POST' },
  ...[20002, 3100, 5173].map(port => ({ name: `frontend-${port}`, url: `http://localhost:${port}/`, method: 'GET' }))
]
const results = await Promise.all(probes.map(async probe => {
  try {
    const response = await fetch(probe.url, { method: probe.method, redirect: 'error', signal: AbortSignal.timeout(3500), ...(probe.method === 'POST' ? { headers: { 'Content-Type': 'application/json' }, body: '{}' } : {}) })
    const type = response.headers.get('content-type') || ''
    let appCode = null
    let validService = false
    if (probe.method === 'POST' && type.includes('application/json')) {
      const payload = await response.json()
      appCode = payload.code
      validService = [0, 200].includes(appCode) && typeof payload.data?.productCount === 'number'
    } else if (probe.method === 'GET') {
      const html = await response.text()
      validService = type.includes('text/html') && html.includes('id="app"') && html.includes('/src/main.ts')
    }
    return { ...probe, reachable: true, httpStatus: response.status, contentType: type, appCode, validService, status: response.ok && validService ? 'PASS' : 'FAIL' }
  } catch (error) {
    return { ...probe, reachable: false, status: 'BLOCKED', reason: error.cause?.code || error.name }
  }
}))
const revision = spawnSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8', windowsHide: true, timeout: 3000 })
const report = {
  generatedAt: new Date().toISOString(), localTime: new Date().toLocaleString('sv-SE', { timeZone: 'Asia/Shanghai' }) + ' Asia/Shanghai',
  revision: revision.stdout?.trim() || 'unknown', scope: 'READ_ONLY_ENVIRONMENT_CHECK; NOT AC-01–AC-22 acceptance',
  tools: { node: { availableInPath: true, version: process.version }, ...Object.fromEntries(['java', 'mvn', 'pnpm', 'mysql', 'ffmpeg'].map(name => [name, commandExists(name)])) },
  files: Object.fromEntries(['frontend/node_modules', 'backend/target', 'backend/src/main/resources/application-local.yml', 'frontend/.env', 'frontend/.env.local'].map(p => [p, existsSync(resolve(root, p))])),
  services: results
}
if (output) { mkdirSync(dirname(output), { recursive: true }); writeFileSync(output, JSON.stringify(report, null, 2) + '\n') }
console.log(JSON.stringify(report, null, 2))
if (!results.some(r => r.name === 'backend-knowledge-overview' && r.status === 'PASS') || !results.some(r => r.name.startsWith('frontend-') && r.status === 'PASS')) process.exitCode = 2
