# Better Slabs

> ai生成, 我不知道准确性, 也别问

Paper/Folia 竖半砖插件，理论支持 **1.19.4+**，提供完整的竖向半砖放置、破坏和碰撞系统。

## 特性

- ✅ **竖半砖系统** - 在任意方向放置半砖（东西南北）
- ✅ **智能碰撞** - 使用 BlockDisplay + Interaction 实现精确碰撞
- ✅ **Folia 支持** - 完全兼容 Folia 多线程服务端
- ✅ **材质包可选** - 可选材质包改变物品栏外观
- ✅ **可配置** - 语言配置、排除特定半砖类型
- ✅ **H2 数据库** - 高性能持久化存储
- ✅ **区块优化** - 智能区块索引，高效加载卸载

## 版本要求

| 组件 | 最低版本 | 说明 |
|------|---------|------|
| **插件** | **1.19.4** | BlockDisplay API 可用版本 |
| **服务端** | Paper/Folia | 需要 Adventure API 支持 |
| 材质包 | 1.19.4+ | 双格式 CMD（兼容新旧客户端）|
| 数据包 | 1.21+ | 可选，纯原版服使用 |

## 安装

1. 下载最新版本的 `BetterSlab-1.0.0.jar`
2. 放入服务器 `plugins` 目录
3. 重启服务器
4. （可选）安装材质包到客户端

## 使用方法

### 基础操作

- **获取竖半砖**: `/betterslabs give <材质> [数量]`
- **切换模式**: 手持半砖，`Shift + 右键空气` 切换普通/竖半砖模式
- **放置**: 右键放置竖半砖
- **破坏**: 
  - 创造模式: 左键直接破坏
  - 生存模式: 长按左键挖掘（显示进度条）

### 命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/betterslabs reload` | `betterslabs.admin` | 重载配置 |
| `/betterslabs info` | `betterslabs.admin` | 查看插件信息 |
| `/betterslabs give <材质> [数量]` | `betterslabs.admin` | 给予竖半砖物品 |

### 配置文件

#### config.yml

```yaml
# 排除特定半砖（未来官方支持时使用）
# 示例:
#   - STONE_SLAB
#   - OAK_SLAB
ignore-slabs:
```

#### lang.yml

完整的语言配置文件，支持自定义所有文本：

```yaml
# 物品栏竖半砖 Lore
item:
  vertical-slab-lore:
    - "§7状态: 竖半砖"
    - "§8Shift+右键空气切换"

# 挖掘进度显示
dig:
  title: "§6挖掘中"
  subtitle: "§7{bar} {current}/{total}s ({percent}%)"
  actionbar: "§6挖掘 {percent}%"

# 命令反馈
command:
  no-permission: "§c无权限。"
  reload-success: "§aBetter Slabs 已重载。"
  # ... 更多配置
```

## 构建

### 前置要求

- Java 17+
- Gradle 8.0+

### 构建步骤

```bash
# Windows
gradlew.bat shadowJar

# Linux/Mac
./gradlew shadowJar
```

构建输出: `build/libs/BetterSlab-1.0.0.jar`

### 生成材质包和数据包

```bash
gradlew.bat generateAllPacks
```

## 技术细节

### 架构

- **SlabStorage** - H2 数据库持久化，支持区块索引
- **DisplayManager** - BlockDisplay 渲染管理
- **CollisionManager** - Interaction 碰撞体管理
- **SlabManager** - 核心逻辑：放置、破坏、合并
- **SlabRegistry** - 支持的半砖类型注册

### 性能优化

- ✅ 区块索引加速查询
- ✅ ConcurrentHashMap 并发安全
- ✅ 数据库连接健康检查
- ✅ DisplayEntity 背面剔除优化（预留）
- ✅ Folia 区域调度支持

### 材质包说明

材质包使用双格式 CustomModelData：

| 客户端版本 | 格式 | 文件位置 |
|-----------|------|---------|
| 1.19.4 – 1.21.3 | `overrides` | `models/item/*_slab.json` |
| 1.21.4+ | `range_dispatch` | `items/*_slab.json` |

**一份材质包同时兼容新旧客户端**，无需分发多个版本。

## 为什么不从 client.jar 提取贴图？

- **世界竖半砖** = BlockDisplay 显示原版完整方块 → 使用客户端自带贴图
- **物品栏竖半砖** = 父模型 + 路径引用（如 `minecraft:block/oak_planks`）

无需嵌入 PNG 文件，也不依赖本地 JAR 文件。

如需查看客户端 JAR 中的方块贴图：

```bash
gradlew.bat listClientJarSlabs -PclientJar="路径/to/client.jar"
```

## 兼容性

- ✅ Paper 1.19.4+
- ✅ Folia 多线程服务端
- ✅ 多世界支持
- ✅ WorldGuard/Residence 等保护插件兼容

## 开发计划

- [ ] 背面剔除优化（减少不可见 Display 数量）
- [ ] 更多半砖合并规则
- [ ] API 接口开放

## 许可证

MIT License

## 作者

**Little_100**  
网站: [www.little100.cn](https://www.little100.cn)

## 贡献

欢迎提交 Issue 和 Pull Request！

## 更新日志

### v1.0.0 (2026-07-21)

- 🎉 初始版本发布
- ✅ 完整的竖半砖系统
- ✅ Folia 支持
- ✅ 语言配置系统
- ✅ 性能优化（区块索引、并发安全）

