/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nukkitx.network.raknet.RakNetConstants;
import com.nukkitx.network.util.EventLoops;
import com.nukkitx.protocol.bedrock.BedrockServer;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.bootstrap.RoryBootstrap;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.connector.common.AuthType;
import org.geysermc.connector.configuration.RoryConfiguration;
import org.geysermc.connector.metrics.Metrics;
import org.geysermc.connector.network.ConnectorServerEventHandler;
import org.geysermc.connector.network.session.RorySession;
import org.geysermc.connector.network.translators.BiomeTranslator;
import org.geysermc.connector.network.translators.EntityIdentifierRegistry;
import org.geysermc.connector.network.translators.PacketTranslatorRegistry;
import org.geysermc.connector.network.translators.collision.CollisionTranslator;
import org.geysermc.connector.network.translators.effect.EffectRegistry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.network.translators.item.PotionMixRegistry;
import org.geysermc.connector.network.translators.item.RecipeRegistry;
import org.geysermc.connector.network.translators.sound.SoundHandlerRegistry;
import org.geysermc.connector.network.translators.sound.SoundRegistry;
import org.geysermc.connector.network.translators.world.WorldManager;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.SkullBlockEntityTranslator;
import org.geysermc.connector.utils.*;
import org.jetbrains.annotations.Contract;

import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class RoryConnector {

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.IGNORE_UNDEFINED)
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

    public static final String NAME = "Rory";
    public static final String GIT_VERSION = "DEV"; // A fallback for running in IDEs
    public static final String VERSION = "DEV"; // A fallback for running in IDEs
    public static final String MINECRAFT_VERSION = "1.16.4 - 1.16.5";

    /**
     * Oauth client ID for Microsoft authentication
     */
    public static final String OAUTH_CLIENT_ID = "204cefd1-4818-4de1-b98d-513fae875d88";

    private static final String IP_REGEX = "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b";

    private final List<RorySession> players = new ArrayList<>();

    private static RoryConnector instance;

    @Setter
    private AuthType defaultAuthType;

    private boolean shuttingDown = false;

    private final ScheduledExecutorService generalThreadPool;

    private BedrockServer bedrockServer;
    private final PlatformType platformType;
    private final RoryBootstrap bootstrap;

    private Metrics metrics;

    private RoryConnector(PlatformType platformType, RoryBootstrap bootstrap) {
        long startupTime = System.currentTimeMillis();

        instance = this;

        this.bootstrap = bootstrap;

        RoryLogger logger = bootstrap.getRoryLogger();
        RoryConfiguration config = bootstrap.getRoryConfig();

        this.platformType = platformType;

        logger.info("******************************************");
        logger.info("");
        logger.info(LanguageUtils.getLocaleStringLog("geyser.core.load", NAME, VERSION));
        logger.info("");
        logger.info("******************************************");

        this.generalThreadPool = Executors.newScheduledThreadPool(config.getGeneralThreadPool());

        logger.setDebug(config.isDebugMode());

        PacketTranslatorRegistry.init();

        /* Initialize translators and registries */
        BiomeTranslator.init();
        BlockTranslator.init();
        BlockEntityTranslator.init();
        EffectRegistry.init();
        EntityIdentifierRegistry.init();
        ItemRegistry.init();
        ItemTranslator.init();
        CollisionTranslator.init();
        LocaleUtils.init();
        PotionMixRegistry.init();
        RecipeRegistry.init();
        SoundRegistry.init();
        SoundHandlerRegistry.init();

        ResourcePack.loadPacks();

        if (platformType != PlatformType.STANDALONE && config.getRemote().getAddress().equals("auto")) {
            // Set the remote address to localhost since that is where we are always connecting
            try {
                config.getRemote().setAddress(InetAddress.getLocalHost().getHostAddress());
            } catch (UnknownHostException ex) {
                logger.debug("Unknown host when trying to find localhost.");
                if (config.isDebugMode()) {
                    ex.printStackTrace();
                }
                config.getRemote().setAddress(InetAddress.getLoopbackAddress().getHostAddress());
            }
        }
        String remoteAddress = config.getRemote().getAddress();
        // Filters whether it is not an IP address or localhost, because otherwise it is not possible to find out an SRV entry.
        if (!remoteAddress.matches(IP_REGEX) && !remoteAddress.equalsIgnoreCase("localhost")) {
            int remotePort;
            try {
                // Searches for a server address and a port from a SRV record of the specified host name
                InitialDirContext ctx = new InitialDirContext();
                Attribute attr = ctx.getAttributes("dns:///_minecraft._tcp." + remoteAddress, new String[]{"SRV"}).get("SRV");
                // size > 0 = SRV entry found
                if (attr != null && attr.size() > 0) {
                    String[] record = ((String) attr.get(0)).split(" ");
                    // Overwrites the existing address and port with that from the SRV record.
                    config.getRemote().setAddress(remoteAddress = record[3]);
                    config.getRemote().setPort(remotePort = Integer.parseInt(record[2]));
                    logger.debug("Found SRV record \"" + remoteAddress + ":" + remotePort + "\"");
                }
            } catch (Exception | NoClassDefFoundError ex) { // Check for a NoClassDefFoundError to prevent Android crashes
                logger.debug("Exception while trying to find an SRV record for the remote host.");
                if (config.isDebugMode())
                    ex.printStackTrace(); // Otherwise we can get a stack trace for any domain that doesn't have an SRV record
            }
        }

        defaultAuthType = AuthType.getByName(config.getRemote().getAuthType());

        CooldownUtils.setDefaultShowCooldown(config.getShowCooldown());
        DimensionUtils.changeBedrockNetherId(config.isAboveBedrockNetherBuilding()); // Apply End dimension ID workaround to Nether
        SkullBlockEntityTranslator.ALLOW_CUSTOM_SKULLS = config.isAllowCustomSkulls();

        // https://github.com/RoryMC/Rory/issues/957
        RakNetConstants.MAXIMUM_MTU_SIZE = (short) config.getMtu();
        logger.debug("Setting MTU to " + config.getMtu());

        boolean enableProxyProtocol = config.getBedrock().isEnableProxyProtocol();
        bedrockServer = new BedrockServer(
                new InetSocketAddress(config.getBedrock().getAddress(), config.getBedrock().getPort()),
                1,
                EventLoops.commonGroup(),
                enableProxyProtocol
        );
        bedrockServer.setHandler(new ConnectorServerEventHandler(this));
        bedrockServer.bind().whenComplete((avoid, throwable) -> {
            if (throwable == null) {
                logger.info(LanguageUtils.getLocaleStringLog("geyser.core.start", config.getBedrock().getAddress(), String.valueOf(config.getBedrock().getPort())));
            } else {
                logger.severe(LanguageUtils.getLocaleStringLog("geyser.core.fail", config.getBedrock().getAddress(), String.valueOf(config.getBedrock().getPort())));
                throwable.printStackTrace();
            }
        }).join();

        if (config.getMetrics().isEnabled()) {
            metrics = new Metrics(this, "RoryMC", config.getMetrics().getUniqueId(), false, java.util.logging.Logger.getLogger(""));
            metrics.addCustomChart(new Metrics.SingleLineChart("players", players::size));
            // Prevent unwanted words best we can
            metrics.addCustomChart(new Metrics.SimplePie("authMode", () -> AuthType.getByName(config.getRemote().getAuthType()).toString().toLowerCase()));
            metrics.addCustomChart(new Metrics.SimplePie("platform", platformType::getPlatformName));
            metrics.addCustomChart(new Metrics.SimplePie("defaultLocale", LanguageUtils::getDefaultLocale));
            metrics.addCustomChart(new Metrics.SimplePie("version", () -> RoryConnector.VERSION));
            metrics.addCustomChart(new Metrics.AdvancedPie("playerPlatform", () -> {
                Map<String, Integer> valueMap = new HashMap<>();
                for (RorySession session : players) {
                    if (session == null) continue;
                    if (session.getClientData() == null) continue;
                    String os = session.getClientData().getDeviceOS().toString();
                    if (!valueMap.containsKey(os)) {
                        valueMap.put(os, 1);
                    } else {
                        valueMap.put(os, valueMap.get(os) + 1);
                    }
                }
                return valueMap;
            }));
            metrics.addCustomChart(new Metrics.AdvancedPie("playerVersion", () -> {
                Map<String, Integer> valueMap = new HashMap<>();
                for (RorySession session : players) {
                    if (session == null) continue;
                    if (session.getClientData() == null) continue;
                    String version = session.getClientData().getGameVersion();
                    if (!valueMap.containsKey(version)) {
                        valueMap.put(version, 1);
                    } else {
                        valueMap.put(version, valueMap.get(version) + 1);
                    }
                }
                return valueMap;
            }));

            String minecraftVersion = bootstrap.getMinecraftServerVersion();
            if (minecraftVersion != null) {
                Map<String, Map<String, Integer>> versionMap = new HashMap<>();
                Map<String, Integer> platformMap = new HashMap<>();
                platformMap.put(platformType.getPlatformName(), 1);
                versionMap.put(minecraftVersion, platformMap);

                metrics.addCustomChart(new Metrics.DrilldownPie("minecraftServerVersion", () -> {
                    // By the end, we should return, for example:
                    // 1.16.5 => (Spigot, 1)
                    return versionMap;
                }));
            }
        }

        boolean isGui = false;
        // This will check if we are in standalone and get the 'useGui' variable from there
        if (platformType == PlatformType.STANDALONE) {
            try {
                Class<?> cls = Class.forName("org.geysermc.platform.standalone.RoryStandaloneBootstrap");
                isGui = (boolean) cls.getMethod("isUseGui").invoke(cls.cast(bootstrap));
            } catch (Exception e) {
                logger.debug("Failed detecting if standalone is using a GUI; if this is a RoryConnect instance this can be safely ignored.");
            }
        }

        double completeTime = (System.currentTimeMillis() - startupTime) / 1000D;
        String message = LanguageUtils.getLocaleStringLog("geyser.core.finish.done", new DecimalFormat("#.###").format(completeTime)) + " ";
        if (isGui) {
            message += LanguageUtils.getLocaleStringLog("geyser.core.finish.gui");
        } else {
            message += LanguageUtils.getLocaleStringLog("geyser.core.finish.console");
        }
        logger.info(message);

        if (platformType == PlatformType.STANDALONE) {
            logger.warning(LanguageUtils.getLocaleStringLog("geyser.core.movement_warn"));
        }
    }

    public void shutdown() {
        bootstrap.getRoryLogger().info(LanguageUtils.getLocaleStringLog("geyser.core.shutdown"));
        shuttingDown = true;

        if (players.size() >= 1) {
            bootstrap.getRoryLogger().info(LanguageUtils.getLocaleStringLog("geyser.core.shutdown.kick.log", players.size()));

            // Make a copy to prevent ConcurrentModificationException
            final List<RorySession> tmpPlayers = new ArrayList<>(players);
            for (RorySession playerSession : tmpPlayers) {
                playerSession.disconnect(LanguageUtils.getPlayerLocaleString("geyser.core.shutdown.kick.message", playerSession.getLocale()));
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {
                    // Simulate a long-running Job
                    try {
                        while (true) {
                            if (players.size() == 0) {
                                return;
                            }

                            TimeUnit.MILLISECONDS.sleep(100);
                        }
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                }
            });

            // Block and wait for the future to complete
            try {
                future.get();
                bootstrap.getRoryLogger().info(LanguageUtils.getLocaleStringLog("geyser.core.shutdown.kick.done"));
            } catch (Exception e) {
                // Quietly fail
            }
        }

        generalThreadPool.shutdown();
        bedrockServer.close();
        players.clear();
        defaultAuthType = null;
        this.getCommandManager().getCommands().clear();

        bootstrap.getRoryLogger().info(LanguageUtils.getLocaleStringLog("geyser.core.shutdown.done"));
    }

    public void addPlayer(RorySession player) {
        players.add(player);
    }

    public void removePlayer(RorySession player) {
        players.remove(player);
    }

    /**
     * Gets a player by their current UUID
     *
     * @param uuid the uuid
     * @return the player or <code>null</code> if there is no player online with this UUID
     */
    @Contract("null -> null")
    public RorySession getPlayerByUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        for (RorySession session : players) {
            if (uuid.equals(session.getPlayerEntity().getUuid())) {
                return session;
            }
        }

        return null;
    }

    /**
     * Gets a player by their Xbox user identifier
     *
     * @param xuid the Xbox user identifier
     * @return the player or <code>null</code> if there is no player online with this xuid
     */
    @SuppressWarnings("unused") // API usage
    public RorySession getPlayerByXuid(String xuid) {
        for (RorySession session : players) {
            if (session.getAuthData() != null && session.getAuthData().getXboxUUID().equals(xuid)) {
                return session;
            }
        }

        return null;
    }

    public static RoryConnector start(PlatformType platformType, RoryBootstrap bootstrap) {
        return new RoryConnector(platformType, bootstrap);
    }

    public void reload() {
        shutdown();
        bootstrap.onEnable();
    }

    public RoryLogger getLogger() {
        return bootstrap.getRoryLogger();
    }

    public RoryConfiguration getConfig() {
        return bootstrap.getRoryConfig();
    }

    public CommandManager getCommandManager() {
        return bootstrap.getRoryCommandManager();
    }

    public WorldManager getWorldManager() {
        return bootstrap.getWorldManager();
    }

    /**
     * Whether to use XML reflections in the jar or manually find the reflections.
     * Will return true if the version number is not 'DEV' and the platform is not Fabric.
     * On Fabric - it complains about being unable to create a default XMLReader.
     * On other platforms this should only be true in compiled jars.
     *
     * @return whether to use XML reflections
     */
    public boolean useXmlReflections() {
        //noinspection ConstantConditions
        return !this.getPlatformType().equals(PlatformType.FABRIC) && !"DEV".equals(RoryConnector.VERSION);
    }

    public static RoryConnector getInstance() {
        return instance;
    }
}
