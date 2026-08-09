package com.configmanager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.util.stream.Stream;

@Mod(ConfigManagerMod.MODID)
public class ConfigManagerMod {
    public static final String MODID = "configmanager";
    static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public ConfigManagerMod() {
        // 同步 config 目录
        syncConfigs("config-manager", "config", "ConfigManager");
        // 同步 defaultconfigs 目录
        syncConfigs("default-config-manager", "defaultconfigs", "DefaultConfigManager");
    }

    /**
     * 同步指定目录的配置文件。
     *
     * @param sourceSubDir kubejs 下的源子目录名（如 "config-manager"）
     * @param targetSubDir 游戏根目录下的目标子目录名（如 "config" 或 "defaultconfigs"）
     * @param tag          日志标签
     */
    private void syncConfigs(String sourceSubDir, String targetSubDir, String tag) {
        Path gameDir = FMLPaths.GAMEDIR.get();
        Path kubejsConfigDir = gameDir.resolve("kubejs").resolve(sourceSubDir);
        Path targetConfigDir = gameDir.resolve(targetSubDir);
        Path kubejsVersionFile = kubejsConfigDir.resolve("version.json");
        Path targetVersionFile = targetConfigDir.resolve("version.json");

        // 确保 kubejs/<sourceSubDir> 文件夹存在
        if (!Files.isDirectory(kubejsConfigDir)) {
            try {
                Files.createDirectories(kubejsConfigDir);
                JsonObject defaultVersion = new JsonObject();
                defaultVersion.addProperty("version", "1.0.0");
                Files.writeString(kubejsVersionFile, GSON.toJson(defaultVersion));
                LOGGER.info("[{}] Created kubejs/{}/ with default version.json", tag, sourceSubDir);
                return;
            } catch (IOException e) {
                LOGGER.error("[{}] Failed to create kubejs/{}/", tag, sourceSubDir, e);
                return;
            }
        }

        if (!Files.isRegularFile(kubejsVersionFile)) {
            try {
                JsonObject defaultVersion = new JsonObject();
                defaultVersion.addProperty("version", "1.0.0");
                Files.writeString(kubejsVersionFile, GSON.toJson(defaultVersion));
                LOGGER.info("[{}] Created default version.json", tag);
            } catch (IOException e) {
                LOGGER.error("[{}] Failed to create version.json", tag, e);
                return;
            }
        }

        String kubejsVersion = readVersion(kubejsVersionFile, tag);
        if (kubejsVersion == null) {
            LOGGER.error("[{}] Failed to read version from kubejs/{}/version.json", tag, sourceSubDir);
            return;
        }

        String targetVersion = readVersion(targetVersionFile, tag);

        if (targetVersion == null) {
            LOGGER.info("[{}] No {} version found, skipping sync", tag, targetSubDir);
            return;
        }

        if (compareVersions(kubejsVersion, targetVersion) > 0) {
            LOGGER.info("[{}] New {} version detected: {} -> {}, syncing...", tag, targetSubDir, targetVersion, kubejsVersion);
            copyConfigFiles(kubejsConfigDir, targetConfigDir, tag);
        } else {
            LOGGER.info("[{}] {} version unchanged ({}), skipping sync", tag, targetSubDir, targetVersion);
        }
    }

    private String readVersion(Path versionFile, String tag) {
        if (!Files.isRegularFile(versionFile)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(versionFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null && json.has("version")) {
                return json.get("version").getAsString();
            }
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to read {}", tag, versionFile, e);
        }
        return null;
    }

    /**
     * 比较两个语义化版本号。返回负数表示 v1 < v2，零表示相等，正数表示 v1 > v2。
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLen = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLen; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (num1 != num2) {
                return num1 - num2;
            }
        }
        return 0;
    }

    /**
     * 将源目录下的所有文件（除 version.json 自身）复制到目标目录。
     */
    private void copyConfigFiles(Path sourceDir, Path targetDir, String tag) {
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to create target directory", tag, e);
            return;
        }

        try (Stream<Path> files = Files.walk(sourceDir)) {
            files.filter(Files::isRegularFile).forEach(sourceFile -> {
                Path relativePath = sourceDir.relativize(sourceFile);
                Path targetFile = targetDir.resolve(relativePath);

                try {
                    Files.createDirectories(targetFile.getParent());
                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.debug("[{}] Copied: {}", tag, relativePath);
                } catch (IOException e) {
                    LOGGER.error("[{}] Failed to copy {}: {}", tag, relativePath, e.getMessage());
                }
            });
            LOGGER.info("[{}] Sync completed", tag);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to walk source directory", tag, e);
        }
    }
}
