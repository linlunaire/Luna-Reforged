<h1 align="center">
  <img src="docs/images/luna-reforged.svg" alt="Luna Reforged" width="192"><br>
  Luna Reforged
</h1>

<h4 align="center">辉月重铸 · 以月华淬炼锋芒</h4>

<p align="center">
  <a href="docs/building.md"><img src="https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin 100%"></a>
  <a href="docs/compatibility.md"><img src="https://img.shields.io/badge/Minecraft-26.2%20%7C%201.21.1-5E9D34" alt="Minecraft 26.2 and 1.21.1"></a>
  <a href="docs/usage.md"><img src="https://img.shields.io/badge/Loaders-Fabric%20%7C%20NeoForge-8B76C5" alt="Fabric and NeoForge"></a>
  <a href="https://github.com/linlunaire/Luna-Reforged/actions/workflows/build.yml"><img src="https://github.com/linlunaire/Luna-Reforged/actions/workflows/build.yml/badge.svg" alt="Build"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-blue" alt="GPL-3.0"></a>
</p>

<p align="center">
  <a href="docs/usage.md">安装与使用</a> ·
  <a href="docs/reforges.md">全部词缀</a> ·
  <a href="docs/custom-reforges.md">自定义词缀</a> ·
  <a href="docs/building.md">构建</a> ·
  <a href="docs/changelog.md">更新记录</a>
</p>

为武器、护甲和镐添加重铸词缀。在辉月重铸台消耗经验，获得攻击、防御、挖掘与特殊命中效果。

<p align="center">
  <a href="docs/usage.md"><img src="docs/images/reforging-siphoning.png" alt="辉月重铸界面，展示汲月词缀的效果和费用" width="960"></a>
</p>

- 44 个词缀，包含点燃、缓慢、生命汲取和条件效果，名称使用渐变配色。
- 右槽留空时随机重铸，放入重铸石可指定词缀。
- 重铸前可查看效果与费用，成功后播放音效、粒子和界面动画。
- 保留装备原有名称、附魔和耐久，铁砧可正常改名。
- 服务端可调整经验费用、添加自定义词缀，配置会自动同步至客户端。

客户端和服务端均需另装 [Kotlin LunaCore 0.4.1](https://github.com/linlunaire/Kotlin-LunaCore/releases/tag/v0.4.1)，选择相同游戏版本与加载器；Fabric 还需对应版本的 Fabric API。安装要求见 [使用说明](docs/usage.md)，实测范围见 [验证记录](docs/verification.md)。

### 许可证

*采用 [GPL-3.0](LICENSE)。玩法基于 [Reforges](https://github.com/Auxilor/Reforges)，来源说明见 [NOTICE](NOTICE.md)。*
