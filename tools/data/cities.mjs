// Fixed city dictionary, preserved independently of generated SQL.
export const cities = [
  [1, 'JINAN', '济南', 116.997222, 36.668333],
  [2, 'QINGDAO', '青岛', 120.380420, 36.064880],
  [3, 'ZIBO', '淄博', 118.063333, 36.790556],
  [4, 'ZAOZHUANG', '枣庄', 117.554167, 34.864722],
  [5, 'DONGYING', '东营', 118.491653, 37.462708],
  [6, 'YANTAI', '烟台', 121.440811, 37.476493],
  [7, 'WEIFANG', '潍坊', 119.101944, 36.710000],
  [8, 'JINING', '济宁', 116.581389, 35.405000],
  [9, 'TAIAN', '泰安', 117.120000, 36.185278],
  [10, 'WEIHAI', '威海', 122.113557, 37.509136],
  [11, 'RIZHAO', '日照', 119.529080, 35.414140],
  [12, 'LINYI', '临沂', 118.342778, 35.063056],
  [13, 'DEZHOU', '德州', 116.367059, 37.446613],
  [14, 'LIAOCHENG', '聊城', 116.002473, 36.450638],
  [15, 'BINZHOU', '滨州', 117.836389, 37.605000],
  [16, 'HEZE', '菏泽', 115.473578, 35.239289]
].map(([id, code, name, longitude, latitude]) => Object.freeze({ id, code, name, longitude, latitude }))
Object.freeze(cities)
