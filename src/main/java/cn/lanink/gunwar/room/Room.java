package cn.lanink.gunwar.room;

import cn.lanink.gunwar.utils.SavePlayerInventory;
import cn.lanink.gunwar.utils.Tools;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.utils.Config;
import tip.messages.NameTagMessage;
import tip.utils.Api;

import java.util.LinkedHashMap;

/**
 * 房间
 */
public class Room {

    private int mode; //0未初始化 1等待 2游戏 3胜利结算
    private int round; //游戏回合
    private final String level, waitSpawn, redSpawn, blueSpawn;
    private final int setWaitTime, setGameTime;
    public int waitTime, gameTime;
    private LinkedHashMap<Player, Integer> players = new LinkedHashMap<>(); //0未分配 1红队 2蓝队
    private LinkedHashMap<Player, Integer> playerHealth = new LinkedHashMap<>(); //玩家血量

    /**
     * 初始化
     * @param config 配置文件
     */
    public Room(Config config) {
        this.level = config.getString("World");
        this.waitSpawn = config.getString("waitSpawn");
        this.redSpawn = config.getString("redSpawn");
        this.blueSpawn = config.getString("blueSpawn");
        this.setWaitTime = config.getInt("waitTime");
        this.setGameTime = config.getInt("gameTime");
        this.initTime();
        this.mode = 0;
    }

    private void initTime() {
        this.waitTime = this.setWaitTime;
        this.gameTime = this.setGameTime;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return this.mode;
    }

    /**
     * 结束房间
     */
    public void endGame() {
        this.players.keySet().forEach(player -> this.quitRoom(player, true));
        this.mode = 0;
    }

    /**
     * 加入房间
     * @param player 玩家
     */
    public void joinRoom(Player player) {
        this.players.put(player, 0);
        SavePlayerInventory.savePlayerInventory(player, false);
        Tools.rePlayerState(player, true);
        player.teleport(this.getWaitSpawn());
        NameTagMessage nameTagMessage = new NameTagMessage(this.level, true, "");
        Api.setPlayerShowMessage(player.getName(), nameTagMessage);
        player.sendMessage("§a你已加入房间: " + this.level);
    }

    /**
     * 退出房间
     * @param player 玩家
     */
    public void quitRoom(Player player, boolean online) {
        this.players.remove(player);
        if (online) {
            player.teleport(Server.getInstance().getDefaultLevel().getSafeSpawn());
            Tools.rePlayerState(player, false);
            SavePlayerInventory.savePlayerInventory(player, true);
        }
    }

    public boolean isPlaying(Player player) {
        return this.players.containsKey(player);
    }

    /**
     * 获取玩家列表
     * @return 玩家列表
     */
    public LinkedHashMap<Player, Integer> getPlayers() {
        return this.players;
    }

    /**
     * 获取玩家队伍
     * @param player 玩家
     * @return 所属队伍
     */
    public int getPlayerMode(Player player) {
        if (this.players.containsKey(player)) {
            return this.players.get(player);
        }
        return 0;
    }

    public int getWaitTime() {
        return this.setWaitTime;
    }

    public int getGameTime() {
        return this.setGameTime;
    }

    /**
     * 获取世界
     * @return 世界
     */
    public Level getLevel() {
        return Server.getInstance().getLevelByName(this.level);
    }

    /**
     * 获取等待出生点
     * @return 出生点
     */
    public Position getWaitSpawn() {
        String[] s = this.waitSpawn.split(":");
        return new Position(Integer.parseInt(s[0]),
                Integer.parseInt(s[1]),
                Integer.parseInt(s[2]),
                this.getLevel());
    }

    /**
     * 获取红队出生点
     * @return 出生点
     */
    public Position getRedSpawn() {
        String[] s = this.redSpawn.split(":");
        return new Position(Integer.parseInt(s[0]),
                Integer.parseInt(s[1]),
                Integer.parseInt(s[2]),
                this.getLevel());
    }

    /**
     * 获取蓝队出生点
     * @return 出生点
     */
    public Position getBlueSpawn() {
        String[] s = this.blueSpawn.split(":");
        return new Position(Integer.parseInt(s[0]),
                Integer.parseInt(s[1]),
                Integer.parseInt(s[2]),
                this.getLevel());
    }

}
