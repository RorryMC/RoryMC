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

package org.geysermc.connector.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.geysermc.connector.network.session.RorySession;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public abstract class RoryCommand {

    protected final String name;
    /**
     * The description of the command - will attempt to be translated.
     */
    protected final String description;
    protected final String permission;

    @Setter
    private List<String> aliases = new ArrayList<>();

    public abstract void execute(@Nullable RorySession session, CommandSender sender, String[] args);

    /**
     * If false, hides the command from being shown on the Rory Standalone GUI.
     *
     * @return true if the command can be run on the server console
     */
    public boolean isExecutableOnConsole() {
        return true;
    }

    /**
     * Used in the GUI to know what subcommands can be run
     *
     * @return a list of all possible subcommands, or empty if none.
     */
    public List<String> getSubCommands() {
        return Collections.emptyList();
    }

    /**
     * Shortcut to {@link #getSubCommands()}{@code .isEmpty()}.
     *
     * @return true if there are subcommand present for this command.
     */
    public boolean hasSubCommands() {
        return !getSubCommands().isEmpty();
    }

    /**
     * Used to send a deny message to Java players if this command can only be used by Bedrock players.
     *
     * @return true if this command can only be used by Bedrock players.
     */
    public boolean isBedrockOnly() {
        return false;
    }
}