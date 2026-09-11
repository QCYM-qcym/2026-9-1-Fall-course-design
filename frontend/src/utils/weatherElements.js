export const ELEMENT_UNITS = Object.freeze({
  T2M: '℃', PRECIP: 'mm', TCC: '%', WIND_SPEED_100M: 'm/s', WIND_DIR_100M: '°', RH: '%'
})
export const isDisplayElement = code => Object.hasOwn(ELEMENT_UNITS, code)
