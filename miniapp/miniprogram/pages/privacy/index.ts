import { PRIVACY_VERSION, TERMS_VERSION } from '../../config/legal'
import { complianceDisplay, complianceInfo } from '../../config/compliance'

Page({
  data: {
    privacyVersion: PRIVACY_VERSION,
    termsVersion: TERMS_VERSION,
    operatorName: complianceDisplay(complianceInfo.operatorName),
    filingNumber: complianceDisplay(complianceInfo.appFilingNumber),
  },
  goBack() { wx.navigateBack() },
  openPlatformPrivacy() {
    if (typeof wx.openPrivacyContract !== 'function') {
      wx.navigateTo({ url: '/pages/legal/index?type=privacy' })
      return
    }
    wx.openPrivacyContract({
      fail: () => wx.navigateTo({ url: '/pages/legal/index?type=privacy' }),
    })
  },
  openPrivacySummary() { wx.navigateTo({ url: '/pages/legal/index?type=privacy' }) },
  openTerms() { wx.navigateTo({ url: '/pages/legal/index?type=terms' }) },
  editProfile() { wx.navigateTo({ url: '/pages/profile-edit/index' }) },
  openAccount() { wx.navigateTo({ url: '/pages/account/index' }) },
  openReport() { wx.navigateTo({ url: '/pages/report/index' }) },
  copyFilingInfo() { wx.setClipboardData({ data: complianceInfo.filingQueryUrl }) },
})
