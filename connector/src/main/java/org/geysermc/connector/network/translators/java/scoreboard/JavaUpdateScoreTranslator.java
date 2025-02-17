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

package org.geysermc.connector.network.translators.java.scoreboard;

import com.github.steveice10.mc.protocol.data.game.scoreboard.ScoreboardAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerUpdateScorePacket;
import org.geysermc.connector.RoryConnector;
import org.geysermc.connector.RoryLogger;
import org.geysermc.connector.network.session.RorySession;
import org.geysermc.connector.network.session.cache.WorldCache;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.scoreboard.Objective;
import org.geysermc.connector.scoreboard.Scoreboard;
import org.geysermc.connector.scoreboard.ScoreboardUpdater;
import org.geysermc.connector.utils.LanguageUtils;

@Translator(packet = ServerUpdateScorePacket.class)
public class JavaUpdateScoreTranslator extends PacketTranslator<ServerUpdateScorePacket> {
    private final RoryLogger logger;

    public JavaUpdateScoreTranslator() {
        logger = RoryConnector.getInstance().getLogger();
    }

    @Override
    public void translate(ServerUpdateScorePacket packet, RorySession session) {
        WorldCache worldCache = session.getWorldCache();
        Scoreboard scoreboard = worldCache.getScoreboard();
        int pps = worldCache.increaseAndGetScoreboardPacketsPerSecond();

        Objective objective = scoreboard.getObjective(packet.getObjective());
        if (objective == null && packet.getAction() != ScoreboardAction.REMOVE) {
            logger.info(LanguageUtils.getLocaleStringLog("geyser.network.translator.score.failed_objective", packet.getObjective()));
            return;
        }

        switch (packet.getAction()) {
            case ADD_OR_UPDATE:
                objective.setScore(packet.getEntry(), packet.getValue());
                break;
            case REMOVE:
                if (objective != null) {
                    objective.removeScore(packet.getEntry());
                } else {
                    for (Objective objective1 : scoreboard.getObjectives().values()) {
                        objective1.removeScore(packet.getEntry());
                    }
                }
                break;
        }

        // ScoreboardUpdater will handle it for us if the packets per second
        // (for score and team packets) is higher then the first threshold
        if (pps < ScoreboardUpdater.FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
            scoreboard.onUpdate();
        }
    }
}
