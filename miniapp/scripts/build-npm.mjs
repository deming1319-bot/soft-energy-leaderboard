import { cpSync, existsSync, mkdirSync, rmSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const source = join(projectRoot, 'node_modules', 'tdesign-miniprogram', 'miniprogram_dist')
const npmRoot = join(projectRoot, 'miniprogram', 'miniprogram_npm')
const target = join(npmRoot, 'tdesign-miniprogram')

if (!existsSync(source)) {
  throw new Error('TDesign 依赖尚未安装，请先执行 pnpm install')
}
if (!target.startsWith(`${npmRoot}${process.platform === 'win32' ? '\\' : '/'}`)) {
  throw new Error('拒绝写入项目外目录')
}

rmSync(target, { recursive: true, force: true })
mkdirSync(target, { recursive: true })
cpSync(source, target, { recursive: true })
console.log(`TDesign 已构建到 ${target}`)
