import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { execFileSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'
import { generateMonthlyWeather } from './generate-monthly-weather-data.mjs'
import * as generator from './generate-monthly-weather-data.mjs'

const data = generateMonthlyWeather()
const codes = ['T2M', 'PRECIP', 'TCC', 'WIND_SPEED_100M', 'WIND_DIR_100M', 'RH']
const near = (a, b) => assert.ok(Math.abs(a - b) <= 0.00501, `${a} != ${b}`)
test('fixed seed produces repeatable records and internal samples', () => {
  assert.equal(data.records.length, 46080)
  assert.deepEqual(generateMonthlyWeather(), data)
  assert.notDeepEqual(generateMonthlyWeather(20260902).records, data.records)
})
test('full Cartesian month has 16 cities, 2 models, 6 elements and unique business keys', () => {
  assert.equal(data.cities.length, 16); assert.equal(data.models.length, 2)
  assert.deepEqual(data.elements.map(e => e.code), codes)
  assert.equal(data.records.length, 46080)
  const keys = new Set(data.records.map(r => `${r.cityId}/${r.modelId}/${r.elementId}/${r.forecastTime}`))
  assert.equal(keys.size, 46080)
  const groups = new Map()
  for (const r of data.records) {
    const key = `${r.cityId}/${r.modelId}/${r.elementId}`
    groups.set(key, (groups.get(key) ?? 0) + 1)
  }
  assert.equal(groups.size, 192); assert.ok([...groups.values()].every(n => n === 240))
})
test('calendar is exactly thirty days with eight three-hour local timestamps each', () => {
  const times = [...new Set(data.records.map(r => r.forecastTime))].sort()
  assert.equal(times.length, 240)
  assert.equal(times[0], '2026-09-01 02:00:00'); assert.equal(times.at(-1), '2026-09-30 23:00:00')
  for (let day = 1; day <= 30; day++) assert.deepEqual(times.filter(t => +t.slice(8, 10) === day).map(t => t.slice(11)),
    ['02:00:00','05:00:00','08:00:00','11:00:00','14:00:00','17:00:00','20:00:00','23:00:00'])
})
test('every element stays in its synthetic range', () => {
  assert.equal(data.records.length, 46080)
  const bounds = [[10,35],[0,30],[0,100],[0,30],[0,359.99],[0,100]]
  for (const r of data.records) { const [min,max] = bounds[r.elementId - 1]; assert.ok(Number.isFinite(r.value) && r.value >= min && r.value <= max) }
})
test('wind and humidity derive from exposed internal variables including overridden temperature', () => {
  assert.equal(data.samples.length, 7680)
  const records = new Map(data.records.map(r => [`${r.cityId}/${r.modelId}/${r.forecastTime}/${r.elementId}`,r.value]))
  for (const s of data.samples) {
    const value = id => records.get(`${s.cityId}/${s.modelId}/${s.forecastTime}/${id}`)
    near(value(4), Math.hypot(s.u100, s.v100))
    const direction = (Math.atan2(-s.u100, -s.v100) * 180 / Math.PI + 360) % 360
    assert.ok(Math.min(Math.abs(value(5)-direction),360-Math.abs(value(5)-direction)) <= .00501)
    near(value(6), Math.min(100, Math.max(0, 100 * Math.exp(17.67*s.d2m/(s.d2m+243.5)-17.67*value(1)/(value(1)+243.5)))))
  }
})
test('shared processes yield dry majority, sustained humid cloudy rain and stronger smooth wind', () => {
  assert.equal(data.samples.length,7680)
  const rows = data.samples.filter(s => s.cityId === 2 && s.modelId === 1)
  const values = new Map(data.records.filter(r => r.cityId === 2 && r.modelId === 1).map(r => [`${r.forecastTime}/${r.elementId}`,r.value]))
  const v = (s,id) => values.get(`${s.forecastTime}/${id}`)
  const wet = rows.filter(s => v(s,2)>0), dry = rows.filter(s => v(s,2)===0)
  assert.ok(dry.length > rows.length*.6 && wet.length > 10)
  assert.ok(rows.some((s,i) => i>1 && [s,rows[i-1],rows[i-2]].every(p=>v(p,2)>0)))
  const avg = (a,id) => a.reduce((n,s)=>n+v(s,id),0)/a.length
  assert.ok(avg(wet,3)>avg(dry,3)+20); assert.ok(avg(wet,6)>avg(dry,6)+10)
  assert.ok(Math.max(...rows.map(s=>v(s,4)))>10)
  for(let i=1;i<rows.length;i++) { const delta=Math.abs(v(rows[i],5)-v(rows[i-1],5)); assert.ok(Math.min(delta,360-delta)<30) }
  const pairs = new Map(data.records.filter(r=>r.modelId===1).map(r=>[`${r.cityId}/${r.elementId}/${r.forecastTime}`,r.value]))
  let different=0
  for(const r of data.records.filter(r=>r.modelId===2 && r.elementId===1)) { const other=pairs.get(`${r.cityId}/1/${r.forecastTime}`); assert.ok(Math.abs(other-r.value)<1.5); if(other!==r.value)different++ }
  assert.ok(different>3500)
})
test('all twelve v1 Jinan acceptance values remain intact', () => {
  for(const [model,element,want] of [[1,1,[19,20.2,21.1]],[2,1,[18.4,19.6,20.5]],[1,2,[0,.4,.2]],[2,2,[.1,.6,.3]]]) {
    const actual=data.records.filter(r=>r.cityId===1 && r.modelId===model && r.elementId===element && ['2026-09-07 08:00:00','2026-09-07 11:00:00','2026-09-07 14:00:00'].includes(r.forecastTime)).map(r=>r.value)
    assert.deepEqual(actual,want)
  }
})

test('meteorological cardinal directions use wind FROM with atan2 argument order',()=>{
  assert.equal(typeof generator.windFromComponents,'function')
  for(const [u,v,speed,direction] of [[0,-5,5,0],[-5,0,5,90],[0,5,5,180],[5,0,5,270],[-3,-4,5,36.87]]) {
    const actual=generator.windFromComponents(u,v); near(actual.speed,speed); near(actual.direction,direction)
  }
})
test('every city and both models have correlated rain/cloud/humidity and smaller rainy-day temperature range',()=>{
  for(let cityId=1;cityId<=16;cityId++) for(let modelId=1;modelId<=2;modelId++) {
    const byTime=new Map()
    for(const r of data.records.filter(r=>r.cityId===cityId && r.modelId===modelId)) {
      if(!byTime.has(r.forecastTime)) byTime.set(r.forecastTime,{})
      byTime.get(r.forecastTime)[r.elementId]=r.value
    }
    assert.equal(byTime.size,240)
    const rows=[...byTime.values()], wet=rows.filter(r=>r[2]>0),dry=rows.filter(r=>r[2]===0)
    const mean=(a,id)=>a.reduce((n,r)=>n+r[id],0)/a.length
    assert.ok(dry.length>144 && wet.length>10)
    assert.ok(mean(wet,3)>mean(dry,3)+20); assert.ok(mean(wet,6)>mean(dry,6)+10)
    const amplitude=day=>{ const values=[...byTime].filter(([t])=>t.startsWith(`2026-09-${day}`)).map(([,r])=>r[1]); return Math.max(...values)-Math.min(...values) }
    assert.ok(amplitude('07')<amplitude('02'),`${cityId}/${modelId}: rainy day range must shrink`)
  }
})
test('CLI emits reproducible UTF8 batch SQL with exactly 46080 distinct rows and no internal elements', () => {
  const run = () => execFileSync(process.execPath,[fileURLToPath(new URL('./generate-monthly-weather-data.mjs',import.meta.url)),'--stdout'], { maxBuffer: 8 * 1024 * 1024 })
  const first=run(), second=run()
  assert.equal(createHash('sha256').update(first).digest('hex'),createHash('sha256').update(second).digest('hex'))
  const sql=first.toString('utf8')
  const rows=[...sql.matchAll(/\((\d+), (\d+), (\d+), '(2026-09-\d{2} \d{2}:00:00)', (-?\d+\.\d{2})\)/g)]
  assert.equal(rows.length,46080); assert.equal(new Set(rows.map(m=>m.slice(1,5).join('/'))).size,46080)
  assert.ok(sql.includes('济南')); assert.doesNotMatch(sql,/\b(?:u100|v100|d2m)\b/i)
  assert.ok((sql.match(/INSERT INTO forecast_record/g)??[]).length<100)
  assert.deepEqual(first,readFileSync(new URL('../../database/data.sql',import.meta.url)))
})
