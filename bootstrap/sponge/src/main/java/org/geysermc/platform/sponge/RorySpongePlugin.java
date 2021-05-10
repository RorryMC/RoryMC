/*
 * Copyright (c) 2019-2021 RoryMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author RoryMC
 * @link https://github.com/RoryMC/Rory
 */

package org.geysermc.platform.sponge;

import com.google.inject.Inject;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.RoryConnector;
import org.geysermc.connector.bootstrap.RoryBootstrap;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.connector.configuration.RoryConfiguration;
import org.geysermc.connector.dump.BootstrapDumpInfo;
import org.geysermc.connector.ping.RoryLegacyPingPassthrough;
import org.geysermc.connector.ping.IRoryPingPassthrough;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.platform.sponge.command.RorySpongeCommandExecutor;
import org.geysermc.platform.sponge.command.RorySpongeCommandManager;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedEvent;
import org.spongepowered.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.UUID;

@Plugin(id = "geyser", name = RoryConnector.NAME + "-Sponge", version = RoryConnector.VERSION, url = "https://geysermc.org", authors = "RoryMC")
public class RorySpongePlugin implements RoryBootstrap {

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    private RorySpongeCommandManager geyserCommandManager;
    private RorySpongeConfiguration geyserConfig;
    private RorySpongeLogger geyserLogger;
    private IRoryPingPassthrough geyserSpongePingPassthrough;

    private RoryConnector connector;

    @Override
    public void onEnable() {
        if (!configDir.exists())
            configDir.mkdirs();

        File configFile = null;
        try {
            configFile = FileUtils.fileOrCopiedFromResource(new File(configDir, "config.yml"), "config.yml", (file) -> file.replaceAll("generateduuid", UUID.randomUUID().toString()));
        } catch (IOException ex) {
            logger.warn(LanguageUtils.getLocaleStringLog("geyser.config.failed"));
            ex.printStackTrace();
        }

        try {
            this.geyserConfig = FileUtils.loadConfig(configFile, RorySpongeConfiguration.class);
        } catch (IOException ex) {
            logger.warn(LanguageUtils.getLocaleStringLog("geyser.config.failed"));
            ex.printStackTrace();
            return;
        }

        if (Sponge.getServer().getBoundAddress().isPresent()) {
            InetSocketAddress javaAddr = Sponge.getServer().getBoundAddress().get();

            // Don't change the ip if its listening on all interfaces
            // By default this should be 127.0.0.1 but may need to be changed in some circumstances
            if (this.geyserConfig.getRemote().getAddress().equalsIgnoreCase("auto")) {
                this.geyserConfig.setAutoconfiguredRemote(true);
                geyserConfig.getRemote().setPort(javaAddr.getPort());
            }
        }

        if (geyserConfig.getBedrock().isCloneRemotePort()) {
            geyserConfig.getBedrock().setPort(geyserConfig.getRemote().getPort());
        }

        this.geyserLogger = new RorySpongeLogger(logger, geyserConfig.isDebugMode());
        RoryConfiguration.checkRoryConfiguration(geyserConfig, geyserLogger);
        this.connector = RoryConnector.start(PlatformType.SPONGE, this);

        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserSpongePingPassthrough = RoryLegacyPingPassthrough.init(connector);
        } else {
            this.geyserSpongePingPassthrough = new RorySpongePingPassthrough();
        }

        this.geyserCommandManager = new RorySpongeCommandManager(Sponge.getCommandManager(), connector);
        Sponge.getCommandManager().register(this, new RorySpongeCommandExecutor(connector), "geyser");
    }

    @Override
    public void onDisable() {
        connector.shutdown();
    }

    @Override
    public RorySpongeConfiguration getRoryConfig() {
        return geyserConfig;
    }

    @Override
    public RorySpongeLogger getRoryLogger() {
        return geyserLogger;
    }

    @Override
    public CommandManager getRoryCommandManager() {
        return this.geyserCommandManager;
    }

    @Override
    public IRoryPingPassthrough getRoryPingPassthrough() {
        return geyserSpongePingPassthrough;
    }

    @Override
    public Path getConfigFolder() {
        return configDir.toPath();
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        onEnable();
    }

    @Listener
    public void onServerStop(GameStoppedEvent event) {
        onDisable();
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new RorySpongeDumpInfo();
    }

    @Override
    public String getMinecraftServerVersion() {
        return Sponge.getPlatform().getMinecraftVersion().getName();
    }
}
