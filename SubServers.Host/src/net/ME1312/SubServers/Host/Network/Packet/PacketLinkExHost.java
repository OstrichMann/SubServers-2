package net.ME1312.SubServers.Host.Network.Packet;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.SubServers.Host.Network.PacketIn;
import net.ME1312.SubServers.Host.Network.PacketOut;
import net.ME1312.SubServers.Host.Network.SubDataClient;
import net.ME1312.SubServers.Host.ExHost;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Link Host Packet
 */
public class PacketLinkExHost implements PacketIn, PacketOut {
    private ExHost host;
    private Logger log;

    /**
     * New PacketLinkHost
     *
     * @param host SubServers.Host
     */
    public PacketLinkExHost(ExHost host) {
        if (Util.isNull(host)) throw new NullPointerException();
        this.host = host;
        try {
            Field f = SubDataClient.class.getDeclaredField("log");
            f.setAccessible(true);
            this.log = (Logger) f.get(null);
            f.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {}
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("name", host.subdata.getName());
        return data;
    }

    @Override
    public void execute(YAMLSection data) {
        if (data.getInt("r") == 0) {
            try {
                Method m = SubDataClient.class.getDeclaredMethod("init");
                m.setAccessible(true);
                m.invoke(host.subdata);
                m.setAccessible(false);
            } catch (Exception e) {}
        } else {
            log.info.println("Could not link name with host: " + data.getRawString("m"));
            GalaxiEngine.getInstance().stop();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
