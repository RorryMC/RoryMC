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

package org.geysermc.connector.bootstrap;

import org.geysermc.connector.dump.BootstrapDumpInfo;
import org.geysermc.connector.ping.IRoryPingPassthrough;
import org.geysermc.connector.configuration.RoryConfiguration;
import org.geysermc.connector.RoryLogger;
import org.geysermc.connector.command.CommandManager;
import org.geysermc.connector.network.translators.world.RoryWorldManager;
import org.geysermc.connector.network.translators.world.WorldManager;

import javax.annotation.Nullable;
import java.nio.file.Path;

public interface RoryBootstrap {

    RoryWorldManager DEFAULT_CHUNK_MANAGER = new RoryWorldManager();

    /**
     * Called when the RoryBootstrap is enabled
     */
    void onEnable();

    /**
     * Called when the RoryBootstrap is disabled
     */
    void onDisable();

    /**
     * Returns the current RoryConfiguration
     *
     * @return The current RoryConfiguration
     */
    RoryConfiguration getRoryConfig();

    /**
     * Returns the current RoryLogger
     *
     * @return The current RoryLogger
     */
    RoryLogger getRoryLogger();

    /**
     * Returns the current CommandManager
     *
     * @return The current CommandManager
     */
    CommandManager getRoryCommandManager();

    /**
     * Returns the current PingPassthrough manager
     *
     * @return The current PingPassthrough manager
     */
    IRoryPingPassthrough getRoryPingPassthrough();

    /**
     * Returns the current WorldManager
     *
     * @return the current WorldManager
     */
    default WorldManager getWorldManager() {
        return DEFAULT_CHUNK_MANAGER;
    }

    /**
     * Return the data folder where files get stored
     *
     * @return Path location of data folder
     */
    Path getConfigFolder();

    /**
     * Information used for the bootstrap section of the debug dump
     *
     * @return The info about the bootstrap
     */
    BootstrapDumpInfo getDumpInfo();

    /**
     * Returns the Minecraft version currently being used on the server. This should be only be implemented on platforms
     * that have direct server access - platforms such as proxies always have to be on their latest version to support
     * the newest Minecraft version, but older servers can use ViaVersion to enable newer versions to join.
     * <br>
     * If used, this should not be null before {@link org.geysermc.connector.RoryConnector} initialization.
     *
     * @return the Minecraft version being used on the server, or <code>null</code> if not applicable
     */
    @Nullable
    default String getMinecraftServerVersion() {
        return null;
    }
}
