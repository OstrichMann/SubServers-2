package net.ME1312.SubServers.Bungee.Network.Packet;

import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Client;
import net.ME1312.SubServers.Bungee.Network.PacketIn;
import net.ME1312.SubServers.Bungee.Network.PacketOut;
import net.ME1312.SubServers.Bungee.SubPlugin;
import net.md_5.bungee.api.config.ListenerInfo;

import java.util.LinkedList;

/**
 * Download Proxy Info Packet
 */
public class PacketDownloadPlatformInfo implements PacketIn, PacketOut {
    private SubPlugin plugin;
    private String id;

    /**
     * New PacketDownloadPlatformInfo (In)
     *
     * @param plugin SubPlugin
     */
    public PacketDownloadPlatformInfo(SubPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * New PacketDownloadPlatformInfo (Out)
     *
     * @param plugin SubPlugin
     * @param id Receiver ID
     */
    public PacketDownloadPlatformInfo(SubPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        if (id != null) data.set("id", id);
        YAMLSection subservers = new YAMLSection();
        subservers.set("version", plugin.api.getWrapperVersion().toString());
        if (plugin.api.getWrapperBuild() != null) subservers.set("build", plugin.api.getWrapperBuild().toString());
        subservers.set("last-reload", plugin.resetDate);
        subservers.set("hosts", plugin.api.getHosts().size());
        subservers.set("subservers", plugin.api.getSubServers().size());
        data.set("subservers", subservers);
        YAMLSection bungee = new YAMLSection();
        bungee.set("version", plugin.api.getProxyVersion().toString());
        bungee.set("disabled-cmds", plugin.getConfig().getDisabledCommands());
        bungee.set("player-limit", plugin.getConfig().getPlayerLimit());
        bungee.set("servers", plugin.api.getServers().size());
        LinkedList<YAMLSection> listeners = new LinkedList<YAMLSection>();
        for (ListenerInfo info : plugin.getConfig().getListeners()) {
            YAMLSection listener = new YAMLSection();
            listener.set("forced-hosts", info.getForcedHosts());
            listener.set("motd", info.getMotd());
            listener.set("priorities", info.getServerPriority());
            listener.set("player-limit", info.getMaxPlayers());
            listeners.add(listener);
        }
        bungee.set("listeners", listeners);
        data.set("bungee", bungee);
        YAMLSection minecraft = new YAMLSection();
        LinkedList<String> mcversions = new LinkedList<String>();
        for (Version version : plugin.api.getGameVersion()) mcversions.add(version.toString());
        minecraft.set("version", mcversions);
        minecraft.set("players", plugin.api.getGlobalPlayers().size());
        data.set("minecraft", minecraft);
        YAMLSection system = new YAMLSection();
        YAMLSection os = new YAMLSection();
        os.set("name", System.getProperty("os.name"));
        os.set("version", System.getProperty("os.version"));
        system.set("os", os);
        YAMLSection java = new YAMLSection();
        java.set("version",  System.getProperty("java.version"));
        system.set("java", java);
        data.set("system", system);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        client.sendPacket(new PacketDownloadPlatformInfo(plugin, (data != null && data.contains("id"))?data.getRawString("id"):null));
    }

    @Override
    public Version getVersion() {
        return new Version("2.11.0a");
    }
}
