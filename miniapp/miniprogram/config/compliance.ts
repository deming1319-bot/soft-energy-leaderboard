export const complianceInfo = {
  operatorName: '乔德明',
  privacyContact: '乔德明',
  privacyEmailOrPhone: '469802389@qq.com',
  contactAddress: '河南省南阳市方城县清河乡小谷庄村',
  appFilingNumber: '豫ICP备2026043639号-1X',
  filingQueryUrl: 'https://beian.miit.gov.cn/',
  storageLocation: '中华人民共和国境内的阿里云服务器',
  accountRetention: '账号存续期间保存；注销成功时立即清除身份和原始答题内容',
  backupRetention: '生产备份最长30日自动轮换，期间仅用于灾难恢复且不再用于业务处理',
  reportResponseTime: '收到后尽快确认，普通反馈在7个工作日内给出处理结果',
} as const

export function complianceDisplay(value: string): string {
  return value.startsWith('__REPLACE_') ? '正式上线前配置' : value
}
