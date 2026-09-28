import { readFile, readdir, stat } from 'node:fs/promises'
import { extname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(fileURLToPath(new URL('.', import.meta.url)), '..')
const issues = []
const read = (path) => readFile(resolve(root, path), 'utf8')

async function readJson(path) {
  try {
    return JSON.parse(await read(path))
  } catch (error) {
    issues.push(`${path} 不是合法 JSON：${error instanceof Error ? error.message : '无法解析'}`)
    return {}
  }
}

async function listFiles(relativeDirectory, extension) {
  const directory = resolve(root, relativeDirectory)
  const entries = await readdir(directory, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const relativePath = `${relativeDirectory}/${entry.name}`
    if (entry.isDirectory()) files.push(...await listFiles(relativePath, extension))
    else if (!extension || extname(entry.name) === extension) files.push(relativePath)
  }
  return files
}

function isIpAddress(hostname) {
  return /^\d{1,3}(?:\.\d{1,3}){3}$/.test(hostname) || hostname.includes(':')
}

function hasBalancedWxmlTags(source) {
  const stack = []
  for (let cursor = 0; cursor < source.length;) {
    const start = source.indexOf('<', cursor)
    if (start < 0) break
    if (source.startsWith('<!--', start)) {
      const commentEnd = source.indexOf('-->', start + 4)
      if (commentEnd < 0) return false
      cursor = commentEnd + 3
      continue
    }
    let quote = ''
    let end = start + 1
    for (; end < source.length; end += 1) {
      const character = source[end]
      if (quote) {
        if (character === quote) quote = ''
      } else if (character === '"' || character === "'") quote = character
      else if (character === '>') break
    }
    if (end >= source.length) return false
    const raw = source.slice(start, end + 1)
    const match = raw.match(/^<\s*(\/)?([A-Za-z][\w-]*)/)
    cursor = end + 1
    if (!match) continue
    const [, closing, tag] = match
    if (/\/\s*>$/.test(raw)) continue
    if (!closing) stack.push(tag)
    else if (stack.pop() !== tag) return false
  }
  return stack.length === 0
}

const project = await readJson('project.config.json')
const app = await readJson('miniprogram/app.json')
await readJson('miniprogram/sitemap.json')
const runtime = await read('miniprogram/config/runtime.ts')
const compliance = await read('miniprogram/config/compliance.ts')
const legal = await read('miniprogram/config/legal.ts')
const welcome = await read('miniprogram/pages/welcome/index.wxml')
const welcomeLogic = await read('miniprogram/pages/welcome/index.ts')
const home = await read('miniprogram/pages/home/index.wxml')

if (!/^wx[0-9a-f]{16}$/i.test(project.appid || '')) issues.push('project.config.json 必须填写格式正确的正式 AppID')
if (project.setting?.urlCheck !== true) issues.push('project.config.json 必须开启 urlCheck')
if (project.setting?.packNpmManually !== true || !Array.isArray(project.setting?.packNpmRelationList)) {
  issues.push('project.config.json 未配置微信开发者工具 npm 构建映射')
}
if (app.__usePrivacyCheck__ !== true) issues.push('app.json 必须启用 __usePrivacyCheck__')
if (Object.keys(app.usingComponents || {}).some((name) => name.startsWith('t-'))) {
  issues.push('TDesign 组件不应全局注册，请在实际使用页面按需声明')
}

const productionApi = runtime.match(/PRODUCTION_API_BASE_URL\s*=\s*['"]([^'"]+)['"]/)?.[1]
if (!productionApi) {
  issues.push('未找到正式 API 地址配置')
} else {
  try {
    const url = new URL(productionApi)
    if (url.protocol !== 'https:') issues.push('正式 API 地址必须使用 HTTPS')
    if (url.hostname === 'localhost' || url.hostname.endsWith('.example.com') || isIpAddress(url.hostname)) {
      issues.push('正式 API 地址必须使用已备案的真实域名，不能使用占位域名、IP 或 localhost')
    }
  } catch {
    issues.push('正式 API 地址格式不正确')
  }
}

const complianceFields = {
  __REPLACE_OPERATOR_NAME__: '运营主体全称',
  __REPLACE_PRIVACY_CONTACT__: '隐私联系人',
  __REPLACE_PRIVACY_EMAIL_OR_PHONE__: '隐私联系电话或邮箱',
  __REPLACE_CONTACT_ADDRESS__: '运营主体联系地址',
  __REPLACE_APP_FILING_NUMBER__: 'APP 备案号',
}
for (const [placeholder, label] of Object.entries(complianceFields)) {
  if (compliance.includes(placeholder)) issues.push(`${label}尚未配置`)
}

if (!welcome.includes('open-type="agreePrivacyAuthorization"')) issues.push('欢迎页缺少微信隐私授权按钮')
if (!welcomeLogic.includes('wx.getPrivacySetting')) issues.push('欢迎页没有检查微信平台隐私授权状态')
if (!legal.includes('反馈与举报')) issues.push('隐私与用户协议未声明投诉举报处理')
if (!home.includes('APP 备案号')) issues.push('小程序首页底部没有展示 APP 备案号')
if (welcome.includes('儒、释、道') || welcome.includes('师门')) issues.push('首版欢迎页仍包含未确认资质的宗教或师门定位')

const privacyVersion = legal.match(/PRIVACY_VERSION\s*=\s*['"](\d{4}-\d{2}-\d{2})['"]/)?.[1]
const termsVersion = legal.match(/TERMS_VERSION\s*=\s*['"](\d{4}-\d{2}-\d{2})['"]/)?.[1]
if (!privacyVersion || !termsVersion || privacyVersion !== termsVersion) issues.push('隐私协议与用户协议版本必须使用同一个有效日期')

for (const page of ['pages/welcome/index', 'pages/privacy/index', 'pages/report/index', 'pages/legal/index', 'pages/account/index']) {
  if (!app.pages?.includes(page)) issues.push(`缺少正式版页面：${page}`)
}

const unsupportedHtmlTag = /<\/?(?:b|br|div|i|p|small|span|strong)(?:\s|\/?>)/i
for (const file of await listFiles('miniprogram/pages', '.wxml')) {
  const source = await read(file)
  if (unsupportedHtmlTag.test(source)) issues.push(`${file} 仍包含非原生 WXML 标签`)
  if (!hasBalancedWxmlTags(source)) issues.push(`${file} 的 WXML 标签没有正确闭合`)
  const pageConfig = await readJson(file.replace(/\.wxml$/, '.json'))
  const usedComponents = [...source.matchAll(/<(t-[\w-]+)\b/g)].map((match) => match[1])
  for (const component of new Set(usedComponents)) {
    if (!pageConfig.usingComponents?.[component] && !app.usingComponents?.[component]) {
      issues.push(`${file} 使用了 ${component}，但页面 JSON 未声明该组件`)
    }
  }
}
for (const file of await listFiles('miniprogram/custom-tab-bar', '.wxml')) {
  const source = await read(file)
  if (unsupportedHtmlTag.test(source)) issues.push(`${file} 仍包含非原生 WXML 标签`)
  if (!hasBalancedWxmlTags(source)) issues.push(`${file} 的 WXML 标签没有正确闭合`)
}

const packageFiles = await listFiles('miniprogram')
const packageBytes = (await Promise.all(packageFiles.map(async (file) => (await stat(resolve(root, file))).size)))
  .reduce((total, size) => total + size, 0)
if (packageBytes >= 2 * 1024 * 1024) issues.push(`小程序目录原始体积为 ${(packageBytes / 1024 / 1024).toFixed(2)}MB，已达到 2MB 主包风险线`)

if (issues.length) {
  console.error('小程序暂不满足上传条件：')
  issues.forEach((issue) => console.error(`- ${issue}`))
  process.exit(1)
}

console.log('小程序静态发布门禁通过：项目配置、AppID、正式域名、主体备案、隐私授权、合规页面与 WXML 模板均已校验。')
