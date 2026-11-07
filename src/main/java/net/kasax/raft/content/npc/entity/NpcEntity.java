package net.kasax.raft.content.npc.entity;

import net.kasax.raft.content.npc.NpcCharacter;
import net.kasax.raft.content.npc.NpcContent;
import net.kasax.raft.content.npc.NpcDialogue;
import net.kasax.raft.content.npc.NpcTrades;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A person of the Endless Sea (docs/design/npcs.md). Lives at a home spot inside its structure (set where it first
 * appears: structure template, spawn egg or command), never despawns and cannot be hurt by the world or by players in
 * survival (a creative player or /kill can remove it). Right-click opens a conversation that follows the player's
 * story progress ({@link NpcDialogue}); traders open the vanilla trade screen from there.
 */
public class NpcEntity extends PathfinderMob implements Merchant, Npc {
    private static final EntityDataAccessor<Boolean> DATA_SITTING = SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.BOOLEAN);
    /** Trades are restocked when this many game ticks passed since the last restock (10 minutes). */
    private static final long RESTOCK_TICKS = 12000L;
    /** How long the NPC keeps facing a player after a conversation opened (the screen sends "bye" when it closes). */
    private static final int TALK_TICKS = 1200;

    private @Nullable NpcCharacter character;
    private @Nullable Player tradingPlayer;
    private @Nullable MerchantOffers offers;
    private int[] savedUses = new int[0];
    private long lastRestock;
    private boolean setUp;
    private @Nullable Player talkPartner;
    private int talkTicks;
    private int lostTicks;
    private int hurtLineCooldown;
    private final List<BlockPos> places = new ArrayList<>();
    private long placesScannedAt = -1_000_000L;
    private @Nullable Set<Block> placeBlocks;

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 4.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.LAVA, -1.0F);
        this.setPathfindingMalus(PathType.DANGER_OTHER, 16.0F);
        ((net.minecraft.world.entity.ai.navigation.GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        this.setPersistenceRequired();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return NpcPaths.navigation(this, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public NpcCharacter character() {
        if (this.character == null) {
            NpcCharacter c = NpcContent.character(this.getType());
            this.character = c == null ? NpcCharacter.KEEPER_FENN : c;
        }
        return this.character;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new NpcGoals.Talk(this));
        this.goalSelector.addGoal(2, new NpcGoals.ReturnHome(this));
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(4, new NpcActivityGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.45, 0.0F) {
            @Override
            public boolean canUse() {
                return !NpcEntity.this.isSitting() && NpcEntity.this.random.nextInt(3) == 0 && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SITTING, false);
    }

    // ------------------------------------------------------------------------------------------ set-up
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  MobSpawnType reason, @Nullable SpawnGroupData groupData) {
        this.setUp();
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    /** Home where the NPC first stands, and its tool in hand. Runs once (template entities, eggs, old saves). */
    private void setUp() {
        if (!this.hasRestriction()) {
            this.restrictTo(this.blockPosition(), this.character().homeRadius);
        }
        if (!this.setUp) {
            this.setUp = true;
            this.setItemSlot(EquipmentSlot.MAINHAND, this.heldItem());
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                this.setDropChance(slot, 0.0F);
            }
        }
    }

    public ItemStack heldItem() {
        ResourceLocation id = ResourceLocation.tryParse(this.character().heldItem);
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ------------------------------------------------------------------------------------------ ticking
    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.setUp();
        if (this.hurtLineCooldown > 0) {
            this.hurtLineCooldown--;
        }
        if (this.talkTicks > 0) {
            this.talkTicks--;
            Player p = this.talkPartner;
            if (p == null || !p.isAlive() || p.isRemoved() || p.level() != this.level() || p.distanceToSqr(this) > 100.0) {
                this.talkTicks = 0;
            }
            if (this.talkTicks == 0) {
                this.talkPartner = null;
            }
        }
        if (this.tradingPlayer != null && (!this.tradingPlayer.isAlive() || this.tradingPlayer.level() != this.level())) {
            this.tradingPlayer = null;
        }
        // lost: in the water, far from home (fell off a pier, pushed by a piston...), up on a roof (every home is one
        // floor) or on top of a railing or fence -> back home after 5 s
        if (this.tickCount % 20 == 0 && this.hasRestriction()) {
            double r = this.getRestrictRadius() + 12.0;
            boolean lost = this.isInWater() || this.isInLava()
                    || this.getY() > this.getRestrictCenter().getY() + 2.5
                    || this.getRestrictCenter().distToCenterSqr(this.position()) > r * r
                    || NpcPaths.onFenceTop(this);
            this.lostTicks = lost ? this.lostTicks + 20 : 0;
            if (this.lostTicks >= 100 && !this.isTalking()) {
                this.lostTicks = 0;
                BlockPos home = this.getRestrictCenter();
                this.setSitting(false);
                this.getNavigation().stop();
                this.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
                level.playSound(null, home, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.NEUTRAL, 0.3F, 1.4F);
            }
        }
    }

    // ------------------------------------------------------------------------------------------ talking
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isAlive() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer sp) {
            if (sp.isCreative() && sp.isSecondaryUseActive()) {
                // builders: sneak-use in creative moves the NPC's home to where it stands now
                this.restrictTo(this.blockPosition(), this.character().homeRadius);
                this.places.clear();
                this.placesScannedAt = -1_000_000L;
                sp.displayClientMessage(Component.translatable("npc.raft.home_set", this.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
                return InteractionResult.SUCCESS;
            }
            if (this.tradingPlayer != null && this.tradingPlayer != sp) {
                sp.displayClientMessage(Component.translatable("npc.raft.busy", this.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.SUCCESS;
            }
            NpcDialogue.open(sp, this);
        }
        return InteractionResult.SUCCESS;
    }

    public void startTalking(Player player) {
        this.talkPartner = player;
        this.talkTicks = TALK_TICKS;
        this.getNavigation().stop();
    }

    public void stopTalking(Player player) {
        if (this.talkPartner == player && this.talkTicks > 20) {
            this.talkTicks = 20;
        }
    }

    public boolean isTalking() {
        return this.talkTicks > 0 || this.tradingPlayer != null;
    }

    public @Nullable Player talkPartner() {
        return this.tradingPlayer != null ? this.tradingPlayer : this.talkPartner;
    }

    public boolean isSitting() {
        return this.entityData.get(DATA_SITTING);
    }

    public void setSitting(boolean sitting) {
        this.entityData.set(DATA_SITTING, sitting);
    }

    // ------------------------------------------------------------------------------------------ places of interest
    /** The blocks of interest around home (scanned every two minutes); empty when the character has none. */
    public List<BlockPos> places() {
        if (!(this.level() instanceof ServerLevel level) || !this.hasRestriction() || this.character().places.isEmpty()) {
            return List.of();
        }
        long now = level.getGameTime();
        if (now - this.placesScannedAt < 2400L) {
            return this.places;
        }
        this.placesScannedAt = now;
        if (this.placeBlocks == null) {
            Set<Block> blocks = new HashSet<>();
            for (String id : this.character().places) {
                ResourceLocation ident = ResourceLocation.tryParse(id);
                Block b = ident == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(ident);
                if (b != Blocks.AIR) {
                    blocks.add(b);
                }
            }
            this.placeBlocks = blocks;
        }
        this.places.clear();
        BlockPos home = this.getRestrictCenter();
        int r = (int) this.getRestrictRadius();
        for (BlockPos p : BlockPos.betweenClosed(home.offset(-r, -3, -r), home.offset(r, 3, r))) {
            int dx = p.getX() - home.getX(), dz = p.getZ() - home.getZ();
            if (dx * dx + dz * dz <= r * r && this.placeBlocks.contains(level.getBlockState(p).getBlock())) {
                this.places.add(p.immutable());
            }
        }
        Collections.shuffle(this.places, new java.util.Random(this.random.nextLong()));
        if (this.places.size() > 16) {
            this.places.subList(16, this.places.size()).clear();
        }
        return this.places;
    }

    // ------------------------------------------------------------------------------------------ invulnerable
    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (!(this.level() instanceof ServerLevel level)) return false;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.isCreativePlayer()) {
            return super.hurt(source, damage);
        }
        if (source.getEntity() instanceof ServerPlayer player && !player.isSpectator() && this.hurtLineCooldown == 0) {
            this.hurtLineCooldown = 40;
            this.getLookControl().setLookAt(player, 30.0F, 30.0F);
            player.sendSystemMessage(Component.translatable("npc.raft.says", this.getDisplayName(),
                    Component.translatable(this.character().key("hurt"))));
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE,
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isCreativePlayer();
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightningBolt) {
    }

    /** The name plate shows (client side) when a player looks at the NPC from close by, see NpcRenderer. */
    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    /** People stay in their world: no portals. */
    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    // ------------------------------------------------------------------------------------------ trading
    /** Opens the trade screen for the player (after the conversation's "trade" option). */
    public void openTrades(ServerPlayer player) {
        if (!NpcTrades.hasTrades(this.character())) {
            return;
        }
        if (this.tradingPlayer != null && this.tradingPlayer != player) {
            player.displayClientMessage(Component.translatable("npc.raft.busy", this.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (this.level() instanceof ServerLevel level && level.getGameTime() - this.lastRestock >= RESTOCK_TICKS) {
            this.lastRestock = level.getGameTime();
            for (MerchantOffer offer : this.getOffers()) {
                offer.resetUses();
            }
        }
        this.setTradingPlayer(player);
        this.startTalking(player);
        this.openTradingScreen(player, this.getDisplayName(), 0);
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Override
    public @Nullable Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            if (!(this.level() instanceof ServerLevel level)) {
                return new MerchantOffers();
            }
            this.offers = NpcTrades.build(this.character(), level);
            if (this.savedUses.length == this.offers.size()) {
                for (int i = 0; i < this.savedUses.length; i++) {
                    for (int k = 0; k < this.savedUses[i]; k++) {
                        this.offers.get(i).increaseUses();
                    }
                }
            }
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        if (this.level() instanceof ServerLevel level) {
            if (offer.shouldRewardExp()) {
                ExperienceOrb.award(level, this.position().add(0.0, 1.2, 0.0), 1 + this.random.nextInt(3));
            }
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.6F, 1.3F);
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack itemStack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.ITEM_PICKUP;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide();
    }

    public boolean stillValid(Player player) {
        return this.getTradingPlayer() == player && this.isAlive() && player.canInteractWithEntity(this, 4.0);
    }

    // ------------------------------------------------------------------------------------------ save / load
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag121) {
        ValueOutput output = ValueOutput.of(tag121, this.registryAccess());
        super.addAdditionalSaveData(output.tag());
        output.putBoolean("NpcSetUp", this.setUp);
        output.putBoolean("Sitting", this.isSitting());
        output.putLong("LastRestock", this.lastRestock);
        if (this.offers != null) {
            int[] uses = new int[this.offers.size()];
            for (int i = 0; i < uses.length; i++) {
                uses[i] = this.offers.get(i).getUses();
            }
            output.putIntArray("TradeUses", uses);
        } else if (this.savedUses.length > 0) {
            output.putIntArray("TradeUses", this.savedUses);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag121) {
        ValueInput input = ValueInput.of(tag121, this.registryAccess());
        super.readAdditionalSaveData(input.tag());
        this.setUp = input.getBooleanOr("NpcSetUp", false);
        this.setSitting(input.getBooleanOr("Sitting", false));
        this.lastRestock = input.getLongOr("LastRestock", 0L);
        this.savedUses = input.getIntArray("TradeUses").orElse(new int[0]);
        this.offers = null;
    }

    /** The world position in front of the eyes, {@code distance} blocks along the current look direction. */
    Vec3 eyeAhead(double distance) {
        return this.getEyePosition().add(this.getLookAngle().scale(distance));
    }
}
