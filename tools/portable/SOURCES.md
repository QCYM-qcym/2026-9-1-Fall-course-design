# 固定版本与许可证来源

- Temurin JRE Windows x64 HotSpot 17.0.11+9：Eclipse Adoptium 官方发布。原始运行时 legal/、NOTICE 等保留在 runtime/java；对应官方源码归档在本目录 sources/。GPLv2 + Classpath Exception 等按所附原文。
- MySQL Community Server 8.0.46 Windows x64 noinstall：Oracle/MySQL 官方 CDN。原始 LICENSE、README、第三方许可文件保留在 runtime/mysql；对应含 Boost 的官方源码归档在 sources/。按原始 GPLv2/第三方条款分发，不安装服务。
- 官方下载 URL、精确文件名与校验值见 downloads.lock.json。Java 使用官方 SHA-256；MySQL 使用官方 HTTPS CDN 的 MD5 校验文件（并非独立数字签名验证）；发布 ZIP 另生成 SHA-256。
- [MySQL noinstall 官方说明](https://dev.mysql.com/doc/refman/8.0/en/windows-install-archive.html)、[官方 MD5 核验说明](https://dev.mysql.com/doc/refman/8.0/en/verifying-md5-checksum.html)、[Adoptium archive 安装说明](https://adoptium.net/installation/archives/)。不使用第三方下载站或 latest。
- 前端依赖原始许可汇集在 frontend/；后端 JAR 保留各依赖内 META-INF 许可，并在 backend/ 提取供阅读。GeoJSON 的来源/许可证见 GeoJSON-README.md；China-GeoData / MIT，仅课程示意，不用于测绘。
- 本包未复制开发机数据库、开发密码、全局配置或个人登录文件。第三方许可与源码材料不表示提供生产支持或修改任何上游许可证。
