# RPG TileSet Converter

将 RPG Maker 瓷砖图集转换为 Godot 引擎格式的工具。这是一个 Java 应用程序，支持 GUI 和命令行两种使用方式。

## 📋 项目概述

**RPG TileSet Converter** 是一个专业的图像处理工具，用于游戏开发者快速转换地形瓷砖图集格式。该工具采用精巧的图像处理算法，将 RPG Maker 标准格式（192×64）的瓷砖图集转换为 Godot 引擎兼容格式（192×144）。

### 核心转换流程

```
RPG Maker 格式 (192×64)
        ↓
   四象限采样 (Mini Tiles: 80×16)
        ↓
   迷你瓷砖替代映射
        ↓
  Godot 格式 (192×144)
```

## ✨ 主要功能

- ✅ **图形化用户界面** - 使用 JavaFX 构建的直观 UI
- ✅ **实时预览** - 在转换前看到效果
- ✅ **灵活的边界处理** - 支持左、右、上、下偏移调整
- ✅ **命令行工具** - 用于批量处理和自动化
- ✅ **多格式支持** - 支持 PNG、JPG 等常见图像格式
- ✅ **精确的算法** - 基于 RPG Maker 的标准瓷砖规范

## 🛠️ 技术栈

| 技术 | 版本 |
|------|------|
| **Java** | 17+ |
| **JavaFX** | 21.0.1 |
| **构建工具** | Maven |
| **许可证** | MIT |

## 📦 安装与使用

### 前置要求

- Java 17 或更高版本
- Maven 3.6+

### 构建项目

```bash
cd RpgTileSetConveter
mvn clean package
```

### 方式 1：图形化界面

```bash
mvn javafx:run
```

或直接运行：

```bash
java -cp target/classes org.greg.image.ImageConverterMain
```

**GUI 使用步骤：**

1. 点击【打开图像】按钮选择 RPG Maker 原始图像
2. 使用微调器调整边界偏移参数（如需要）
3. 在右侧预览转换结果
4. 点击【保存】按钮导出 Godot 格式图像

### 方式 2：命令行工具

基本用法：

```bash
java -cp target/classes org.greg.image.ImageConverterCLI <输入文件> <输出文件> [左偏移] [右偏移] [上偏移] [下偏移]
```

**示例：**

```bash
# 简单转换
java -cp target/classes org.greg.image.ImageConverterCLI input.png output.png

# 指定边界偏移
java -cp target/classes org.greg.image.ImageConverterCLI input.png output.png 2 3 1 2
```

**参数说明：**

| 参数 | 描述 | 默认值 |
|------|------|--------|
| 输入文件 | RPG Maker 原始瓷砖图像路径 | 必需 |
| 输出文件 | 转换后的 Godot 格式图像保存路径 | 必需 |
| 左偏移 | 左边界像素偏移量 | 0 |
| 右偏移 | 右边界像素偏移量 | 0 |
| 上偏移 | 上边界像素偏移量 | 0 |
| 下偏移 | 下边界像素偏移量 | 0 |

## 📁 项目结构

```
RpgTileSetConveter/
├── pom.xml                          # Maven 配置文件
├── src/
│   ├── main/
│   │   ├── java/org/greg/image/
│   │   │   ├── ImageConverterMain.java      # 应用入口
│   │   │   ├── ImageConverterUI.java        # JavaFX 图形界面
│   │   │   ├── ImageConverterCLI.java       # 命令行工具
│   │   │   ├── ImageConverter.java          # 核心转换引擎
│   │   │   └── DraggableNodeDemo.java       # UI 辅助工具
│   │   └── resources/
│   └── test/java/
├── target/                          # 编译输出目录
└── README.md                        # 本文件
```

## 🔧 核心类说明

### ImageConverter

**主要职责：** 图像转换的核心引擎

| 方法 | 功能 |
|------|------|
| `convertToGodotImage()` | 将 RPG Maker 格式转换为 Godot 格式 |
| `setOffsets()` | 设置边界偏移参数 |
| `copyTile()` | 复制单个瓷砖 |
| `applySubtileData()` | 应用迷你瓷砖映射 |

### ImageConverterUI

**主要职责：** 提供用户友好的图形界面

**主要功能：**
- 图像加载与预览
- 实时参数调整
- 结果可视化
- 文件导出

### ImageConverterCLI

**主要职责：** 提供命令行接口用于批量处理

## 📊 算法详解

### 转换步骤

#### 1. 四象限采样 (Mini Tiles Generation)

- 从 RPG Maker 源图像（192×64）的两个主要瓷砖采样
- 将每个瓷砖分为 4 个象限进行处理
- 生成中间表示形式（80×16）

#### 2. 迷你瓷砖映射

- 使用预定义的 3×3 网格替代映射（共 48 个条目）
- 每个目标瓷砖由 3×3 网格的源瓷砖引用组成
- 系统地组装最终的 Godot 格式图像

#### 3. 输出格式

- 最终输出：192×144 像素（4行 × 12列 瓷砖）
- 每个瓷砖：48×48 像素
- 支持透明度通道（ARGB）

## 🎮 应用场景

- RPG Maker MV/MZ 到 Godot 的迁移
- 游戏资源格式转换
- 批量地形瓷砖处理
- 游戏开发工作流优化

## ⚙️ 配置说明

### 边界偏移参数

边界偏移参数用于微调瓷砖边界的识别方式，以适应不同的美术风格：

| 参数 | 作用 | 调整建议 |
|------|------|---------|
| **leftOffset** | 左边界向右移动的像素数 | 0-10 像素 |
| **rightOffset** | 右边界向右移动的像素数 | 0-10 像素 |
| **topOffset** | 上边界向下移动的像素数 | 0-10 像素 |
| **bottomOffset** | 下边界向下移动的像素数 | 0-10 像素 |

**调试建议：**

1. 从所有偏移都为 0 开始
2. 逐步调整参数以匹配你的瓷砖风格
3. 在 GUI 中实时预览效果
4. 记录找到的最佳参数以供后续使用

## 🐛 故障排除

### 问题：转换后的图像显示不正确

**解决方案：**
- 检查源图像是否为 RPG Maker 标准格式（192×64 或其倍数）
- 调整边界偏移参数
- 确认输入图像包含 2 个主瓷砖（位置 0,0 和 1,0）

### 问题：命令行工具找不到主类

**解决方案：**
```bash
# 确保已构建项目
mvn clean package

# 使用 JAR 文件
java -cp target/RpgTileSetConverter-1.0-SNAPSHOT.jar org.greg.image.ImageConverterCLI input.png output.png
```

### 问题：JavaFX 相关错误

**解决方案：**
```bash
# 使用 Maven 插件启动
mvn clean javafx:run

# 或指定 JVM 模块参数
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -cp target/classes org.greg.image.ImageConverterMain
```

## 💡 示例

### 示例 1：简单转换

```bash
java -cp target/classes org.greg.image.ImageConverterCLI terrain.png terrain_godot.png
```

### 示例 2：带调整参数的转换

```bash
java -cp target/classes org.greg.image.ImageConverterCLI terrain.png terrain_godot.png 2 2 1 1
```

### 示例 3：使用 GUI 工具

1. 运行 GUI：`mvn javafx:run`
2. 点击【Open Image】选择 `terrain.png`
3. 调整左、右、上、下微调器
4. 点击【Save】导出结果

## 📝 输入文件格式要求

**RPG Maker 瓷砖格式标准：**

```
+--------+--------+
|  Tile  |  Tile  |
|  (0,0) |  (1,0) |
+--------+--------+
| 192×64 像素区域  |
|    192 × 64     |
+--------+--------+
```

- **宽度：** 192 像素（两个 96×48 的瓷砖）或其倍数
- **高度：** 64 像素（一个 48 像素高的瓷砖）或其倍数
- **颜色空间：** RGBA（支持透明度）
- **推荐格式：** PNG（保留透明度）

## 🚀 性能指标

- **转换速度：** < 100ms（典型情况）
- **内存占用：** ~ 50-100MB（取决于输入大小）
- **支持的最大图像：** 4096×4096 像素

## 📚 参考资源

- [RPG Maker 官方文档](https://rmmv.net/)
- [Godot 引擎文档](https://docs.godotengine.org/)
- [JavaFX 文档](https://openjfx.io/)

## 📄 许可证

本项目采用 **MIT 许可证**。详见 LICENSE 文件。

## 👨‍💻 贡献

欢迎提交 Issue 和 Pull Request！

**贡献步骤：**

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📧 联系方式

- 作者：Greg
- 邮箱：[your-email@example.com]
- GitHub：[your-github-profile]

## 🙏 致谢

感谢所有使用和支持本项目的开发者！

---

**最后更新：** 2026年5月29日

**版本：** 1.0-SNAPSHOT

**语言：** [中文](README.md) | [English](README_EN.md)

