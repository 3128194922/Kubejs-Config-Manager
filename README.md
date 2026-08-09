# KubeJS Config Manager

KubeJS 附加模组,用于管理配置文件的分发。基于版本号比较,自动将 `kubejs/` 下的配置源目录同步到游戏根目录的 `config/` 与 `defaultconfigs/`。

## 功能

- **config 同步**:将 `kubejs/config-manager/` 下的文件同步到 `config/`
- **defaultconfigs 同步**:将 `kubejs/default-config-manager/` 下的文件同步到 `defaultconfigs/`
- 两路同步各自维护独立的 `version.json`,互不干扰
- 仅当源版本号高于目标版本号时执行覆盖复制,避免重复同步

## 工作机制

模组在加载时,对每路同步执行以下流程:

1. 若 `kubejs/<源目录>/` 不存在,自动创建并写入默认 `version.json`(`{"version": "1.0.0"}`)
2. 读取 `kubejs/<源目录>/version.json` 中的版本号
3. 读取目标目录下的 `version.json` 中的版本号
   - 若目标目录无 `version.json`,跳过本次同步
4. 按语义化版本号比较(如 `1.0.0` < `1.2.0` < `2.0.0`)
5. 若源版本更高,将源目录下所有文件(保留相对路径结构)覆盖复制到目标目录

## 目录结构

```
游戏根目录/
├── kubejs/
│   ├── config-manager/          # config 同步源
│   │   ├── version.json         # {"version": "1.0.0"}
│   │   └── ...任意配置文件/子目录
│   └── default-config-manager/  # defaultconfigs 同步源
│       ├── version.json         # {"version": "1.0.0"}
│       └── ...任意默认配置文件/子目录
├── config/
│   └── version.json             # 由同步写入,记录当前 config 版本
└── defaultconfigs/
    └── version.json             # 由同步写入,记录当前 defaultconfigs 版本
```

## 使用方法

1. 在 `kubejs/config-manager/` 放置需要分发的 `config/` 配置文件
2. 在 `kubejs/default-config-manager/` 放置需要分发的 `defaultconfigs/` 默认配置文件
3. 修改对应源目录下的 `version.json`,将版本号调高(例如从 `1.0.0` 改为 `1.1.0`)
4. 启动游戏,模组会自动检测版本差异并完成同步

## 日志说明

两路同步的日志分别以 `[ConfigManager]` 与 `[DefaultConfigManager]` 作为前缀,便于区分:

```
[ConfigManager] New config version detected: 1.0.0 -> 1.1.0, syncing...
[ConfigManager] Sync completed
[DefaultConfigManager] No defaultconfigs version found, skipping sync
```

## 环境要求

- Minecraft 1.20.1
- Forge 47.x
- KubeJS 2001.6.4+

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/`。
