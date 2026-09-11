import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import ForecastTimeline from './ForecastTimeline.vue'
import ForecastRangePresets from './ForecastRangePresets.vue'
import WeatherLegend from './WeatherLegend.vue'
import CityDetailPanel from './CityDetailPanel.vue'

const times = ['2026-09-07', '2026-09-08'].flatMap(day => ['02', '05', '08', '11', '14', '17', '20', '23'].map(hour => `${day} ${hour}:00:00`))
describe('daily forecast timeline', () => {
  it('shows a date selector and only the selected day’s eight actual slots', async () => {
    const wrapper = mount(ForecastTimeline, { props: { times, selectedTime: times[4], disabled: false } })
    expect(wrapper.findAll('option')).toHaveLength(2)
    expect(wrapper.findAll('.timeline-track button')).toHaveLength(8)
    expect(wrapper.find('[aria-pressed="true"]').attributes('aria-label')).toContain(times[4])
    await wrapper.get('select').setValue('2026-09-08')
    expect(wrapper.emitted('select').at(-1)).toEqual([times[12]])
    await wrapper.setProps({ selectedTime: times[12] })
    expect(wrapper.findAll('.timeline-track button').every(button => button.attributes('aria-label').includes('2026-09-08'))).toBe(true)
    await wrapper.findAll('.timeline-track button')[0].trigger('click')
    expect(wrapper.emitted('select').at(-1)).toEqual([times[8]])
  })
  it('uses only existing slots in a partial day and disables controls while loading', async () => {
    const wrapper = mount(ForecastTimeline, { props: { times: [times[0], times[9]], selectedTime: times[0], disabled: false } })
    await wrapper.get('select').setValue('2026-09-08')
    expect(wrapper.emitted('select').at(-1)).toEqual([times[9]])
    await wrapper.setProps({ disabled: true })
    expect(wrapper.get('select').attributes()).toHaveProperty('disabled')
    expect(wrapper.findAll('button').every(button => button.attributes('disabled') !== undefined)).toBe(true)
  })
})

describe('weather display controls', () => {
  it('range shortcuts emit the exact frozen ranges without submitting a filter form', async () => {
    const range = ['2026-09-01 02:00:00', '2026-09-30 23:00:00']
    const wrapper = mount(ForecastRangePresets, { props: { range } })
    expect(wrapper.find('[aria-pressed="true"]').text()).toBe('30 天月度数据')
    const buttons = wrapper.findAll('button')
    expect(buttons.every(button => button.attributes('type') === 'button')).toBe(true)
    await buttons[1].trigger('click')
    expect(wrapper.emitted('select')[0]).toEqual([['2026-09-07 08:00:00', '2026-09-07 14:00:00']])
    await buttons[0].trigger('click')
    expect(wrapper.emitted('select')[1]).toEqual([range])
    expect(wrapper.emitted('select')[1][0]).not.toBe(range)
  })

  it('wind legend explains the full circular scale and city detail includes a bearing', () => {
    const element = { elementCode: 'WIND_DIR_100M', elementName: '100 米风向', unit: '°' }
    const legend = mount(WeatherLegend, { props: { element, range: { min: 0, max: 360, empty: false } } })
    for (const bearing of ['0° 北', '90° 东', '180° 南', '270° 西', '360° 北']) expect(legend.text()).toContain(bearing)
    expect(legend.text()).toContain('颜色表示方位')
    const city = { id: 1, cityName: '济南', value: 237.5 }
    const detail = mount(CityDetailPanel, { props: { element, city, cities: [city], ready: true } })
    expect(detail.text()).toContain('237.5')
    expect(detail.text()).toContain('西南风')
  })
})
