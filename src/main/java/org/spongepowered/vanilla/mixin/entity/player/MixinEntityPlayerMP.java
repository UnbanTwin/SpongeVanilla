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
import net.minecraft.world.World;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.player.PlayerDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.text.SpongeChatComponent;
import org.spongepowered.common.text.SpongeText;
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

    @Overwrite
    public void onDeath(DamageSource cause) {
        boolean keepInventory = this.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory");
        lastEvent = SpongeEventFactory.createPlayerDeath(Sponge.getGame(),
                null,
                (Player) this,
                ((Player) this).getLocation(),
                ((SpongeChatComponent) this.getCombatTracker().getDeathMessage()).toText(),
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

        if(this.worldObj.getGameRules().getGameRuleBooleanValue("showDeathMessages")) {
            Team collection = this.getTeam();
            if(collection != null && collection.func_178771_j() != Team.EnumVisible.ALWAYS) {
                if(collection.func_178771_j() == Team.EnumVisible.HIDE_FOR_OTHER_TEAMS) {
                    this.mcServer.getConfigurationManager().sendMessageToAllTeamMembers((EntityPlayerMP) (Object) this, ((SpongeText) lastEvent.getDeathMessage()).toComponent(getLocale()));
                } else if(collection.func_178771_j() == Team.EnumVisible.HIDE_FOR_OWN_TEAM) {
                    this.mcServer.getConfigurationManager().sendMessageToTeamOrEvryPlayer((EntityPlayerMP) (Object) this, ((SpongeText) lastEvent.getDeathMessage()).toComponent(getLocale()));
                }
            } else {
                this.mcServer.getConfigurationManager().sendChatMsg(((SpongeText) lastEvent.getDeathMessage()).toComponent(getLocale()));
            }
        }

        if(!lastEvent.keepsInventory()) {
            this.inventory.dropAllItems();
        }

        Collection collection1 = this.worldObj.getScoreboard().getObjectivesFromCriteria(IScoreObjectiveCriteria.deathCount);
        Iterator iterator = collection1.iterator();

        while(iterator.hasNext()) {
            ScoreObjective entitylivingbase = (ScoreObjective)iterator.next();
            Score entityegginfo = this.getWorldScoreboard().getValueFromObjective(this.getCommandSenderName(), entitylivingbase);
            entityegginfo.func_96648_a();
        }

        EntityLivingBase entitylivingbase1 = this.func_94060_bK();
        if(entitylivingbase1 != null) {
            EntityList.EntityEggInfo entityegginfo1 = (EntityList.EntityEggInfo)EntityList.entityEggs.get(Integer.valueOf(EntityList.getEntityID(entitylivingbase1)));
            if(entityegginfo1 != null) {
                this.triggerAchievement(entityegginfo1.field_151513_e);
            }

            entitylivingbase1.addToPlayerScore(this, this.scoreValue);
        }

        this.triggerAchievement(StatList.deathsStat);
        this.func_175145_a(StatList.timeSinceDeathStat);
        this.getCombatTracker().func_94549_h();

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
