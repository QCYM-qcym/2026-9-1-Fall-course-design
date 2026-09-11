const temperatureColors = ['#426ba3', '#54a5ad', '#bccf9a', '#e6b478', '#d77962']
const waterColors = ['#223e58', '#3267a0', '#6275bd', '#9a79c6', '#dab5dc']
const metadata = (code, name, unit, precision, displayType, colors, bounds = {}) =>
  Object.freeze({ code, name, unit, precision, displayType, legendTitle: `${name}（${unit}）`, colors: Object.freeze(colors), ...bounds })

export const WEATHER_ELEMENTS = Object.freeze({
  T2M: metadata('T2M', '2 米气温', '℃', 2, 'continuous', temperatureColors),
  PRECIP: metadata('PRECIP', '降水量', 'mm', 2, 'nonnegative', waterColors, { min: 0 }),
  TCC: metadata('TCC', '总云量', '%', 1, 'percentage', ['#294555', '#658696', '#b9cdd5'], { min: 0, max: 100 }),
  WIND_SPEED_100M: metadata('WIND_SPEED_100M', '100 米风速', 'm/s', 2, 'nonnegative', ['#2c5866', '#6eabac', '#d6d79d'], { min: 0 }),
  WIND_DIR_100M: metadata('WIND_DIR_100M', '100 米风向', '°', 1, 'direction', ['#71b4c4', '#d6ca95', '#ba8daa', '#818dcc', '#71b4c4'], { min: 0, max: 360 }),
  RH: metadata('RH', '相对湿度', '%', 1, 'percentage', ['#344d62', '#568eaa', '#b1dcd0'], { min: 0, max: 100 })
})

export const ELEMENT_UNITS = Object.freeze(Object.fromEntries(Object.values(WEATHER_ELEMENTS).map(meta => [meta.code, meta.unit])))
export const isDisplayElement = code => Object.hasOwn(ELEMENT_UNITS, code)

export function windDirection(angle) {
  if (!Number.isFinite(angle)) return ''
  const normalized = ((angle % 360) + 360) % 360
  return ['北', '东北', '东', '东南', '南', '西南', '西', '西北'][Math.round(normalized / 45) % 8]
}

export function formatElementNumber(value, code) {
  return Number.isFinite(value) ? value.toFixed(WEATHER_ELEMENTS[code]?.precision ?? 2) : '--'
}

export function formatElementValue(value, code, missing = '--') {
  if (!Number.isFinite(value)) return missing
  const meta = WEATHER_ELEMENTS[code]
  return `${formatElementNumber(value, code)}${meta ? ` ${meta.unit}` : ''}${meta?.displayType === 'direction' ? ` · ${windDirection(value)}风` : ''}`
}
