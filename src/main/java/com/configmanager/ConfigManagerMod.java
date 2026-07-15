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
        syncConfigs();
    }

    private void syncConfigs() {
        Path gameDir = FMLPaths.GAMEDIR.get();
        Path kubejsConfigDir = gameDir.resolve("kubejs").resolve("config-manager");
        Path targetConfigDir = gameDir.resolve("config");
        Path kubejsVersionFile = kubejsConfigDir.resolve("version.json");
        Path targetVersionFile = targetConfigDir.resolve("version.json");

        // 确保 kubejs/config-manager 文件夹存在
        if (!Files.isDirectory(kubejsConfigDir)) {
            try {
                Files.createDirectories(kubejsConfigDir);
                JsonObject defaultVersion = new JsonObject();
                defaultVersion.addProperty("version", "1.0.0");
                Files.writeString(kubejsVersionFile, GSON.toJson(defaultVersion));
                LOGGER.info("[ConfigManager] Created kubejs/config-manager/ with default version.json");
                return;
            } catch (IOException e) {
                LOGGER.error("[ConfigManager] Failed to create kubejs/config-manager/", e);
                return;
            }
        }

        if (!Files.isRegularFile(kubejsVersionFile)) {
            try {
                JsonObject defaultVersion = new JsonObject();
                defaultVersion.addProperty("version", "1.0.0");
                Files.writeString(kubejsVersionFile, GSON.toJson(defaultVersion));
                LOGGER.info("[ConfigManager] Created default version.json");
            } catch (IOException e) {
                LOGGER.error("[ConfigManager] Failed to create version.json", e);
                return;
            }
        }

        String kubejsVersion = readVersion(kubejsVersionFile);
        if (kubejsVersion == null) {
            LOGGER.error("[ConfigManager] Failed to read version from kubejs/config-manager/version.json");
            return;
        }

        String targetVersion = readVersion(targetVersionFile);

        if (targetVersion == null) {
            LOGGER.info("[ConfigManager] No config version found, skipping sync");
            return;
        }

        if (compareVersions(kubejsVersion, targetVersion) > 0) {
            LOGGER.info("[ConfigManager] New config version detected: {} -> {}, syncing...", targetVersion, kubejsVersion);
            copyConfigFiles(kubejsConfigDir, targetConfigDir);
        } else {
            LOGGER.info("[ConfigManager] Config version unchanged ({}), skipping sync", targetVersion);
        }
    }

    private String readVersion(Path versionFile) {
        if (!Files.isRegularFile(versionFile)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(versionFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null && json.has("version")) {
                return json.get("version").getAsString();
            }
        } catch (IOException e) {
            LOGGER.error("[ConfigManager] Failed to read {}", versionFile, e);
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
     * 将 kubejs/config-manager 下的所有文件（除 version.json 自身）复制到 config 目录。
     */
    private void copyConfigFiles(Path sourceDir, Path targetDir) {
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            LOGGER.error("[ConfigManager] Failed to create target config directory", e);
            return;
        }

        try (Stream<Path> files = Files.walk(sourceDir)) {
            files.filter(Files::isRegularFile).forEach(sourceFile -> {
                Path relativePath = sourceDir.relativize(sourceFile);
                Path targetFile = targetDir.resolve(relativePath);

                try {
                    Files.createDirectories(targetFile.getParent());
                    Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.debug("[ConfigManager] Copied: {}", relativePath);
                } catch (IOException e) {
                    LOGGER.error("[ConfigManager] Failed to copy {}: {}", relativePath, e.getMessage());
                }
            });
            LOGGER.info("[ConfigManager] Config sync completed");
        } catch (IOException e) {
            LOGGER.error("[ConfigManager] Failed to walk source directory", e);
        }
    }
}