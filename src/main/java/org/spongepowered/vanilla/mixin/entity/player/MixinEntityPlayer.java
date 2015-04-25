/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
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
 */
package org.spongepowered.vanilla.mixin.entity.player;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.vanilla.interfaces.IMixinEntityPlayerMP;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase {
    @Shadow private int experienceLevel;
    @Shadow private float experience;
    @Shadow private int experienceTotal;
    @Shadow private InventoryPlayer inventory;

    public MixinEntityPlayer(World worldIn) {
        super(worldIn);
    }

    @Overwrite
    protected int getExperiencePoints(EntityPlayer player)
    {
        if ((((IMixinEntityPlayerMP) this).getLastDeathEvent() != null && ((IMixinEntityPlayerMP) this).getLastDeathEvent().keepsLevel()) || this.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory"))
        {
            return 0;
        }
        else
        {
            int i = this.experienceLevel * 7;
            return i > 100 ? 100 : i;
        }
    }

    // Move lastEvent field from the old EntityPlayer to the new (this) one
    @Inject(method = "clonePlayer", at = @At(value = "HEAD"))
    public void clonePlayerHeadHandler(EntityPlayer old, boolean whatever, CallbackInfo ci) {
        ((IMixinEntityPlayerMP) this).setLastDeathEvent(((IMixinEntityPlayerMP) old).getLastDeathEvent());
    }

    // Completely cancel the copying over of params if keepInventory (game rule) is enabled, we'll do that manually below
    @Redirect(method = "clonePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameRules;getGameRuleBooleanValue(Ljava/lang/String;)Z"))
    public boolean keepInventoryCheckRedirect(GameRules gameRules, String gameRule) {
        return false;
    }

    // The check above encompasses both keepsInventory and keepsLevel -- if keepsInventory is on but keepsLevel is off (or opposite), copy it manually
    @Inject(method = "clonePlayer", at = @At(value = "RETURN"))
    public void clonePlayerReturnHandler(EntityPlayer old, boolean respawnFromEnd, CallbackInfo ci) {
        System.out.println(((IMixinEntityPlayerMP) this).getLastDeathEvent());
        if (!respawnFromEnd && ((IMixinEntityPlayerMP) this).getLastDeathEvent() != null) {
            // TODO: handle score copying here (api methods for setting that perhaps?) http://i.imgur.com/mV5rTQd.png
            //this.setScore(old.getScore());
            if (((IMixinEntityPlayerMP) this).getLastDeathEvent().keepsInventory()) {
                this.inventory.copyInventory(old.inventory);
            }

            if (((IMixinEntityPlayerMP) this).getLastDeathEvent().keepsLevel()) {
                this.experienceLevel = old.experienceLevel;
                this.experienceTotal = old.experienceTotal;
                this.experience = old.experience;
            }


        }
    }
}
