import { TEMPERATURE_COLORS, PRECIPITATION_COLORS } from './weatherWorkbench.js'

export function formatWeatherValue(value) {
  return Number.isFinite(value) ? value.toFixed(2) : '暂无数据'
}

export function createMapOption(rows, range, elementCode, unit, selectedCityId) {
  const byName = new Map(rows.map(row => [row.name, row]))
  return {
    animationDurationUpdate: 220,
    tooltip: {
      trigger: 'item', renderMode: 'richText', backgroundColor: '#172b3c', borderColor: '#466073', textStyle: { color: '#edf3f6' },
      formatter: params => `${params.name}\n${formatWeatherValue(byName.get(params.name)?.value)}${Number.isFinite(byName.get(params.name)?.value) ? ` ${unit}` : ''}`
    },
    visualMap: {
      show: false, min: range.min, max: range.max, seriesIndex: 0,
      inRange: { color: elementCode === 'PRECIP' ? PRECIPITATION_COLORS : TEMPERATURE_COLORS },
      outOfRange: { color: '#314655' }
    },
    series: [{
      id: 'shandong-weather', type: 'map', map: 'shandong-course', roam: false,
      left: '2%', right: '2%', top: '4%', bottom: '4%', selectedMode: 'single',
      itemStyle: { areaColor: '#314655', borderColor: '#a9c3cf', borderWidth: 1, shadowColor: '#00000045', shadowBlur: 14 },
      label: {
        show: true, color: '#fff', fontSize: 11, lineHeight: 16, textBorderColor: '#173344', textBorderWidth: 2,
        formatter: params => {
          const row = byName.get(params.name)
          return `${row?.cityName ?? params.name}\n${formatWeatherValue(row?.value)}`
        }
      },
      emphasis: { label: { color: '#fff' }, itemStyle: { borderColor: '#ffffff', borderWidth: 2, areaColor: undefined } },
      select: { label: { color: '#fff' }, itemStyle: { borderColor: '#fff5c3', borderWidth: 3 } },
      data: rows.map(row => ({
        name: row.name, value: row.value, selected: row.id === selectedCityId,
        ...(row.value === null ? { itemStyle: { areaColor: '#314655' } } : {})
      }))
    }]
  }
}
