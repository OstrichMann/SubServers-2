package net.ME1312.SubServers.Client.Sponge.Network.Packet;

import net.ME1312.SubServers.Client.Sponge.Library.Config.YAMLSection;
import net.ME1312.SubServers.Client.Sponge.Library.Version.Version;
import net.ME1312.SubServers.Client.Sponge.Network.PacketIn;
import net.ME1312.SubServers.Client.Sponge.Network.SubDataClient;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;

import java.lang.reflect.Field;

/**
 * Reset Packet
 */
public class PacketInReset implements PacketIn {
    private Logger log = null;

    /**
     * New PacketInReset
     */
    public PacketInReset() {
        try {
            Field f = SubDataClient.class.getDeclaredField("log");
            f.setAccessible(true);
            this.log = (Logger) f.get(null);
            f.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {}
    }

    @Override
    public void execute(YAMLSection data) {
        if (data != null && data.contains("m")) log.warn("Received request for a server shutdown: " + data.getString("m"));
        else log.warn("Received request for a server shutdown");
        Sponge.getServer().shutdown();
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
