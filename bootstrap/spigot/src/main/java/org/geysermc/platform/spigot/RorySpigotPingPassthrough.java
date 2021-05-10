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

package org.geysermc.platform.spigot;

import com.github.steveice10.mc.protocol.MinecraftConstants;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;
import org.geysermc.connector.common.ping.RoryPingInfo;
import org.geysermc.connector.ping.IRoryPingPassthrough;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;

@AllArgsConstructor
public class RorySpigotPingPassthrough implements IRoryPingPassthrough {

    private final RorySpigotLogger logger;

    @Override
    public RoryPingInfo getPingInformation(InetSocketAddress inetSocketAddress) {
        try {
            ServerListPingEvent event = new RoryPingEvent(inetSocketAddress.getAddress(), Bukkit.getMotd(), Bukkit.getOnlinePlayers().size(), Bukkit.getMaxPlayers());
            Bukkit.getPluginManager().callEvent(event);
            RoryPingInfo geyserPingInfo = new RoryPingInfo(event.getMotd(),
                    new RoryPingInfo.Players(event.getMaxPlayers(), event.getNumPlayers()),
                    new RoryPingInfo.Version(Bukkit.getVersion(), MinecraftConstants.PROTOCOL_VERSION) // thanks Spigot for not exposing this, just default to latest
            );
            Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(geyserPingInfo.getPlayerList()::add);
            return geyserPingInfo;
        } catch (Exception e) {
            logger.debug("Error while getting Bukkit ping passthrough: " + e.toString());
            return new RoryPingInfo(null, null, null);
        }
    }

    // These methods are unimplemented on spigot api by default so we add stubs so plugins don't complain
    private static class RoryPingEvent extends ServerListPingEvent {

        public RoryPingEvent(InetAddress address, String motd, int numPlayers, int maxPlayers) {
            super(address, motd, numPlayers, maxPlayers);
        }

        @Override
        public void setServerIcon(CachedServerIcon icon) throws IllegalArgumentException, UnsupportedOperationException {
        }

        @Override
        public Iterator<Player> iterator() throws UnsupportedOperationException {
            return Collections.EMPTY_LIST.iterator();
        }
    }

}
