package cn.lanink.gunwar.room.blasting;

import cn.lanink.gamecore.utils.exception.RoomLoadException;
import cn.lanink.gunwar.GunWar;
import cn.lanink.gunwar.entity.EntityGunWarBomb;
import cn.lanink.gunwar.entity.EntityGunWarBombBlock;
import cn.lanink.gunwar.room.base.BaseRoom;
import cn.lanink.gunwar.room.base.Team;
import cn.lanink.gunwar.utils.Tools;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.BlockID;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.DummyBossBar;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lt_name
 */
public class BlastingModeRoom extends BaseRoom {

    protected final String blastingPointA, blastingPointB;
    protected EntityGunWarBomb entityGunWarBomb;
    private EntityGunWarBombBlock entityGunWarBombBlock;
    protected final ConcurrentHashMap<Player, DummyBossBar> bossBarMap = new ConcurrentHashMap<>();
    protected boolean bombWasFound = false;
    public Player demolitionBombPlayer;
    protected boolean changeTeam = false;

    /**
     * 初始化
     *
     * @param level  游戏世界
     * @param config 配置文件
     */
    public BlastingModeRoom(Level level, Config config) throws RoomLoadException {
        super(level, config);
        this.blastingPointA = config.getString("blastingPointA");
        this.blastingPointB = config.getString("blastingPointB");
        if ("".equals(this.blastingPointA.trim()) || "".equals(this.blastingPointB.trim())) {
            throw new RoomLoadException("§c房间：" + level.getFolderName() + " 配置不完整，加载失败！");
        }
    }

    @Override
    public List<String> getListeners() {
        List<String> list = super.getListeners();
        list.add("BlastingGameListener");
        return list;
    }

    @Override
    public void timeTask() {
        if (this.getPlayers().isEmpty()) {
            this.endGame();
            return;
        }
        if (!roundIsEnd) {
            //Boss血条显示炸弹爆炸倒计时
            if (this.entityGunWarBomb != null && !this.entityGunWarBomb.isClosed() &&
                    this.entityGunWarBomb.getExplosionTime() > 0) {
                double discoveryDistance = this.getBlastingPointRadius() * 0.8 + 5;
                if (!this.bombWasFound) {
                    for (Map.Entry<Player, Team> entry : this.getPlayers().entrySet()) {
                        if (entry.getValue() == Team.BLUE) {
                            if (entry.getKey().distance(this.getBlastingPointA()) <= discoveryDistance ||
                                    entry.getKey().distance(this.getBlastingPointB()) <= discoveryDistance) {
                                this.bombWasFound = true;
                                String s;
                                if (this.entityGunWarBomb.distance(this.getBlastingPointA()) <= this.getBlastingPointRadius()) {
                                    s = this.language.translateString("game_blasting_bombFound", "§cA");
                                }else {
                                    s = this.language.translateString("game_blasting_bombFound", "§9B");
                                }
                                Tools.sendTitle(this, Team.BLUE, "", s);
                            }
                        }
                    }
                }
                String s = "";
                if (this.bombWasFound) {
                    if (this.entityGunWarBomb.distance(this.getBlastingPointA()) <= this.getBlastingPointRadius()) {
                        s += this.language.translateString("game_blasting_bombFound", "§cA");
                    }else {
                        s += this.language.translateString("game_blasting_bombFound", "§9B");
                    }
                }
                s += this.language.translateString("game_blasting_countdownToBombExplosion",
                        this.entityGunWarBomb.getExplosionTime());
                for (Map.Entry<Player, Team> entry : this.getPlayers().entrySet()) {
                    Tools.createBossBar(entry.getKey(), this.bossBarMap);
                    DummyBossBar bossBar = this.bossBarMap.get(entry.getKey());
                    bossBar.setText(s);
                    bossBar.setLength(this.entityGunWarBomb.getExplosionTime() / 50F * 100);
                }
            }else if (!this.bossBarMap.isEmpty()) {
                for (Map.Entry<Player, DummyBossBar> entry : this.bossBarMap.entrySet()) {
                    entry.getKey().removeBossBar(entry.getValue().getBossBarId());
                }
                this.bossBarMap.clear();
            }

            if (this.gameTime <= 0 && (this.entityGunWarBomb == null || this.entityGunWarBomb.isClosed())) {
                this.roundEnd(2);
                this.gameTime = this.getSetGameTime();
                return;
            }
            this.gameTime--;
            int red = 0, blue = 0;
            for (Team team : this.getPlayers().values()) {
                if (team == Team.RED) {
                    red++;
                } else if (team == Team.BLUE) {
                    blue++;
                }
            }
            if (red == 0) {
                if (this.entityGunWarBomb == null) {
                    this.roundEnd(2);
                    this.gameTime = this.getSetGameTime();
                }
            } else if (blue == 0) {
                this.roundEnd(1);
                this.gameTime = this.getSetGameTime();
            }
        }
        //显示爆破点
        Server.getInstance().getScheduler().scheduleAsyncTask(this.gunWar, new AsyncTask() {
            @Override
            public void onRun() {
                LinkedList<Vector3> list = Tools.getRoundEdgePoint(getBlastingPointA(), getBlastingPointRadius());
                list.addAll(Tools.getRoundEdgePoint(getBlastingPointB(), getBlastingPointRadius()));
                for (Vector3 vector3 : list) {
                    vector3.y += 0.1;
                    if (getLevel().getBlock(vector3).getId() == BlockID.AIR) {
                        for (int y = vector3.getFloorY(); y > y-5; y--) {
                            if (getLevel().getBlock(new Vector3(vector3.x, y, vector3.z)).getId() != BlockID.AIR) {
                                vector3.y = y + 1.1;
                                break;
                            }
                        }
                    }else {
                        for (int y = vector3.getFloorY(); y < y+5; y++) {
                            if (getLevel().getBlock(new Vector3(vector3.x, y, vector3.z)).getId() == BlockID.AIR) {
                                vector3.y = y + 0.1;
                                break;
                            }
                        }
                    }
                    getLevel().addParticleEffect(vector3, ParticleEffect.REDSTONE_TORCH_DUST);
                }
            }
        });
    }

    @Override
    protected void initData() {
        super.initData();
        this.entityGunWarBomb = null;
        this.entityGunWarBombBlock = null;
        if (this.bossBarMap != null && !this.bossBarMap.isEmpty()) {
            for (Map.Entry<Player, DummyBossBar> entry : this.bossBarMap.entrySet()) {
                entry.getKey().removeBossBar(entry.getValue().getBossBarId());
            }
            this.bossBarMap.clear();
        }
        this.bombWasFound = false;
        this.demolitionBombPlayer = null;
        this.changeTeam = false;
    }

    @Override
    public void roundStart() {
        int delay = 0;
        //交换队伍
        if (!this.changeTeam && (this.redScore + this.blueScore) >= this.victoryScore * 0.6) {
            delay = 60;
            Tools.sendTitle(this, this.language.translateString("game_blasting_changeTeam"));
            this.changeTeam = true;
            LinkedList<Player> oldRedTeam = new LinkedList<>();
            LinkedList<Player> oldBlueTeam = new LinkedList<>();
            for (Map.Entry<Player, Team> entry : this.getPlayers().entrySet()) {
                switch (entry.getValue()) {
                    case RED:
                    case RED_DEATH:
                        oldRedTeam.add(entry.getKey());
                        break;
                    case BLUE:
                    case BLUE_DEATH:
                        oldBlueTeam.add(entry.getKey());
                        break;
                    default:
                        this.quitRoom(entry.getKey());
                        break;
                }
            }
            for (Player player : oldRedTeam) {
                this.players.put(player, Team.BLUE);
                player.setNameTag("§9" + player.getName());
            }
            for (Player player : oldBlueTeam) {
                this.players.put(player, Team.RED);
                player.setNameTag("§c" + player.getName());
            }
            int cache = this.redScore;
            this.redScore = this.blueScore;
            this.blueScore = cache;
        }
        Server.getInstance().getScheduler().scheduleDelayedTask(this.gunWar, () -> {
            super.roundStart();
            //随机挑选一名红队成员给炸弹
            LinkedList<Player> list = new LinkedList<>();
            for (Map.Entry<Player, Team> entry : this.getPlayers().entrySet()) {
                if (entry.getValue() == Team.RED) {
                    list.add(entry.getKey());
                }
            }
            if (!list.isEmpty()) {
                Player player = list.get(GunWar.RANDOM.nextInt(list.size()));
                player.getInventory().addItem(Tools.getItem(201));
                player.sendTitle("", this.language.translateString("game_blasting_youCarryBomb"));
            }
        }, delay);
    }

    @Override
    public void roundEnd(int victory) {
        if (this.entityGunWarBomb != null) {
            this.entityGunWarBomb.close();
        }
        if (this.entityGunWarBombBlock != null) {
            this.entityGunWarBombBlock.close();
        }
        super.roundEnd(victory);
        this.entityGunWarBomb = null;
        this.entityGunWarBombBlock = null;
        if (!this.bossBarMap.isEmpty()) {
            for (Map.Entry<Player, DummyBossBar> entry : this.bossBarMap.entrySet()) {
                entry.getKey().removeBossBar(entry.getValue().getBossBarId());
            }
            this.bossBarMap.clear();
        }
        this.bombWasFound = false;
        this.demolitionBombPlayer = null;
    }

    @Override
    public void playerDeath(Player player, Entity damager, String killMessage) {
        Item bomb = Tools.getItem(201);
        for (Item item : player.getInventory().getContents().values()) {
            if (bomb.equals(item)) {
                this.getLevel().dropItem(player, bomb);
                break;
            }
        }
        super.playerDeath(player, damager, killMessage);
    }

    public void setEntityGunWarBomb(EntityGunWarBomb entityGunWarBomb) {
        this.entityGunWarBomb = entityGunWarBomb;
    }

    public EntityGunWarBomb getEntityGunWarBomb() {
        return this.entityGunWarBomb;
    }

    public void setEntityGunWarBombBlock(EntityGunWarBombBlock entityGunWarBombBlock) {
        this.entityGunWarBombBlock = entityGunWarBombBlock;
    }

    public EntityGunWarBombBlock getEntityGunWarBombBlock() {
        return this.entityGunWarBombBlock;
    }

    public void setRoundIsEnd(boolean roundIsEnd) {
        this.roundIsEnd = roundIsEnd;
    }

    /**
     * 炸弹爆炸
     */
    public void bombExplosion() {
        this.roundIsEnd = true;
        Tools.sendTitle(this, this.language.translateString("game_blasting_bombHasExploded"));
        for (Map.Entry<Player, DummyBossBar> entry : this.bossBarMap.entrySet()) {
            entry.getKey().removeBossBar(entry.getValue().getBossBarId());
        }
        Server.getInstance().getScheduler().scheduleDelayedTask(this.gunWar, () -> this.roundEnd(1), 60);
    }

    /**
     * @return 爆破点A
     */
    public Position getBlastingPointA() {
        String[] s = this.blastingPointA.split(":");
        return new Position(Integer.parseInt(s[0]),
                Integer.parseInt(s[1]),
                Integer.parseInt(s[2]),
                this.getLevel());
    }

    /**
     * @return 爆破点B
     */
    public Position getBlastingPointB() {
        String[] s = this.blastingPointB.split(":");
        return new Position(Integer.parseInt(s[0]),
                Integer.parseInt(s[1]),
                Integer.parseInt(s[2]),
                this.getLevel());
    }

    /**
     * @return 爆破点范围半径
     */
    public double getBlastingPointRadius() {
        return 5;
    }

    /**
     * @return 安装炸弹用时 (tick)
     */
    public int getPlantBombTime() {
        return 5 * 20;
    }

    /**
     * @return 拆除炸弹那用时 (tick)
     */
    public int getDemolitionBombTime() {
        return 5 * 20;
    }

}
