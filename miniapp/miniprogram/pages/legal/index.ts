import { PRIVACY_VERSION, TERMS_VERSION, privacySections, termsSections } from '../../config/legal'
import type { LegalSection } from '../../config/legal'

Page({
  data: {
    title: '用户协议',
    eyebrow: 'SERVICE TERMS',
    version: TERMS_VERSION,
    sections: [] as LegalSection[],
    privacy: false,
  },
  onLoad(query: Record<string, string | undefined>) {
    const privacy = query.type === 'privacy'
    this.setData({
      privacy,
      title: privacy ? '隐私处理摘要' : '用户协议',
      eyebrow: privacy ? 'PRIVACY SUMMARY' : 'SERVICE TERMS',
      version: privacy ? PRIVACY_VERSION : TERMS_VERSION,
      sections: privacy ? privacySections : termsSections,
    })
  },
  goBack() { wx.navigateBack() },
  openPlatformPrivacy() {
    if (typeof wx.openPrivacyContract !== 'function') {
      wx.showToast({ title: '请在正式小程序中查看', icon: 'none' })
      return
    }
    wx.openPrivacyContract({ fail: () => wx.showToast({ title: '平台隐私指引尚未配置', icon: 'none' }) })
  },
})
