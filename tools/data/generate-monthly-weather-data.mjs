import { writeFileSync } from 'node:fs'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { cities } from './cities.mjs'

export const SEED = 20260901
const models = [{ id: 1, code: 'ECMWF' }, { id: 2, code: 'NOAA' }]
const elements = [
  ['T2M', '2 米气温', '℃'], ['PRECIP', '降水量', 'mm'], ['TCC', '总云量', '%'],
  ['WIND_SPEED_100M', '100米风速', 'm/s'], ['WIND_DIR_100M', '100米风向', '°'], ['RH', '相对湿度', '%']
].map(([code, name, unit], i) => ({ id: i + 1, code, name, unit }))
const round = n => Math.round(n * 100) / 100
const clamp = (n, min, max) => Math.min(max, Math.max(min, n))
const pulse = (day, center, width) => Math.exp(-(((day - center) / width) ** 2))

export function windFromComponents(u100, v100) {
  return { speed: round(Math.hypot(u100,v100)),
    direction: round((Math.atan2(-u100,-v100)*180/Math.PI+360)%360)%360 }
}

export function generateMonthlyWeather(seed = SEED) {
  let state = seed >>> 0
  const random = () => { state = (Math.imul(1664525, state) + 1013904223) >>> 0; return state / 4294967296 }
  const phase = random() * Math.PI * 2
  const offsets = cities.map(() => (random() - .5) * 1.2)
  const records = [], samples = []
  for (const city of cities) for (let step = 0; step < 240; step++) {
    const day = step / 8, hour = 2 + (step % 8) * 3
    const forecastTime = `2026-09-${String(Math.floor(day) + 1).padStart(2,'0')} ${String(hour).padStart(2,'0')}:00:00`
    const localDay = day - (city.longitude - 118) * .07
    const rainProcess = Math.max(pulse(localDay,6.4,1.1),pulse(localDay,15.2,1.3),pulse(localDay,24.1,1.1))
    const windProcess = Math.max(pulse(localDay,9.5,1.2),pulse(localDay,20,1.4))
    const diurnal = Math.cos((hour - 14) * Math.PI / 12)
    const baseTemperature = 25 - .16*day + (36-city.latitude)*.6 + offsets[city.id-1]
      + 1.1*Math.sin(day*.22+phase) + (4.2-2.7*rainProcess)*diurnal - 2.8*rainProcess
    const direction = 210 + 45*Math.sin(day*.28+phase) + 18*Math.sin(day*.8) + city.id*.6
    for (const model of models) {
      const bias = model.id === 1 ? 0 : -.45 + .12*Math.sin(day*.7)
      let temperature = round(baseTemperature + bias)
      // Each PRECIP point covers the preceding non-overlapping three hours.
      let precipitation = round(rainProcess > .53 ? (rainProcess-.53)*12*(1+.15*diurnal)*(model.id===1?1:1.08) : 0)
      // 兼容 v1.0 固定验收样例：济南 9/7 08、11、14 的十二个值。
      if(city.id===1 && Math.floor(day)===6 && [8,11,14].includes(hour)) {
        const index=[8,11,14].indexOf(hour)
        temperature=(model.id===1?[19,20.2,21.1]:[18.4,19.6,20.5])[index]
        precipitation=(model.id===1?[0,.4,.2]:[.1,.6,.3])[index]
      }
      const cloud = round(clamp(18+76*rainProcess+5*Math.sin(day*.9+phase)+(model.id-1)*2,0,100))
      const speed = 3.5+10*windProcess+1.2*rainProcess+.4*Math.sin(day*.8+phase)+city.id*.025+(model.id-1)*.25
      const radians = (direction+(model.id-1)*2)*Math.PI/180
      const u100 = -speed*Math.sin(radians), v100 = -speed*Math.cos(radians)
      const d2m = temperature - (8.5-7.3*rainProcess+.5*diurnal+(model.id-1)*.2)
      // Water-surface Magnus approximation in Celsius; synthetic, not observed data.
      // RH = 100 exp(17.67 D/(D+243.5) - 17.67 T/(T+243.5)), clamped to 0..100.
      const rh = clamp(100*Math.exp(17.67*d2m/(d2m+243.5)-17.67*temperature/(temperature+243.5)),0,100)
      const wind = windFromComponents(u100,v100)
      const values = [temperature,precipitation,cloud,wind.speed,wind.direction,round(rh)]
      samples.push({cityId:city.id, modelId:model.id, forecastTime, u100,v100,d2m})
      for(const element of elements) records.push({cityId:city.id,modelId:model.id,elementId:element.id,forecastTime,value:values[element.id-1]})
    }
  }
  return { cities,models,elements,records,samples }
}

function sql(data) {
  const quote = s => `'${s.replaceAll("'","''")}'`
  const batch = (table,columns,rows) => `INSERT INTO ${table} (${columns}) VALUES\n  ${rows.join(',\n  ')};\n`
  const chunks = [
    '-- 课程设计合成气象数据；ECMWF / NOAA 仅为共享合成过程的模型场景标识。',
    `-- Generator seed ${SEED}; 16 cities x 2 models x 6 elements x 240 times = ${data.records.length} rows.`,
    '-- PRECIP: preceding non-overlapping 3-hour amount. Includes v1.0 fixed Jinan acceptance overrides.',
    'USE shandong_weather;\nSET NAMES utf8mb4;\nSTART TRANSACTION;',
    batch('city','id, city_code, city_name, longitude, latitude',data.cities.map(c=>`(${c.id}, ${quote(c.code)}, ${quote(c.name)}, ${c.longitude.toFixed(6)}, ${c.latitude.toFixed(6)})`)),
    batch('forecast_model','id, model_code, model_name, description',data.models.map(m=>`(${m.id}, ${quote(m.code)}, ${quote(m.code)}, ${quote(m.id===1?'欧洲中期天气预报中心模型':'美国国家海洋和大气管理局模型')})`)),
    batch('weather_element','id, element_code, element_name, unit',data.elements.map(e=>`(${e.id}, ${quote(e.code)}, ${quote(e.name)}, ${quote(e.unit)})`))
  ]
  for(let i=0;i<data.records.length;i+=1000) chunks.push(batch('forecast_record','city_id, model_id, element_id, forecast_time, value',
    data.records.slice(i,i+1000).map(r=>`(${r.cityId}, ${r.modelId}, ${r.elementId}, ${quote(r.forecastTime)}, ${r.value.toFixed(2)})`)))
  return chunks.join('\n')+'\nCOMMIT;\n'
}

if(process.argv[1] && pathToFileURL(process.argv[1]).href===import.meta.url) {
  const output=sql(generateMonthlyWeather())
  if(process.argv.includes('--stdout')) process.stdout.write(output)
  else { writeFileSync(fileURLToPath(new URL('../../database/data.sql',import.meta.url)),output,'utf8'); console.log('Generated database/data.sql: 46080 records, 240 timestamps') }
}
