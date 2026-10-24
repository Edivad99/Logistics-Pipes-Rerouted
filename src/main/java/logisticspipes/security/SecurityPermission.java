package logisticspipes.security;

import logisticspipes.Translations;

/**
 * One thing a security station can allow a player to do, in the order its player popup lists them.
 */
public enum SecurityPermission {
    OPEN_GUI("openGui", Translations.Screen.SECURITY_STATION_PLAYER_CONFIGURE_SETTINGS),
    OPEN_REQUEST("openRequest", Translations.Screen.SECURITY_STATION_PLAYER_ACTIVE_REQUESTING),
    OPEN_UPGRADES("openUpgrades", Translations.Screen.SECURITY_STATION_PLAYER_UPGRADE_PIPES),
    OPEN_NETWORK_MONITOR("openNetworkMonitor", Translations.Screen.SECURITY_STATION_PLAYER_CHECK_NETWORK),
    REMOVE_PIPES("removePipes", Translations.Screen.SECURITY_STATION_PLAYER_REMOVE_PIPES),
    ACCESS_ROUTING_CHANNELS("accessRoutingChannels",
        Translations.Screen.SECURITY_STATION_PLAYER_ACCESS_ROUTING_CHANNELS);

    /** The boolean's key in the station's save data. */
    final String key;
    public final String translationKey;

    SecurityPermission(String key, String translationKey) {
        this.key = key;
        this.translationKey = translationKey;
    }
}
