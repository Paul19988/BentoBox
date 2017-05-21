package us.tastybento.bskyblock.database;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Players;
import us.tastybento.bskyblock.util.VaultHelper;

public class PlayersManager{
    
    private BSkyBlock plugin;
    private BSBDatabase database;
    
    private HashMap<UUID, Players> playerCache;
    
    /**
     * Provides a memory cache of online player information
     * This is the one-stop-shop of player info
     * If the player is not cached, then a request is made to Players to obtain it
     *  
     * @param plugin
     */
    public PlayersManager(BSkyBlock plugin){
        this.plugin = plugin;
        database = BSBDatabase.getDatabase();
        playerCache = new HashMap<UUID, Players>();
    }
    
    public void load(){
        //TODO
    }
    
    public void save(boolean async){
        if(async){
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                
                @Override
                public void run() {
                    for(Entry<UUID, Players> entry : playerCache.entrySet()){
                        database.savePlayerData(entry.getValue());
                    }
                }
            });
        } else {
            for(Entry<UUID, Players> entry : playerCache.entrySet()){
                database.savePlayerData(entry.getValue());
            }
        }
    }
    
    public void shutdown(){
        save(false);
        playerCache.clear();
    }
    
    public Players getPlayer(UUID uuid){
        return playerCache.get(uuid);
    }
    
    /*
     * Cache control methods
     */

    public Players addPlayer(final UUID playerUUID) {
        if (playerUUID == null)
            return null;
        //plugin.getLogger().info("DEBUG: added player " + playerUUID);
        if (!playerCache.containsKey(playerUUID)) {
                final Players player = new Players(playerUUID);
                playerCache.put(playerUUID, player);
                return player;
        } else {
            return playerCache.get(playerUUID);
        }
    }
    
    /**
     * Stores the player's info and removes the player from the cache
     * 
     * @param player - UUID of player
     *            
     */
    public void removeOnlinePlayer(final UUID player) {
        if (playerCache.containsKey(player)) {
            database.savePlayerData(playerCache.get(player));
            playerCache.remove(player);
            // plugin.getLogger().info("Removing player from cache: " + player);
        }
    }

    /**
     * Removes all players on the server now from cache and saves their info
     */
    public void removeAllPlayers() {
        for (UUID pl : playerCache.keySet()) {
            database.savePlayerData(playerCache.get(pl));
        }
        playerCache.clear();
    }

    /*
     * Player info query methods
     */

    /**
     * Checks if the player is known or not
     * 
     * @param uniqueID
     * @return true if player is know, otherwise false
     */
    public boolean isAKnownPlayer(final UUID uniqueID) {
        if (uniqueID == null) {
            return false;
        }
        // Try cache
        if (playerCache.containsKey(uniqueID)) {
            return true;
        } else {
            // Get from the database - do not add to cache yet
            return database.isPlayerKnown(uniqueID) ? true: false;
        }
    }

    /**
     * Returns the player object for the named player
     * 
     * @param playerUUID
     *            - String name of player
     * @return - player object
     */
    public Players get(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID);
    }

    /**
     * Checks if player has an island.
     * 
     * @param playerUUID
     *            - string name of player
     * @return true if player has island
     */
    public boolean hasIsland(final UUID playerUUID) {
        addPlayer(playerUUID);
        return plugin.getIslands().hasIsland(playerUUID);
    }

    /**
     * Checks if player is in a Team from cache if available
     * 
     * @param playerUUID
     * @return true if player in a team
     */
    public boolean inTeam(final UUID playerUUID) {
        addPlayer(playerUUID);
        Island island = plugin.getIslands().getIsland(playerUUID);
        if (island == null) {
            return false;
        }
        return island.getMembers().size() > 1 ? true: false;
    }

    /**
     * Removes any island associated with this player and generally cleans up
     * the player
     * 
     * @param playerUUID
     */
    public void zeroPlayerData(UUID playerUUID) {
        Players player = addPlayer(playerUUID);
        player.clearHomeLocations();
        plugin.getIslands().removePlayer(playerUUID);
        /*
         * TODO
        playerCache.get(playerUUID).save(); // Needed?
        TopTen.topTenRemoveEntry(playerUUID);*/
    }

    /**
     * Sets the home location for the player
     * @param playerUUID
     * @param location
     * @param number - 1 is default. Can be any number.
     */
    public void setHomeLocation(UUID playerUUID, Location location, int number) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setHomeLocation(location,number);
    }

    /**
     * Set the default home location for player
     * @param playerUUID
     * @param location
     */
    public void setHomeLocation(UUID playerUUID, Location location) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setHomeLocation(location,1);
    }

    /**
     * Clears any home locations for player
     * @param playerUUID
     */
    public void clearHomeLocations(UUID playerUUID) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).clearHomeLocations();
    }

    /**
     * Returns the home location, or null if none
     * 
     * @param playerUUID
     * @param number 
     * @return Home location or null if none
     */
    public Location getHomeLocation(UUID playerUUID, int number) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocation(number);
    }

    /**
     * Gets the default home location for player
     * @param playerUUID
     * @return Home location or null if none
     */
    public Location getHomeLocation(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocation(1);
    }

    /**
     * Provides all home locations for player
     * @param playerUUID
     * @return List of home locations
     */
    public HashMap<Integer, Location> getHomeLocations(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocations();
    }

    /**
     * Attempts to return a UUID for a given player's name. Only uses online or cached information.
     * @param string
     * @return UUID of player or null if unknown
     */
    public UUID getUUID(String string) {
        return getUUID(string, false);
    }

    /**
     * Attempts to return a UUID for a given player's name
     * @param string
     * @param adminCheck - if made via an admin call, this will go out to the 'net and grab - may cause lag
     * @return UUID of player or null if unknown
     */
    @SuppressWarnings("deprecation")
    public UUID getUUID(String string, boolean adminCheck) {
        // Look in the database if it ready
        return database.getUUID(string, adminCheck);
    }

    /**
     * Sets the player's name and updates the name>UUID database
     * @param uniqueId
     * @param name
     */
    public void setPlayerName(UUID uniqueId, String name) {
        addPlayer(uniqueId);
        playerCache.get(uniqueId).setPlayerN(name);
        database.savePlayerName(name, uniqueId);
    }

    /**
     * Obtains the name of the player from their UUID
     * Player must have logged into the game before
     * 
     * @param playerUUID
     * @return String - playerName
     */
    public String getName(UUID playerUUID) {
        if (playerUUID == null) {
            return "";
        }
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getPlayerName();
    }

    /**
     * Reverse lookup - returns the owner of an island from the location
     * 
     * @param loc
     * @return UUID of owner of island
     */
    public UUID getPlayerFromIslandLocation(Location loc) {
        if (loc == null)
            return null;
        // Look in the grid
        Island island = plugin.getIslands().getIslandAt(loc);
        if (island != null) {
            return island.getOwner();
        }
        return null;
    }

    /**
     * Gets how many island resets the player has left
     * 
     * @param playerUUID
     * @return number of resets
     */
    public int getResetsLeft(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getResetsLeft();
    }

    /**
     * Sets how many resets the player has left
     * 
     * @param playerUUID
     * @param resets
     */
    public void setResetsLeft(UUID playerUUID, int resets) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setResetsLeft(resets);
    }

    /**
     * Returns how long the player must wait before they can be invited to an
     * island with the location
     * 
     * @param playerUUID
     * @param location
     * @return time to wait in minutes/hours
     */
    public long getInviteCoolDownTime(UUID playerUUID, Location location) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getInviteCoolDownTime(location);
    }

    /**
     * Starts the timer for the player for this location before which they can
     * be invited
     * Called when they are kicked from an island or leave.
     * 
     * @param playerUUID
     * @param location
     */
    public void startInviteCoolDownTimer(UUID playerUUID, Location location) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).startInviteCoolDownTimer(location);
    }

    /**
     * Returns the locale for this player. If missing, will return nothing
     * @param playerUUID
     * @return name of the locale this player uses
     */
    public String getLocale(UUID playerUUID) {
        addPlayer(playerUUID);
        if (playerUUID == null) {
            return "";
        }
        return playerCache.get(playerUUID).getLocale();
    }

    /**
     * Sets the locale this player wants to use
     * @param playerUUID
     * @param localeName
     */
    public void setLocale(UUID playerUUID, String localeName) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setLocale(localeName);
    }

    /**
     * Ban target from a player's island. Ban may be blocked by event being cancelled.
     * @param playerUUID
     * @param targetUUID
     * @return true if banned, false if not
     */
    public boolean ban(UUID playerUUID, UUID targetUUID) {
        addPlayer(playerUUID);
        addPlayer(targetUUID);
        Island island = plugin.getIslands().getIsland(playerUUID);
        if (island != null) {
            // Player has island
            return island.addToBanList(targetUUID);
        }
        return false;
    }

    /**
     * Unban target from player's island
     * @param playerUUID
     * @param targetUUID
     * @return 
     */
    public boolean unBan(UUID playerUUID, UUID targetUUID) {
        addPlayer(playerUUID);
        addPlayer(targetUUID);
        Island island = plugin.getIslands().getIsland(playerUUID);
        if (island != null) {
            // Player has island
            return island.removeFromBanList(targetUUID);
        }
        return false;
    }

    /**
     * @param playerUUID
     * @param targetUUID
     * @return true if target is banned from player's island
     */
    public boolean isBanned(UUID playerUUID, UUID targetUUID) {
        if (playerUUID == null || targetUUID == null) {
            // If the island is unowned, then playerUUID could be null
            return false;
        }
        addPlayer(playerUUID);
        addPlayer(targetUUID);
        // Check if the target player has a permission bypass (admin.noban)
        Player target = plugin.getServer().getPlayer(targetUUID);
        if (target != null && VaultHelper.checkPerm(target, Settings.PERMPREFIX + "admin.noban")) {
            return false;
        }
        Island island = plugin.getIslands().getIsland(playerUUID);
        if (island != null) {
            // Player has island
            return island.isBanned(targetUUID);
        }
        return false;
    }

    /**
     * Clears resets for online players or players in the cache
     * @param resetLimit
     */
    public void clearResets(int resetLimit) {
        for (Players player : playerCache.values()) {
            player.setResetsLeft(resetLimit);
        }   
    }

    /**
     * Sets whether the player uses the control panel or not when doing /island
     * @param b
     */
    public void setControlPanel(UUID playerUUID, boolean b) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setControlPanel(b);

    }

    /**
     * Gets whether the player uses the control panel or not when doing /island
     * @param playerUUID
     * @return true if they use the control panel
     */
    public boolean getControlPanel(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getControlPanel();

    }

    /**
     * Add death to player
     * @param playerUUID
     */
    public void addDeath(UUID playerUUID) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).addDeath();
    }

    /**
     * Set death number for player
     * @param playerUUID
     * @param deaths
     */
    public void setDeaths(UUID playerUUID, int deaths) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setDeaths(deaths);
    }

    /**
     * Get number of times player has died in ASkyBlock worlds since counting began
     * @param playerUUID
     * @return
     */
    public int getDeaths(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getDeaths();
    }

}
