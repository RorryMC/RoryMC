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

package org.geysermc.connector.network.translators.world;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.connector.network.session.RorySession;
import org.geysermc.connector.network.session.cache.ChunkCache;
import org.geysermc.connector.network.translators.inventory.translators.LecternInventoryTranslator;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.GameRule;

public class RoryWorldManager extends WorldManager {

    private static final Object2ObjectMap<String, String> gameruleCache = new Object2ObjectOpenHashMap<>();

    @Override
    public int getBlockAt(RorySession session, int x, int y, int z) {
        ChunkCache chunkCache = session.getChunkCache();
        if (chunkCache != null) { // Chunk cache can be null if the session is closed asynchronously
            return chunkCache.getBlockAt(x, y, z);
        }
        return BlockTranslator.JAVA_AIR_ID;
    }

    @Override
    public void getBlocksInSection(RorySession session, int x, int y, int z, Chunk chunk) {
        ChunkCache chunkCache = session.getChunkCache();
        Column cachedColumn;
        Chunk cachedChunk;
        if (chunkCache == null || (cachedColumn = chunkCache.getChunk(x, z)) == null || (cachedChunk = cachedColumn.getChunks()[y]) == null) {
            return;
        }

        // Copy state IDs from cached chunk to output chunk
        for (int blockY = 0; blockY < 16; blockY++) { // Cache-friendly iteration order
            for (int blockZ = 0; blockZ < 16; blockZ++) {
                for (int blockX = 0; blockX < 16; blockX++) {
                    chunk.set(blockX, blockY, blockZ, cachedChunk.get(blockX, blockY, blockZ));
                }
            }
        }
    }

    @Override
    public boolean hasOwnChunkCache() {
        // This implementation can only fetch data from the session chunk cache
        return false;
    }

    @Override
    public int[] getBiomeDataAt(RorySession session, int x, int z) {
        if (session.getConnector().getConfig().isCacheChunks()) {
            ChunkCache chunkCache = session.getChunkCache();
            if (chunkCache != null) { // Chunk cache can be null if the session is closed asynchronously
                Column column = chunkCache.getChunk(x, z);
                if (column != null) { // Column can be null if the server sent a partial chunk update before the first ground-up-continuous one
                    return column.getBiomeData();
                }
            }
        }
        return new int[1024];
    }

    @Override
    public NbtMap getLecternDataAt(RorySession session, int x, int y, int z, boolean isChunkLoad) {
        // Without direct server access, we can't get lectern information on-the-fly.
        // I should have set this up so it's only called when there is a book in the block state. - Camotoy
        NbtMapBuilder lecternTag = LecternInventoryTranslator.getBaseLecternTag(x, y, z, 1);
        lecternTag.putCompound("book", NbtMap.builder()
                .putByte("Count", (byte) 1)
                .putShort("Damage", (short) 0)
                .putString("Name", "minecraft:written_book")
                .putCompound("tag", NbtMap.builder()
                        .putString("photoname", "")
                        .putString("text", "")
                        .build())
                .build());
        lecternTag.putInt("page", -1); // I'm surprisingly glad this exists - it forces Bedrock to stop reading immediately. Usually.
        return lecternTag.build();
    }

    @Override
    public boolean shouldExpectLecternHandled() {
        return false;
    }

    @Override
    public void setGameRule(RorySession session, String name, Object value) {
        session.sendDownstreamPacket(new ClientChatPacket("/gamerule " + name + " " + value));
        gameruleCache.put(name, String.valueOf(value));
    }

    @Override
    public Boolean getGameRuleBool(RorySession session, GameRule gameRule) {
        String value = gameruleCache.get(gameRule.getJavaID());
        if (value != null) {
            return Boolean.parseBoolean(value);
        }

        return gameRule.getDefaultValue() != null ? (Boolean) gameRule.getDefaultValue() : false;
    }

    @Override
    public int getGameRuleInt(RorySession session, GameRule gameRule) {
        String value = gameruleCache.get(gameRule.getJavaID());
        if (value != null) {
            return Integer.parseInt(value);
        }

        return gameRule.getDefaultValue() != null ? (int) gameRule.getDefaultValue() : 0;
    }

    @Override
    public void setPlayerGameMode(RorySession session, GameMode gameMode) {
        session.sendDownstreamPacket(new ClientChatPacket("/gamemode " + gameMode.name().toLowerCase()));
    }

    @Override
    public void setDifficulty(RorySession session, Difficulty difficulty) {
        session.sendDownstreamPacket(new ClientChatPacket("/difficulty " + difficulty.name().toLowerCase()));
    }

    @Override
    public boolean hasPermission(RorySession session, String permission) {
        return false;
    }
}
