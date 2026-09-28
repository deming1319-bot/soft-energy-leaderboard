Component({
  data: { selected: 0, tabs: [{ pagePath: '/pages/home/index', text: '修习', glyph: '问' }, { pagePath: '/pages/profile/index', text: '我的', glyph: '我' }] },
  methods: {
    switchTab(event: WechatMiniprogram.TouchEvent) {
      const index = Number(event.currentTarget.dataset.index)
      const tab = this.data.tabs[index]
      if (!tab) return
      this.setData({ selected: index })
      wx.switchTab({ url: tab.pagePath })
    },
  },
})
