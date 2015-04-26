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

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.player.PlayerDeathEvent;
import org.spongepowered.api.event.entity.player.PlayerRespawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.text.SpongeTexts;
import org.spongepowered.vanilla.interfaces.IMixinEntityPlayerMP;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP extends EntityPlayer implements IMixinEntityPlayerMP {
    @Shadow public MinecraftServer mcServer;

    @Shadow public abstract Locale getLocale();

    private PlayerDeathEvent lastEvent;

    public MixinEntityPlayerMP(World worldIn, GameProfile gameProfileIn) {
        super(worldIn, gameProfileIn);
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    public void onDeathHead(DamageSource cause, CallbackInfo ci) {
        boolean keepInventory = this.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory");
        lastEvent = SpongeEventFactory.createPlayerDeath(Sponge.getGame(),
                null,
                (Player) this,
                ((Player) this).getLocation(),
                SpongeTexts.toText(this.getCombatTracker().getDeathMessage()),
                keepInventory ? 0 : getExperiencePoints(this),
                (int) (keepInventory ? experience : 0),
                keepInventory ? experienceLevel : 0,
                keepInventory,
                keepInventory
        );
        Sponge.getGame().getEventManager().post(lastEvent);

        if (lastEvent.getNewLevel() == 0 && lastEvent.getNewExperience() == 0) {
            if (lastEvent.keepsLevel()) {
                lastEvent.setNewLevel(experienceLevel);
                lastEvent.setNewExperience((int) experience);
            }
        } else {
            lastEvent.setKeepsLevel(true);
        }
    }

    @ModifyArg(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendMessageToAllTeamMembers" +
            "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/IChatComponent;)V"))
    public IChatComponent onDeathMessageFirstSend(EntityPlayer player, IChatComponent component) {
        return SpongeTexts.toComponent(lastEvent.getDeathMessage());
    }

    @ModifyArg(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendMessageToTeamOrEvryPlayer" +
            "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/IChatComponent;)V"))
    public IChatComponent onDeathMessageSecondSend(EntityPlayer player, IChatComponent component) {
        return SpongeTexts.toComponent(lastEvent.getDeathMessage());
    }

    @ModifyArg(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendChatMsg" +
            "(Lnet/minecraft/util/IChatComponent;)V"))
    public IChatComponent onDeathMessageThirdSend(IChatComponent component) {
        return SpongeTexts.toComponent(lastEvent.getDeathMessage());
    }

    @Redirect(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameRules;getGameRuleBooleanValue(Ljava/lang/String;)Z"))
    public boolean keepInventoryCheckRedirect(GameRules gameRules, String gameRule) {
        return lastEvent.keepsInventory();
    }

    @Inject(method = "onDeath", at = @At(value = "RETURN"))
    public void onDeathReturn(DamageSource cause, CallbackInfo ci) {
        experience = 0;
        experienceTotal = 0;
        experienceLevel = 0;
        addExperienceLevel(lastEvent.getNewLevel());
        addExperience(lastEvent.getNewExperience());
    }

    @Override
    public PlayerDeathEvent getLastDeathEvent() {
        return lastEvent;
    }

    @Override
    public void setLastDeathEvent(PlayerDeathEvent event) {
        lastEvent = event;
    }
}
