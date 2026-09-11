import { afterEach, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import source from './WeatherSwitchers.vue?raw'
import { parse, compileStyle } from '@vue/compiler-sfc'
import WeatherSwitchers from './WeatherSwitchers.vue'

const elements = ['T2M','PRECIP','TCC','WIND_SPEED_100M','WIND_DIR_100M','RH']
  .map((elementCode,i)=>({id:i+1,elementCode,elementName:elementCode}))
const props = { models: [{id:1,modelName:'ECMWF'},{id:2,modelName:'NOAA'}], elements, modelId:1, elementId:1, disabled:false }
let wrapper, style
afterEach(()=>{wrapper?.unmount(); style?.remove()})

it('bounds the panel above the timeline and permits internal vertical scrolling',()=>{
  // Apply the real SFC CSS. jsdom checks CSS behavior, not pixel layout or clipping.
  const {descriptor}=parse(source)
  style=document.createElement('style')
  style.textContent=compileStyle({source:descriptor.styles[0].content,id:'switchers',scoped:false}).code
  document.head.append(style)
  wrapper=mount(WeatherSwitchers,{props,attachTo:document.body})
  const panel=getComputedStyle(wrapper.element)
  expect(panel.overflowY).toBe('auto')
  expect(panel.maxHeight).toBe('calc(100% - 310px)')
  const button=getComputedStyle(wrapper.get('[aria-label="气象要素"] button').element)
  expect(button.overflowWrap).toBe('anywhere')
})

it('all six layers remain native focusable buttons and emit their own IDs',async()=>{
  wrapper=mount(WeatherSwitchers,{props,attachTo:document.body})
  const buttons=wrapper.findAll('[aria-label="气象要素"] button')
  expect(buttons).toHaveLength(6)
  for(const button of buttons){button.element.focus();expect(document.activeElement).toBe(button.element);await button.trigger('click')}
  expect(wrapper.emitted('element')).toEqual([[1],[2],[3],[4],[5],[6]])
  await wrapper.setProps({elementId:6})
  expect(buttons[5].attributes('aria-pressed')).toBe('true')
  await wrapper.setProps({disabled:true})
  expect(buttons.every(b=>b.element.disabled)).toBe(true)
})
