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

package org.geysermc.connector.network.translators.bedrock;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.BlockPickRequestPacket;
import org.geysermc.connector.entity.ItemFrameEntity;
import org.geysermc.connector.network.session.RorySession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.InventoryUtils;

@Translator(packet = BlockPickRequestPacket.class)
public class BedrockBlockPickRequestTranslator extends PacketTranslator<BlockPickRequestPacket> {

    @Override
    public void translate(BlockPickRequestPacket packet, RorySession session) {
        Vector3i vector = packet.getBlockPosition();
        int blockToPick = session.getConnector().getWorldManager().getBlockAt(session, vector.getX(), vector.getY(), vector.getZ());
        
        // Block is air - chunk caching is probably off
        if (blockToPick == BlockTranslator.JAVA_AIR_ID) {
            // Check for an item frame since the client thinks that's a block when it's an entity in Java
            ItemFrameEntity entity = ItemFrameEntity.getItemFrameEntity(session, packet.getBlockPosition());
            if (entity != null) {
                // Check to see if the item frame has an item in it first
                if (entity.getHeldItem() != null && entity.getHeldItem().getId() != 0) {
                    // Grab the item in the frame
                    InventoryUtils.findOrCreateItem(session, entity.getHeldItem());
                } else {
                    // Grab the frame as the item
                    InventoryUtils.findOrCreateItem(session, "minecraft:item_frame");
                }
            }
            return;
        }

        InventoryUtils.findOrCreateItem(session, BlockTranslator.getBlockMapping(blockToPick).getPickItem());
    }
}
