import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StatusPill from './StatusPill.vue'

describe('StatusPill', () => {
  it('将冠军状态显示为中文并应用荣誉色', () => {
    const wrapper = mount(StatusPill, { props: { value: 'CHAMPION' } })
    expect(wrapper.text()).toContain('冠军')
    expect(wrapper.classes()).toContain('pill--champion')
  })
})
