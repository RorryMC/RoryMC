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

package org.geysermc.platform.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.RoryConnector;
import org.geysermc.connector.bootstrap.RoryBootstrap;
import org.geysermc.connector.configuration.RoryConfiguration;
import org.geysermc.connector.dump.BootstrapDumpInfo;
import org.geysermc.connector.ping.RoryLegacyPingPassthrough;
import org.geysermc.connector.ping.IRoryPingPassthrough;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.LanguageUtils;
import org.geysermc.platform.velocity.command.RoryVelocityCommandExecutor;
import org.geysermc.platform.velocity.command.RoryVelocityCommandManager;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Plugin(id = "geyser", name = RoryConnector.NAME + "-Velocity", version = RoryConnector.VERSION, url = "https://geysermc.org", authors = "RoryMC")
public class RoryVelocityPlugin implements RoryBootstrap {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer proxyServer;

    @Inject
    private CommandManager commandManager;

    private RoryVelocityCommandManager geyserCommandManager;
    private RoryVelocityConfiguration geyserConfig;
    private RoryVelocityLogger geyserLogger;
    private IRoryPingPassthrough geyserPingPassthrough;

    private RoryConnector connector;

    @Getter
    private final Path configFolder = Paths.get("plugins/" + RoryConnector.NAME + "-Velocity/");

    @Override
    public void onEnable() {
        try {
            if (!configFolder.toFile().exists())
                //noinspection ResultOfMethodCallIgnored
                configFolder.toFile().mkdirs();
            File configFile = FileUtils.fileOrCopiedFromResource(configFolder.resolve("config.yml").toFile(), "config.yml", (x) -> x.replaceAll("generateduuid", UUID.randomUUID().toString()));
            this.geyserConfig = FileUtils.loadConfig(configFile, RoryVelocityConfiguration.class);
        } catch (IOException ex) {
            logger.warn(LanguageUtils.getLocaleStringLog("geyser.config.failed"), ex);
            ex.printStackTrace();
        }

        InetSocketAddress javaAddr = proxyServer.getBoundAddress();

        // By default this should be localhost but may need to be changed in some circumstances
        if (this.geyserConfig.getRemote().getAddress().equalsIgnoreCase("auto")) {
            this.geyserConfig.setAutoconfiguredRemote(true);
            // Don't use localhost if not listening on all interfaces
            if (!javaAddr.getHostString().equals("0.0.0.0") && !javaAddr.getHostString().equals("")) {
                this.geyserConfig.getRemote().setAddress(javaAddr.getHostString());
            }
            geyserConfig.getRemote().setPort(javaAddr.getPort());
        }

        if (geyserConfig.getBedrock().isCloneRemotePort()) {
            geyserConfig.getBedrock().setPort(javaAddr.getPort());
        }

        this.geyserLogger = new RoryVelocityLogger(logger, geyserConfig.isDebugMode());
        RoryConfiguration.checkRoryConfiguration(geyserConfig, geyserLogger);

        if (geyserConfig.getRemote().getAuthType().equals("floodgate") && !proxyServer.getPluginManager().getPlugin("floodgate").isPresent()) {
            geyserLogger.severe(LanguageUtils.getLocaleStringLog("geyser.bootstrap.floodgate.not_installed") + " " + LanguageUtils.getLocaleStringLog("geyser.bootstrap.floodgate.disabling"));
            return;
        } else if (geyserConfig.isAutoconfiguredRemote() && proxyServer.getPluginManager().getPlugin("floodgate").isPresent()) {
            // Floodgate installed means that the user wants Floodgate authentication
            geyserLogger.debug("Auto-setting to Floodgate authentication.");
            geyserConfig.getRemote().setAuthType("floodgate");
        }

        geyserConfig.loadFloodgate(this, proxyServer, configFolder.toFile());

        this.connector = RoryConnector.start(PlatformType.VELOCITY, this);

        this.geyserCommandManager = new RoryVelocityCommandManager(connector);
        this.commandManager.register("geyser", new RoryVelocityCommandExecutor(connector));
        if (geyserConfig.isLegacyPingPassthrough()) {
            this.geyserPingPassthrough = RoryLegacyPingPassthrough.init(connector);
        } else {
            this.geyserPingPassthrough = new RoryVelocityPingPassthrough(proxyServer);
        }
    }

    @Override
    public void onDisable() {
        connector.shutdown();
    }

    @Override
    public RoryVelocityConfiguration getRoryConfig() {
        return geyserConfig;
    }

    @Override
    public RoryVelocityLogger getRoryLogger() {
        return geyserLogger;
    }

    @Override
    public org.geysermc.connector.command.CommandManager getRoryCommandManager() {
        return this.geyserCommandManager;
    }

    @Override
    public IRoryPingPassthrough getRoryPingPassthrough() {
        return geyserPingPassthrough;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        onEnable();
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        onDisable();
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new RoryVelocityDumpInfo(proxyServer);
    }
}
