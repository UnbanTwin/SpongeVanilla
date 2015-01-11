/*
 * License (MIT)
 *
 * Copyright (c) 2014-2015 Granite Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.granitepowered.granite.util;

import org.granitepowered.granite.mc.MCBlockPos;
import org.granitepowered.granite.mc.MCChatComponent;
import org.granitepowered.granite.mc.MCGameRules;
import org.granitepowered.granite.mc.MCPacketBlockChange;
import org.granitepowered.granite.mc.MCPacketChatMessage;
import org.granitepowered.granite.mc.MCPacketTitle;
import org.granitepowered.granite.mc.MCPacketTitle$Type;
import org.granitepowered.granite.mc.MCPotionEffect;
import org.granitepowered.granite.mc.MCWorld;

public class Instantiator {

    private static final InstantiatorInterface instance = null;

    public static InstantiatorInterface get() {
        return instance;
    }

    public interface InstantiatorInterface {

        MCPacketChatMessage newPacketChatMessage(MCChatComponent chatComponent, byte type);

        MCBlockPos newBlockPos(int x, int y, int z);

        MCGameRules newGameRules();

        MCPacketTitle newPacketTitle(MCPacketTitle$Type type, MCChatComponent component);

        MCPacketTitle newPacketTitle(int fadeIn, int stay, int fadeOut);

        MCPotionEffect newPotionEffect(int id, int effectDuration, int effectAmplifier, boolean ambient, boolean showParticles);

        MCPacketBlockChange newPacketBlockChange(MCWorld world, MCBlockPos pos);
    }
}
