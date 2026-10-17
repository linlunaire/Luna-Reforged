# 安装与使用

## 安装

客户端和服务端均需安装辉月重铸与 [Kotlin LunaCore](https://github.com/linlunaire/Kotlin-LunaCore)。Fabric 还需安装 Fabric API。

| Minecraft | Java | 加载器 | Fabric API |
| --- | --- | --- | --- |
| 26.2 | 25 | Fabric Loader 0.19.3+ | 0.159.0+26.2 |
| 26.2 | 25 | NeoForge 26.2.0.75+ | — |
| 1.21.1 | 21 | Fabric Loader 0.19.3+ | 0.116.15+1.21.1 |
| 1.21.1 | 21 | NeoForge 21.1.248+ | — |

选择对应的 `Luna-Reforged-<加载器>-<游戏版本>-0.5.1.jar`，并从 [LunaCore 0.4.1 发行页](https://github.com/linlunaire/Kotlin-LunaCore/releases/tag/v0.4.1) 下载 `Kotlin-LunaCore-<加载器>-<游戏版本>-0.4.1.jar`。两个项目的游戏版本与加载器必须一致；辉月重铸发行包只包含辉月本身。

将模组和前置 JAR 放入两端的 `mods` 文件夹。不要安装 `-sources.jar`，也不要重复安装前置。

## 26.2 原生 Vulkan

在游戏视频设置中选择 `Prefer Vulkan (Experimental)`，然后重启。26.2 的 Fabric 与 NeoForge 已在本机完成 Vulkan 联机、重铸界面、渐变名称、粒子和资源重载检查，具体环境见 [验证记录](verification.md)。

NeoForge 26.2.0.75 的早期加载窗口存在已知兼容问题。如果启动时报 `GLFW_NO_API` / `VK_ERROR_NATIVE_WINDOW_IN_USE_KHR`，关闭游戏，将该实例的 `config/fml.toml` 中 `earlyWindowControl` 改为 `false`，并确认 Vulkan 选项仍然启用后重启。这个设置只关闭 NeoForge 的早期加载窗口。测试实例已按此配置通过；模组不会自动改写加载器设置。[NeoForge 问题记录与处理方法](https://github.com/neoforged/NeoForge/issues/3230)

## 开始重铸

1. 合成并放置辉月重铸台，右键打开界面。
2. 左槽放入装备，右槽可留空，也可放入定向重铸石。
3. 查看费用与效果预览，点击重铸。

重铸台配方：第一行「紫水晶碎片、铁锭、紫水晶碎片」，第二行「黑曜石、工作台、黑曜石」，第三行三个黑曜石。重铸台需要钻石级镐才能回收。

普通重铸首次消耗 3 级经验，随重铸次数递增，最高 30 级；不会抽到当前词缀。定向重铸石保证获得指定词缀，并消耗一枚石头：灵动石默认消耗 40 级，丰饶石和勘探石默认消耗 3 级。

## 词缀与效果

内置 44 个词缀，包含点燃、缓慢、生命汲取，以及天气、距离、生命值等条件效果。词缀使用渐变名称和分行说明，绿色表示增益，红色表示代价。

重铸保留装备原有名称、附魔和耐久；铁砧可正常改名。重铸台配有月环界面、效果预览、音效与粒子反馈。

[查看全部词缀](reforges.md) · [自定义词缀](custom-reforges.md) · [更新记录](changelog.md)

## 配置

首次启动会生成 `config/luna-reforged.json`，修改后重启服务端：

```json
{
  "baseLevels": 3,
  "growth": 1.15,
  "maxLevels": 30,
  "stoneLevels": 40,
  "includeNativeReforges": true
}
```

`baseLevels` 是普通重铸的起价，也是丰饶石、勘探石的固定费用；`growth` 是普通重铸每次的费用倍率；`maxLevels` 是普通重铸上限；`stoneLevels` 只控制灵动石的固定费用。扣除的是经验等级，不是经验点。将 `includeNativeReforges` 设为 `false`，可只保留原版 16 个词缀和自定义词缀；已有装备不受影响。

普通重铸费用为 `ceil(min(maxLevels, baseLevels × growth ^ 已重铸次数))`，按当前装备记录的次数计算，石头重铸也会增加该次数。默认前六次普通重铸为 3、4、4、5、6、7 级。普通起价、上限和石头费用需为 1–100 的整数，上限不低于起价；`growth` 范围为 1.0–2.0。

如果服务器经验获取较容易，可以先试以下配置；这是可选示例，不会覆盖现有配置：

```json
{
  "baseLevels": 8,
  "growth": 1.2,
  "maxLevels": 40,
  "stoneLevels": 60,
  "includeNativeReforges": true
}
```

这组配置下，前六次普通重铸为 8、10、12、14、17、20 级，上限 40；灵动石固定 60 级，丰饶石和勘探石固定 8 级。费用与词缀文件都在启动服务端时加载，当前没有 `/reload` 或 `/reforges reload` 热更新；单人游戏可退出世界再进入。

自定义词缀放在服务端的 `config/luna-reforged/reforges/` 目录中。将 `_example.json` 另存为新的 JSON 文件，修改后重启生效，客户端无需另装配置。

从旧版本升级时，同时替换辉月重铸和 LunaCore，移除旧核心 JAR。LunaCore 0.4.0 的内部 ID 为 `lunacore`；旧的 `transit_core` 不能满足新版本依赖。辉月重铸的物品和存档命名空间仍为 `luna_reforged`。
