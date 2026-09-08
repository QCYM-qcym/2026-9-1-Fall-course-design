# 山东 16 市边界

- 来源：[Supeset/China-GeoData](https://github.com/Supeset/China-GeoData)，原始文件 `geojson/china_province_city_full.geojson`。
- 获取日期：2026-09-08。
- 处理：仅筛选 `adcode` 以 `37` 开头的 16 个 feature，重新序列化为 FeatureCollection；未手绘、修改或简化坐标，保留原属性和多边形。
- 文件：`shandong.json`；用于本课程 ECharts Map 的城市区域展示与点击，不代表实时天气或官方精确测绘成果，不用于导航、测绘或行政界线认定。
- 来源仓库以 MIT 许可发布，版权与许可原文保留如下。本项目仅用于课程演示；来源对上游只作概括说明，本项目不额外保证其测绘精度或上游权利，不将其宣传为官方地图服务。
- 编码映射以 `database/data.sql` 的 16 个 cityCode 为依据，在 `utils/weatherWorkbench.js` 明确适配；自动测试校验全部名称、几何类型和匹配数，启动后再次核对真实城市字典。

## Source license

MIT License

Copyright (c) 2025 圈集

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
