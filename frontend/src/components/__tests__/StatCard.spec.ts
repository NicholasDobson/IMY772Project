import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatCard from '@/components/amr/StatCard.vue'

describe('StatCard', () => {
  const baseProps = {
    label: 'MDRO Incident Rate',
    value: '63.3%',
    trendText: '+3.2% from 2024',
  }

  it('renders the label', () => {
    const wrapper = mount(StatCard, { props: baseProps })
    expect(wrapper.find('.stat-label').text()).toBe('MDRO Incident Rate')
  })

  it('renders the value', () => {
    const wrapper = mount(StatCard, { props: baseProps })
    expect(wrapper.find('.stat-value').text()).toBe('63.3%')
  })

  it('renders the trend text', () => {
    const wrapper = mount(StatCard, { props: baseProps })
    expect(wrapper.find('.stat-trend').text()).toContain('+3.2% from 2024')
  })

  it('is not visible by default (missing stat-card--visible class)', () => {
    const wrapper = mount(StatCard, { props: baseProps })
    expect(wrapper.classes()).not.toContain('stat-card--visible')
  })

  it('gains stat-card--visible class when visible prop is true', () => {
    const wrapper = mount(StatCard, { props: { ...baseProps, visible: true } })
    expect(wrapper.classes()).toContain('stat-card--visible')
  })

  it('applies transition delay style when delay prop is provided', () => {
    const wrapper = mount(StatCard, { props: { ...baseProps, delay: 150 } })
    expect(wrapper.attributes('style')).toContain('150ms')
  })

  it('renders the trend icon when trendIcon prop is provided', () => {
    const wrapper = mount(StatCard, {
      props: { ...baseProps, trendIcon: 'pi-sort-up-fill' },
    })
    const icon = wrapper.find('i.pi')
    expect(icon.exists()).toBe(true)
    expect(icon.classes()).toContain('pi-sort-up-fill')
  })

  it('does not render a trend icon when trendIcon is null', () => {
    const wrapper = mount(StatCard, { props: { ...baseProps, trendIcon: null } })
    expect(wrapper.find('i.pi').exists()).toBe(false)
  })

  it('applies trendClass to the trend container', () => {
    const wrapper = mount(StatCard, {
      props: { ...baseProps, trendClass: 'trend-danger' },
    })
    expect(wrapper.find('.stat-trend').classes()).toContain('trend-danger')
  })

  it('defaults to trend-muted trendClass', () => {
    const wrapper = mount(StatCard, { props: baseProps })
    expect(wrapper.find('.stat-trend').classes()).toContain('trend-muted')
  })

  it('applies valueClass to the value element', () => {
    const wrapper = mount(StatCard, {
      props: { ...baseProps, valueClass: 'value-blue' },
    })
    expect(wrapper.find('.stat-value').classes()).toContain('value-blue')
  })
})
