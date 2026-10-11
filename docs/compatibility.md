# 0.5.0 与原版 Reforges 的对齐范围

本版以 [Reforges 提交 8ee14ce](https://github.com/Auxilor/Reforges/tree/8ee14ce9c6d979d5fa5a7e71a6e2e4df964d2d4e) 为对照；2026-09-30 核查时它仍是上游 HEAD。依赖行为核对至 `libreforge 2026.39` 标签 `957055ca5aa5c809d8af98fdbb6c3f58beb13f4d`。本模组独立实现原生逻辑，不加载 Bukkit、eco 或 libreforge。

| 项目 | 0.5.0 行为及依据 |
|---|---|
| 名称 | 非斜体渐变词缀前缀、彩色分行说明，提供中英文。0.5.0 为深色界面调整潮汐、引力、坚毅、强韧、穿刺及扩展词缀背水的配色；其他词缀保留既有端点。物品原有自定义名称不被改写 |
| 铁砧 | 输入框和费用计算均使用原始名称；26.2 NeoForge 的 `createResultInternal` 单独适配，防止前缀变成改名内容 |
| 未重铸物品 | 提示该装备可以重铸，与上游 `show-reforgable` 的默认显示意图一致 |
| 普通重铸 | 右槽可留空，仅消耗经验；匹配目标、非石头专属、排除当前词缀后等概率抽取 |
| 默认词缀 | 16 个原版词缀全部保留；勘探恢复为普通随机词缀。灵动、丰饶仍是石头专属 |
| 扩展池 | 28 个原生扩展默认保留，可通过 `includeNativeReforges: false` 从新抽取池移除；已有装备和自定义 JSON 仍有效 |
| 石头 | 指定词缀、扣一枚、可以重复施加同一词缀。固定价格不随次数增长；灵动有专属价格，丰饶沿用基础价格 |
| 普通费用 | 上游代码为 `base × 1.15^times`，与其配置文件内的公式注释不同；本模组以实现为准，并保留经验等级取整及上限 |
| 配方 | 灵动/丰饶采用上游未安装 EcoItems 时的配方；原生勘探石继续保留，属于额外内容 |
| 击退 | Strong / Light 对攻击击退属性乘以 1.10 / 0.95，修正旧版直接加 0.1 / 减 0.05。原生攻击击退基础属性为零时，百分比本身不会增加击退 |
| 下落增益 | Gravitated / Dynamic 在近战攻击者为玩家且竖直速度 `< -0.1` 时对伤害额外乘 1.08 / 1.10；不再只修改原版 1.5 倍暴击常量 |
| 双手 | 近战事件读取主副手符合目标的词缀；被动属性分别绑定主副手槽，穿戴护甲仍仅绑定其对应穿戴槽 |
| 护甲触发 | 上游当前默认护甲使用的 `take_damage` 排除实体攻击、虚空和强制击杀；本版遵循该范围，摔落等绕过普通护甲的环境伤害仍可受词缀影响。原生扩展另有 `all` / `fall` / `fire` / `projectile` 范围，以词缀说明为准 |
| 条件 | 空中/水中/夜间/生命条件在效果触发时检查；夜间取有天空光照维度的第 13000–22999 tick。护甲检查穿戴者，攻击检查攻击者 |
| 旧装备 | 装备时替换本模组拥有的属性修饰器，修复旧版击退及主副手范围；保留外部属性、名称、耐久和已付费次数 |

显示依据：[ReforgesDisplay.kt](https://github.com/Auxilor/Reforges/blob/8ee14ce9c6d979d5fa5a7e71a6e2e4df964d2d4e/eco-core/core-plugin/src/main/kotlin/com/willfp/reforges/display/ReforgesDisplay.kt)。菜单、价格和重复石头依据：[ReforgeGUI.kt](https://github.com/Auxilor/Reforges/blob/8ee14ce9c6d979d5fa5a7e71a6e2e4df964d2d4e/eco-core/core-plugin/src/main/kotlin/com/willfp/reforges/gui/ReforgeGUI.kt)。默认值及渐变依据：[默认词缀 YAML](https://github.com/Auxilor/Reforges/tree/8ee14ce9c6d979d5fa5a7e71a6e2e4df964d2d4e/eco-core/core-plugin/src/main/resources/reforges)。

下落和护甲范围可核查：[crit_multiplier](https://github.com/Auxilor/libreforge/blob/2026.39/core/common/src/main/kotlin/com/willfp/libreforge/effects/impl/EffectCritMultiplier.kt)、[take_damage](https://github.com/Auxilor/libreforge/blob/2026.39/core/common/src/main/kotlin/com/willfp/libreforge/triggers/impl/TriggerTakeDamage.kt)。这里只对照可观察行为；未复制或打包 libreforge 实现。

## 保留的原生差异

- 经济继续使用经验：普通初始 3 级、30 级封顶；灵动默认 40 级，对应上游 100000 / 7500 的相对价差；丰饶固定 3 级。丰饶击杀奖 5 经验点，勘探 5% 概率奖 2 点，而非金币。已有配置不被覆盖。
- 界面继续使用辉月重铸台和月环动画，没有复制 Bukkit 箱子菜单；模型、图标和特效为原生资源。石头不采用上游玩家头像贴图表达式。
- 弓与三叉戟使用 Minecraft 提供的发射武器快照，发射后换武器不会替换该弹丸的词缀。上游弓触发器按命中时持有者取效果、三叉戟缓存发射时持有者；本版不模拟整个持有者集合。离手投射物也不继承额外副手效果。
- 原生 `killXp` 绑定造成击杀的武器；不模拟上游 `kill` 触发器对任意当前持有道具的全部联动。被动属性在正确槽位生效，不受事件 `chance / condition` 控制。
- 自定义配置为严格 JSON，只支持文档列出的原生字段。支持 eco 渐变和常见颜色/格式码，未实现完整 MiniMessage、占位符或脚本。旧自定义物品保留参数快照；插件的集中 ID 查询/热重载模式不同。
- 原版标签的剑、斧、镐、弓/弩，以及 26.2 的矛可以匹配；重锤继续作为原生近战扩展。第三方投射物需要专门适配。
- 菜单由服务端校验拥有者、输入版本、经验、石头匹配和 5 tick 间隔；成功后才发送动画回执。关闭与物品转移使用原版库存规则。

不承诺兼容插件 YAML、任意 libreforge 效果链、权限折扣、经济插件、PlaceholderAPI、Bukkit 事件或旧 Bukkit PDC 存档。26.2 与 1.21.1 使用独立产物，不能把两个加载器或两个游戏版本的 JAR 安装在同一实例。

配置方式见 [自定义词缀](custom-reforges.md)，实际测试范围见 [验证记录](verification.md)。

0.4.0 的内置说明由 `ReforgeLore` 从数值生成，统一数值前置与增益/代价颜色。点燃、缓慢、生命汲取及其共用冷却是原生扩展，并非上游默认 16 个词缀的行为；完整数值见 [词缀目录](reforges.md)。
