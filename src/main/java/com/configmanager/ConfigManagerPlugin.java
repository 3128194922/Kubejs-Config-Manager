package com.configmanager;

import dev.latvian.mods.kubejs.KubeJSPlugin;

public class ConfigManagerPlugin extends KubeJSPlugin {
    @Override
    public void init() {
        ConfigManagerMod.LOGGER.info("ConfigManager KubeJS plugin initialized");
    }
}