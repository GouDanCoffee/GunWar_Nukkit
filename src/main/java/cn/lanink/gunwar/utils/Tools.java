package cn.lanink.gunwar.utils;

import cn.lanink.gamecore.utils.Language;
import cn.lanink.gunwar.GunWar;
import cn.lanink.gunwar.entity.*;
import cn.lanink.gunwar.item.ItemManage;
import cn.lanink.gunwar.item.base.BaseItem;
import cn.lanink.gunwar.room.base.BaseRoom;
import cn.lanink.gunwar.room.base.Team;
import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityFirework;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemColorArmor;
import cn.nukkit.item.ItemFirework;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.PlaySoundPacket;
import cn.nukkit.utils.BlockColor;
import cn.nukkit.utils.DummyBossBar;
import cn.nukkit.utils.DyeColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Tools {

    public static void createBossBar(Player player, ConcurrentHashMap<Player, DummyBossBar> bossBarMap) {
        if (!bossBarMap.containsKey(player)) {
            DummyBossBar bossBar = new DummyBossBar.Builder(player).build();
            bossBar.setColor(255, 0, 0);
            player.createBossBar(bossBar);
            bossBarMap.put(player, bossBar);
        }else {
            player.createBossBar(bossBarMap.get(player));
        }
    }

    /**
     * @param center 圆心
     * @param diameter 半径
     * @return 圆边上的点
     */
    public static LinkedList<Vector3> getRoundEdgePoint(Vector3 center, double diameter) {
        LinkedList<Vector3> list = new LinkedList<>();
        Vector3 point = center.clone();
        point.x += diameter;
        double xDistance = point.x - center.x;
        double zDistance = point.z - center.z;
        for (int i = 0; i < 360; i += 1) {
            list.add(new Vector3(
                    xDistance * Math.cos(i) - zDistance * Math.sin(i) + center.x,
                    center.y,
                    xDistance * Math.sin(i) + zDistance * Math.cos(i) + center.z));
        }
        return list;
    }

    /**
     * 显示玩家
     *
     * @param room 房间
     * @param player 玩家
     */
    public static void showPlayer(BaseRoom room, Player player) {
        for (Player p : room.getPlayers().keySet()) {
            p.showPlayer(player);
        }
    }

    /**
     * 隐藏玩家
     *
     * @param room 房间
     * @param player 玩家
     */
    public static void hidePlayer(BaseRoom room, Player player) {
        for (Player p : room.getPlayers().keySet()) {
            p.hidePlayer(player);
        }
    }

    public static String getShowStringMagazine(int now, int max) {
        return "§e" + now + "/" + max + "  " + getShowStringProgress(now, max);
    }

    public static String getShowStringProgress(int now, int max) {
        StringBuilder string = new StringBuilder();
        for (int j = 0; j < max; j++) {
            if (j < now) {
                string.append("§a▍");
            }else {
                string.append("§c▍");
            }
        }
        return string.toString();
    }

    public static void sendMessage(BaseRoom room, String message) {
        for (Player player : room.getPlayers().keySet()) {
            player.sendMessage(message);
        }
    }

    public static void sendTitle(BaseRoom room, String title) {
        Tools.sendTitle(room, title, "");
    }

    public static void sendTitle(BaseRoom room, String title, String subtitle) {
        for (Player player : room.getPlayers().keySet()) {
            player.sendTitle(title, subtitle);
        }
    }

    public static void sendTitle(BaseRoom room, Team team, String title) {
        sendTitle(room, team, title, "");
    }

    public static void sendTitle(BaseRoom room, Team team, String title, String subtitle) {
        for (Map.Entry<Player, Team> entry : room.getPlayers().entrySet()) {
            if (team == Team.RED || team == Team.RED_DEATH) {
                if (entry.getValue() == Team.RED || entry.getValue() == Team.RED_DEATH) {
                    entry.getKey().sendTitle(title, subtitle);
                }
            }else {
                if (entry.getValue() == Team.BLUE || entry.getValue() == Team.BLUE_DEATH) {
                    entry.getKey().sendTitle(title, subtitle);
                }
            }
        }
    }

    public static void sendRoundVictoryTitle(BaseRoom room, int v) {
        for (Player player : room.getPlayers().keySet()) {
            String title;
            switch (v) {
                case 1:
                    title = GunWar.getInstance().getLanguage().translateString("roundVictoryRed");
                    break;
                case 2:
                    title = GunWar.getInstance().getLanguage().translateString("roundVictoryBlue");
                    break;
                default:
                    title = GunWar.getInstance().getLanguage().translateString("roundVictoryDraw");
                    break;
            }
            player.sendTitle(title, "", 10, 20, 10);
        }
    }

    public static double randomDouble(double min, double max) {
        return min + ((max - min) * GunWar.RANDOM.nextDouble());
    }

    /**
     * 执行命令
     * @param player 玩家
     * @param cmds 命令
     */
    public static void cmd(Player player, List<String> cmds) {
        if (player == null || cmds == null || cmds.size() < 1) {
            return;
        }
        for (String s : cmds) {
            String[] cmd = s.split("&");
            if (cmd.length > 1 && "con".equals(cmd[1])) {
                Server.getInstance().dispatchCommand(new ConsoleCommandSender(), cmd[0].replace("@p", player.getName()));
            } else {
                Server.getInstance().dispatchCommand(player, cmd[0].replace("@p", player.getName()));
            }
        }
    }

    /**
     * 清理实体
     * @param level 世界
     */
    public static void cleanEntity(Level level) {
        cleanEntity(level, false);
    }

    public static void cleanEntity(Level level, boolean all) {
        for (Entity entity : level.getEntities()) {
            if (!(entity instanceof Player)) {
                if (entity instanceof EntityPlayerCorpse ||
                        entity instanceof EntityFlag ||
                        entity instanceof EntityFlagStand ||
                        entity instanceof EntityGunWarBomb ||
                        entity instanceof EntityGunWarBombBlock) {
                    if (!all) {
                        continue;
                    }
                }
                entity.close();
            }
        }
    }

    /**
     * 给装备
     *
     * @param room 房间
     * @param player 玩家
     * @param team 所属队伍
     */
    public static void giveItem(@NotNull BaseRoom room, @NotNull Player player, @NotNull Team team) {
        //如果玩家未加入队伍则不给物品
        if (team == Team.NULL) {
            return;
        }
        player.getInventory().setArmorContents(getArmors(team));
        ArrayList<String> items = new ArrayList<>(room.getInitialItems());
        if (team == Team.RED || team == Team.RED_DEATH) {
            items.addAll(room.getRedTeamInitialItems());
        }else {
            items.addAll(room.getBlueTeamInitialItems());
        }
        for (String string : items) {
            try {
                String[] s1 = string.split("&");
                String[] s2 = s1[1].split("@");
                int count = Integer.parseInt(s2[0]);
                Item item = null;
                if ("item".equalsIgnoreCase(s2[1])) {
                    String[] s3 = s1[0].split(":");
                    if (s3.length > 1) {
                        item = Item.get(Integer.parseInt(s3[0]), Integer.parseInt(s3[1]));
                    }else {
                        item = Item.get(Integer.parseInt(s3[0]), 0);
                    }
                }else {
                    BaseItem baseItem = null;
                    switch (ItemManage.getItemType(s2[1])) {
                        case WEAPON_MELEE:
                            baseItem = ItemManage.getMeleeWeaponMap().get(s1[0]);
                            break;
                        case WEAPON_PROJECTILE:
                            baseItem = ItemManage.getProjectileWeaponMap().get(s1[0]);
                            break;
                        case WEAPON_GUN:
                            baseItem = ItemManage.getGunWeaponMap().get(s1[0]);
                            break;
                        default:
                            break;
                    }
                    if (baseItem != null) {
                        item = baseItem.getItem();
                    }
                }
                if (item != null) {
                    item.setCount(count);
                    player.getInventory().addItem(item);
                    if (GunWar.debug) {
                        GunWar.getInstance().getLogger().info("[debug] 给玩家：" + player.getName() +
                                "物品：" + item.getCustomName() + "数量：" + count);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据队伍获取盔甲
     *
     * @param team 队伍
     * @return 盔甲
     */
    public static Item[] getArmors(Team team) {
        //如果玩家未加入队伍则不给物品
        if (team == Team.NULL) {
            return new Item[4];
        }
        ItemColorArmor helmet = (ItemColorArmor) Item.get(298, 0, 1);
        ItemColorArmor chestPlate = (ItemColorArmor) Item.get(299, 0, 1);
        ItemColorArmor leggings = (ItemColorArmor) Item.get(300, 0, 1);
        ItemColorArmor boots = (ItemColorArmor) Item.get(301, 0, 1);
        BlockColor color;
        if (team == Team.RED || team == Team.RED_DEATH) {
            color = new BlockColor(255, 0, 0);
        }else {
            color = new BlockColor(0, 0, 255);
        }
        Item[] armor = new Item[4];
        armor[0] = helmet.setColor(color);
        armor[1] = chestPlate.setColor(color);
        armor[2] = leggings.setColor(color);
        armor[3] = boots.setColor(color);
        return armor;
    }

    /**
     * 获取物品
     * @return 物品
     */
    public static Item getItem(int type) {
        Language language = GunWar.getInstance().getLanguage();
        Item item = Item.get(0);
        switch (type) {
            case 10: //退出房间物品
                item = Item.get(324, 0, 1);
                item.setNamedTag(new CompoundTag()
                        .putBoolean("isGunWarItem", true)
                        .putInt("GunWarItemType", 10));
                item.setCustomName(language.translateString("itemQuitRoom"));
                item.setLore(language.translateString("itemQuitRoomLore").split("\n"));
                return item;
            case 11: //选择红队物品
                item = Item.get(241, 14, 1);
                item.setNamedTag(new CompoundTag()
                        .putBoolean("isGunWarItem", true)
                        .putInt("GunWarItemType", 11));
                item.setCustomName(language.translateString("itemTeamSelectRed"));
                return item;
            case 12: //选择蓝队物品
                item = Item.get(241, 11, 1);
                item.setNamedTag(new CompoundTag()
                        .putBoolean("isGunWarItem", true)
                        .putInt("GunWarItemType", 12));
                item.setCustomName(language.translateString("itemTeamSelectBlue"));
                return item;
            case 201: //爆破模式 炸弹
                item = Item.get(46);
                item.setNamedTag(new CompoundTag()
                        .putBoolean("isGunWarItem", true)
                        .putInt("GunWarItemType", 201));
                item.setCustomName(language.translateString("item_Bomb_Name"));
                return item;
            default:
                return item;
        }
    }

    /**
     * 重置玩家状态
     * @param player 玩家
     * @param joinRoom 是否为加入房间
     */
    public static void rePlayerState(Player player, boolean joinRoom) {
        player.removeAllEffects();
        player.getFoodData().setLevel(player.getFoodData().getMaxLevel());
        player.getAdventureSettings().set(AdventureSettings.Type.ALLOW_FLIGHT, false).update();
        if (joinRoom) {
            if (GunWar.getInstance().isEnableAloneHealth()) {
                //允许触发EntityRegainHealthEvent
                player.setHealth(player.getMaxHealth() - 1);
            }else {
                player.setHealth(player.getMaxHealth());
            }
            player.setNameTagVisible(false);
            player.setNameTagAlwaysVisible(false);
            player.setGamemode(Player.ADVENTURE);
        }else {
            player.setHealth(player.getMaxHealth());
            player.setNameTag(player.getName());
            player.setNameTagVisible(true);
            player.setNameTagAlwaysVisible(true);
            player.setGamemode(Player.SURVIVAL);
        }
    }

    /**
     * 播放声音
     * @param room 房间
     * @param sound 声音
     */
    public static void playSound(BaseRoom room, Sound sound) {
        room.getPlayers().keySet().forEach(player -> playSound(player, sound));
    }

    /**
     * 发包方式播放声音
     * @param player 玩家
     * @param sound 声音
     */
    public static void playSound(Player player, Sound sound) {
        PlaySoundPacket packet = new PlaySoundPacket();
        packet.name = sound.getSound();
        packet.volume = 1.0F;
        packet.pitch = 1.0F;
        packet.x = player.getFloorX();
        packet.y = player.getFloorY();
        packet.z = player.getFloorZ();
        player.dataPacket(packet);
    }

    /**
     * 获取底部 Y
     * 调用前应判断非空
     * @param player 玩家
     * @return Y
     */
    public static double getFloorY(Player player) {
        for (int y = 0; y < 10; y++) {
            Level level = player.getLevel();
            Block block = level.getBlock(player.getFloorX(), player.getFloorY() - y, player.getFloorZ());
            if (block.getId() != 0) {
                if (block.getBoundingBox() != null) {
                    return block.getBoundingBox().getMaxY() + 0.2;
                }
                return block.getMinY() + 0.2;
            }
        }
        return player.getFloorY();
    }

    /**
     * 放烟花
     * GitHub：https://github.com/PetteriM1/FireworkShow
     * @param position 位置
     */
    public static void spawnFirework(Position position) {
        Level level = position.getLevel();
        ItemFirework item = new ItemFirework();
        CompoundTag tag = new CompoundTag();
        CompoundTag ex = new CompoundTag();
        ex.putByteArray("FireworkColor",new byte[]{
                (byte) DyeColor.values()[GunWar.RANDOM.nextInt(ItemFirework.FireworkExplosion.ExplosionType.values().length)].getDyeData()
        });
        ex.putByteArray("FireworkFade",new byte[0]);
        ex.putBoolean("FireworkFlicker",GunWar.RANDOM.nextBoolean());
        ex.putBoolean("FireworkTrail",GunWar.RANDOM.nextBoolean());
        ex.putByte("FireworkType",ItemFirework.FireworkExplosion.ExplosionType.values()
                [GunWar.RANDOM.nextInt(ItemFirework.FireworkExplosion.ExplosionType.values().length)].ordinal());
        tag.putCompound("Fireworks",(new CompoundTag("Fireworks")).putList(new ListTag<CompoundTag>("Explosions").add(ex)).putByte("Flight",1));
        item.setNamedTag(tag);
        CompoundTag nbt = new CompoundTag();
        nbt.putList(new ListTag<DoubleTag>("Pos")
                .add(new DoubleTag("",position.x+0.5D))
                .add(new DoubleTag("",position.y+0.5D))
                .add(new DoubleTag("",position.z+0.5D))
        );
        nbt.putList(new ListTag<DoubleTag>("Motion")
                .add(new DoubleTag("",0.0D))
                .add(new DoubleTag("",0.0D))
                .add(new DoubleTag("",0.0D))
        );
        nbt.putList(new ListTag<FloatTag>("Rotation")
                .add(new FloatTag("",0.0F))
                .add(new FloatTag("",0.0F))

        );
        nbt.putCompound("FireworkItem", NBTIO.putItemHelper(item));
        EntityFirework entity = new EntityFirework(level.getChunk((int)position.x >> 4, (int)position.z >> 4), nbt);
        entity.spawnToAll();
    }

}
