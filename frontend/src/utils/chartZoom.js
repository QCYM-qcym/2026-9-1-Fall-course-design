// Both controls operate on the same category axis, preserving null gaps.
export function buildTimeZoom() {
  return [
    { type: 'slider', xAxisIndex: 0, bottom: 10, height: 22, start: 0, end: 100,
      borderColor: '#466170', backgroundColor: '#172f3e', fillerColor: '#70b1cf24',
      handleStyle: { color: '#8eb6c7', borderColor: '#bbd3dc' },
      dataBackground: { lineStyle: { color: '#6e94a6' }, areaStyle: { color: '#3d6072' } },
      textStyle: { color: '#afc6d1' }, showDetail: false },
    { type: 'inside', xAxisIndex: 0, start: 0, end: 100, zoomOnMouseWheel: 'ctrl', moveOnMouseWheel: false }
  ]
}
