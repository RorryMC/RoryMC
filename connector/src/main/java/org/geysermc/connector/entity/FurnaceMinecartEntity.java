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

package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.RorySession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;

public class FurnaceMinecartEntity extends DefaultBlockMinecartEntity {

    private boolean hasFuel = false;

    public FurnaceMinecartEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, RorySession session) {
        if (entityMetadata.getId() == 13 && !showCustomBlock) {
            hasFuel = (boolean) entityMetadata.getValue();
            updateDefaultBlockMetadata(session);
        }

        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public void updateDefaultBlockMetadata(RorySession session) {
        metadata.put(EntityData.DISPLAY_ITEM, session.getBlockTranslator().getBedrockBlockId(hasFuel ? BlockTranslator.JAVA_RUNTIME_FURNACE_LIT_ID : BlockTranslator.JAVA_RUNTIME_FURNACE_ID));
        metadata.put(EntityData.DISPLAY_OFFSET, 6);
    }
}
