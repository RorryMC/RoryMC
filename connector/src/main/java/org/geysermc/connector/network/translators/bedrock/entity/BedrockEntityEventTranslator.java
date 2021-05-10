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

package org.geysermc.connector.network.translators.bedrock.entity;

import com.github.steveice10.mc.protocol.data.game.window.VillagerTrade;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientSelectTradePacket;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.inventory.RoryItemStack;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.inventory.MerchantContainer;
import org.geysermc.connector.network.session.RorySession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = EntityEventPacket.class)
public class BedrockEntityEventTranslator extends PacketTranslator<EntityEventPacket> {

    @Override
    public void translate(EntityEventPacket packet, RorySession session) {
        switch (packet.getType()) {
            case EATING_ITEM:
                // Resend the packet so we get the eating sounds
                session.sendUpstreamPacket(packet);
                return;
            case COMPLETE_TRADE:
                session.addInventoryTask(() -> {
                    ClientSelectTradePacket selectTradePacket = new ClientSelectTradePacket(packet.getData());
                    session.sendDownstreamPacket(selectTradePacket);
                });

                session.addInventoryTask(() -> {
                    Entity villager = session.getPlayerEntity();
                    Inventory openInventory = session.getOpenInventory();
                    if (openInventory instanceof MerchantContainer) {
                        MerchantContainer merchantInventory = (MerchantContainer) openInventory;
                        VillagerTrade[] trades = merchantInventory.getVillagerTrades();
                        if (trades != null && packet.getData() >= 0 && packet.getData() < trades.length) {
                            VillagerTrade trade = merchantInventory.getVillagerTrades()[packet.getData()];
                            openInventory.setItem(2, RoryItemStack.from(trade.getOutput()), session);
                            villager.getMetadata().put(EntityData.TRADE_XP, trade.getXp() + villager.getMetadata().getInt(EntityData.TRADE_XP));
                            villager.updateBedrockMetadata(session);
                        }
                    }
                }, 100);
                return;
        }
        session.getConnector().getLogger().debug("Did not translate incoming EntityEventPacket: " + packet.toString());
    }
}
