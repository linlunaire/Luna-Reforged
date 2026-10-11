# 构建

项目自有源码、辅助工具和 Gradle 构建脚本均使用 Kotlin。Gradle Wrapper 保留官方启动脚本；构建、资源生成和打包不需要 Python。

26.2 使用 JDK 25 和 Gradle 9.5.1，1.21.1 使用 JDK 21 和 Gradle 8.14.5。请在仓库根目录执行：

```powershell
$env:JAVA_HOME = '你的 JDK 25 路径'
.\versions\26.2\gradlew.bat -p versions/26.2 build
$env:JAVA_HOME = '你的 JDK 21 路径'
.\versions\1.21.1\gradlew.bat -p versions/1.21.1 build
.\versions\1.21.1\gradlew.bat -p tools verifyArtifacts packageSources
```

产物输出至 `build/release/<游戏版本>/`。规则检查随构建执行；验包会读取实际发行 JAR，检查字节码目标、运行库边界、加载器元数据、Mixin 映射和资源。

## 辅助工具

工具位于 `tools/src/main/kotlin/`，使用已有 Wrapper 即可运行，要求 Java 21 或以上。下列命令可使用任一版本的 Wrapper：

```powershell
# 重新生成方块模型、配方和中英资源
.\versions\1.21.1\gradlew.bat -p tools generateAssets
# 检查四个消费者和两个 1.21.1 兼容前置
.\versions\1.21.1\gradlew.bat -p tools verifyArtifacts
# 归档玩法项目的完整源码
.\versions\1.21.1\gradlew.bat -p tools packageSources
# 组合四种安装方案、匹配的 LunaCore、文档、完整源码和校验清单
.\versions\1.21.1\gradlew.bat -p tools packageRelease `
  '-Plunacore26=原生 LunaCore 26.2 JAR 目录' `
  '-PlunacoreSource=匹配的 LunaCore 源码目录'
```

`packageRelease` 会先运行验包和源码归档，再检查 1.21.1 兼容前置的策略源码与传入的 LunaCore 源码逐字节一致，并校验最终 ZIP 的 CRC。两套完整源码、Wrapper 和许可证一并打包。

## 代码边界

- `domain` 保存与 Minecraft 无关的词缀和价格规则。
- `common` 处理物品组件、服务端交易与效果；`client/ReforgePresentation` 只保存当前屏幕的文本和排版，不持有世界或玩家。
- `versions/<版本>` 适配游戏 API 和实际绘制入口。两版界面共享展示状态与动画逻辑，26.2 通过原生 GUI 管线绘制。
- LunaCore 保持独立 JVM 策略库与共享 Kotlin 运行库；1.21.1 兼容包重新编译相同策略源码至 Java 21。

新增词缀或展示状态时，扩展其所属层，并覆盖配置/组件变更后的刷新；请勿为可变装备或世界对象增加无失效机制的全局缓存。

CI 按游戏版本分别构建，并使用 `verifyArtifacts -PminecraftVersion=<版本>` 检查该任务产物。1.21.1 的 Kotlin Mixin 目标由 Loom 直接映射到发行命名空间，验包会检查 JAR 内注解中的实际目标。

服务端检查与真实客户端联机步骤见 [验证说明](verification.md)。

26.2 的 Vulkan 预览使用 `runClient -Ppreview -PpreviewBackend=vulkan`。还需在对应 `run/client-preview/options.txt` 中设置 `preferredGraphicsBackend:"vulkan"`；NeoForge 26.2 的启动窗口设置见 [安装说明](usage.md#262-原生-vulkan)。`previewBackend` 会核对实际 GPU 后端，回退到 OpenGL 时检查失败，避免把成功启动误计为 Vulkan 通过。
