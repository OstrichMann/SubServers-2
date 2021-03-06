package net.ME1312.SubServers.Bungee;

import com.dosse.upnp.UPnP;
import com.google.gson.Gson;
import net.ME1312.SubServers.Bungee.Event.SubAddProxyEvent;
import net.ME1312.SubServers.Bungee.Event.SubRemoveProxyEvent;
import net.ME1312.SubServers.Bungee.Event.SubStoppedEvent;
import net.ME1312.SubServers.Bungee.Host.*;
import net.ME1312.SubServers.Bungee.Library.*;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLConfig;
import net.ME1312.SubServers.Bungee.Library.Config.YAMLSection;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidHostException;
import net.ME1312.SubServers.Bungee.Library.Exception.InvalidServerException;
import net.ME1312.SubServers.Bungee.Library.Version.Version;
import net.ME1312.SubServers.Bungee.Network.Cipher;
import net.ME1312.SubServers.Bungee.Network.ClientHandler;
import net.ME1312.SubServers.Bungee.Network.Packet.PacketOutReload;
import net.ME1312.SubServers.Bungee.Network.SubDataServer;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Main Plugin Class
 */
public final class SubPlugin extends BungeeCord implements Listener {
    protected final LinkedHashMap<String, LinkedHashMap<String, String>> lang = new LinkedHashMap<String, LinkedHashMap<String, String>>();
    protected final HashMap<String, Class<? extends Host>> hostDrivers = new HashMap<String, Class<? extends Host>>();
    public final HashMap<String, Proxy> proxies = new HashMap<String, Proxy>();
    public final HashMap<String, Host> hosts = new HashMap<String, Host>();
    public final HashMap<String, Server> exServers = new HashMap<String, Server>();
    private final HashMap<String, ServerInfo> legServers = new HashMap<String, ServerInfo>();

    public final PrintStream out;
    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public YAMLConfig config;
    private YAMLConfig bungeeconfig;
    public YAMLConfig langconfig;
    public final SubAPI api = new SubAPI(this);
    public SubDataServer subdata = null;
    public SubServer sudo = null;
    public static final Version version = Version.fromString("2.13.2a");

    public Proxy redis = null;
    public boolean canSudo = false;
    public final boolean isPatched;
    public long resetDate = 0;
    private boolean running = false;
    private boolean posted = false;
    private static BigInteger lastSignature = BigInteger.valueOf(-1);

    @SuppressWarnings("unchecked")
    protected SubPlugin(PrintStream out, boolean isPatched) throws IOException {
        this.isPatched = isPatched;
        System.out.println("SubServers > Loading SubServers.Bungee v" + version.toString() + " Libraries (for Minecraft " + api.getGameVersion()[api.getGameVersion().length - 1] + ")");

        this.out = out;
        if (!(new UniversalFile(dir, "config.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/bungee.yml", new UniversalFile(dir, "config.yml").getPath());
            YAMLConfig tmp = new YAMLConfig(new UniversalFile("config.yml"));
            tmp.get().set("stats", UUID.randomUUID().toString());
            tmp.save();
            System.out.println("SubServers > Created ~/config.yml");
        }
        bungeeconfig = new YAMLConfig(new UniversalFile(dir, "config.yml"));

        UniversalFile dir = new UniversalFile(this.dir, "SubServers");
        dir.mkdir();
        if (!(new UniversalFile(dir, "config.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
            System.out.println("SubServers > Created ~/SubServers/config.yml");
        } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "config.yml"))).get().getSection("Settings").getString("Version", "0")).compareTo(new Version("2.11.2a+"))) != 0) {
            Files.move(new UniversalFile(dir, "config.yml").toPath(), new UniversalFile(dir, "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
            System.out.println("SubServers > Updated ~/SubServers/config.yml");
        }
        config = new YAMLConfig(new UniversalFile(dir, "config.yml"));

        if (!(new UniversalFile(dir, "lang.yml").exists())) {
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/lang.yml", new UniversalFile(dir, "lang.yml").getPath());
            System.out.println("SubServers > Created ~/SubServers/lang.yml");
        } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "lang.yml"))).get().getString("Version", "0")).compareTo(new Version("2.13.1a+"))) != 0) {
            Files.move(new UniversalFile(dir, "lang.yml").toPath(), new UniversalFile(dir, "lang.old" + Math.round(Math.random() * 100000) + ".yml").toPath());
            Util.copyFromJar(SubPlugin.class.getClassLoader(), "net/ME1312/SubServers/Bungee/Library/Files/lang.yml", new UniversalFile(dir, "lang.yml").getPath());
            System.out.println("SubServers > Updated ~/SubServers/lang.yml");
        }
        langconfig = new YAMLConfig(new UniversalFile(dir, "lang.yml"));

        if (!(new UniversalFile(dir, "Templates").exists())) {
            new UniversalFile(dir, "Templates").mkdirs();

            Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/vanilla.zip"), new UniversalFile(dir, "Templates"));
            System.out.println("SubServers > Created ~/SubServers/Templates/Vanilla");

            Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/spigot.zip"), new UniversalFile(dir, "Templates"));
            System.out.println("SubServers > Created ~/SubServers/Templates/Spigot");

            Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/forge.zip"), new UniversalFile(dir, "Templates"));
            System.out.println("SubServers > Created ~/SubServers/Templates/Forge");

            Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/sponge.zip"), new UniversalFile(dir, "Templates"));
            System.out.println("SubServers > Created ~/SubServers/Templates/Sponge");
        } else {
            if (new UniversalFile(dir, "Templates:Vanilla:template.yml").exists() && (new Version((new YAMLConfig(new UniversalFile(dir, "Templates:Vanilla:template.yml"))).get().getString("Version", "0")).compareTo(new Version("2.13a/rv3+"))) != 0) {
                Files.move(new UniversalFile(dir, "Templates:Vanilla").toPath(), new UniversalFile(dir, "Templates:Vanilla.old" + Math.round(Math.random() * 100000)).toPath());
                Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/vanilla.zip"), new UniversalFile(dir, "Templates"));
                System.out.println("SubServers > Updated ~/SubServers/Templates/Vanilla");
            }
            if (new UniversalFile(dir, "Templates:Spigot:template.yml").exists() && (new Version((new YAMLConfig(new UniversalFile(dir, "Templates:Spigot:template.yml"))).get().getString("Version", "0")).compareTo(new Version("2.13a/rv2+"))) != 0) {
                Files.move(new UniversalFile(dir, "Templates:Spigot").toPath(), new UniversalFile(dir, "Templates:Spigot.old" + Math.round(Math.random() * 100000)).toPath());
                Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/spigot.zip"), new UniversalFile(dir, "Templates"));
                System.out.println("SubServers > Updated ~/SubServers/Templates/Spigot");
            }
            if (new UniversalFile(dir, "Templates:Forge:template.yml").exists() && (new Version((new YAMLConfig(new UniversalFile(dir, "Templates:Forge:template.yml"))).get().getString("Version", "0")).compareTo(new Version("2.13a/rv3+"))) != 0) {
                Files.move(new UniversalFile(dir, "Templates:Forge").toPath(), new UniversalFile(dir, "Templates:Forge.old" + Math.round(Math.random() * 100000)).toPath());
                Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/forge.zip"), new UniversalFile(dir, "Templates"));
                System.out.println("SubServers > Updated ~/SubServers/Templates/Forge");
            }
            if (new UniversalFile(dir, "Templates:Sponge:template.yml").exists() && (new Version((new YAMLConfig(new UniversalFile(dir, "Templates:Sponge:template.yml"))).get().getString("Version", "0")).compareTo(new Version("2.13a/rv3+"))) != 0) {
                Files.move(new UniversalFile(dir, "Templates:Sponge").toPath(), new UniversalFile(dir, "Templates:Sponge.old" + Math.round(Math.random() * 100000)).toPath());
                Util.unzip(SubPlugin.class.getResourceAsStream("/net/ME1312/SubServers/Bungee/Library/Files/Templates/sponge.zip"), new UniversalFile(dir, "Templates"));
                System.out.println("SubServers > Updated ~/SubServers/Templates/Sponge");
            }
        }

        if (new UniversalFile(dir, "Recently Deleted").exists()) {
            int f = new UniversalFile(dir, "Recently Deleted").listFiles().length;
            for (File file : new UniversalFile(dir, "Recently Deleted").listFiles()) {
                try {
                    if (file.isDirectory()) {
                        if (new UniversalFile(dir, "Recently Deleted:" + file.getName() + ":info.json").exists()) {
                            FileReader reader = new FileReader(new UniversalFile(dir, "Recently Deleted:" + file.getName() + ":info.json"));
                            YAMLSection info = new YAMLSection(new Gson().fromJson(Util.readAll(reader), Map.class));
                            reader.close();
                            if (info.contains("Timestamp")) {
                                if (TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTime().getTime() - info.getLong("Timestamp")) >= 7) {
                                    Util.deleteDirectory(file);
                                    f--;
                                    System.out.println("SubServers > Removed ~/SubServers/Recently Deleted/" + file.getName());
                                }
                            } else {
                                Util.deleteDirectory(file);
                                f--;
                                System.out.println("SubServers > Removed ~/SubServers/Recently Deleted/" + file.getName());
                            }
                        } else {
                            Util.deleteDirectory(file);
                            f--;
                            System.out.println("SubServers > Removed ~/SubServers/Recently Deleted/" + file.getName());
                        }
                    } else {
                        Files.delete(file.toPath());
                        f--;
                        System.out.println("SubServers > Removed ~/SubServers/Recently Deleted/" + file.getName());
                    }
                } catch (Exception e) {
                    System.out.println("SubServers > Problem scanning ~/SubServers/Recently Deleted/" + file.getName());
                    e.printStackTrace();
                    Files.delete(file.toPath());
                }
            }
            if (f <= 0) {
                Files.delete(new UniversalFile(dir, "Recently Deleted").toPath());
            }
        }

        api.addHostDriver(net.ME1312.SubServers.Bungee.Host.Internal.InternalHost.class, "built-in");
        api.addHostDriver(net.ME1312.SubServers.Bungee.Host.External.ExternalHost.class, "network");

        getPluginManager().registerListener(null, this);

        System.out.println("SubServers > Pre-Parsing Config...");
        for (String name : config.get().getSection("Servers").getKeys()) {
            try {
                if (Util.getCaseInsensitively(config.get().getSection("Hosts").get(), config.get().getSection("Servers").getSection(name).getString("Host")) == null) throw new InvalidServerException("There is no host with this name: " + config.get().getSection("Servers").getSection(name).getString("Host"));
                legServers.put(name, new BungeeServerInfo(name, new InetSocketAddress(InetAddress.getByName((String) ((Map<String, ?>) Util.getCaseInsensitively(config.get().getSection("Hosts").get(), config.get().getSection("Servers").getSection(name).getString("Host"))).get("Address")), config.get().getSection("Servers").getSection(name).getInt("Port")), config.get().getSection("Servers").getSection(name).getColoredString("Motd", '&'), config.get().getSection("Servers").getSection(name).getBoolean("Restricted")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("SubServers > Loading BungeeCord Libraries...");
    }

    /**
     * Load SubServers before BungeeCord finishes
     */
    @Override
    @SuppressWarnings("unchecked")
    public void startListeners() {
        try {
            if (getPluginManager().getPlugin("RedisBungee") != null) redis = Util.getDespiteException(() -> new Proxy((String) redis("getServerId")), null);
            reload();

            if (UPnP.isUPnPAvailable()) {
                if (config.get().getSection("Settings").getSection("UPnP", new YAMLSection()).getBoolean("Forward-Proxy", true)) for (ListenerInfo listener : getConfig().getListeners()) {
                    UPnP.openPortTCP(listener.getHost().getPort());
                }
            } else {
                getLogger().warning("UPnP is currently unavailable; Ports may not be automatically forwarded on this device");
            }

            super.startListeners();

            if (!posted) {
                post();
                posted = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load data from the config (will attempt to merge with current configuration)
     *
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void reload() throws IOException {
        List<String> ukeys = new ArrayList<String>();
        long begin = Calendar.getInstance().getTime().getTime();
        boolean status;
        if (!(status = running)) resetDate = begin;

        YAMLSection prevconfig = config.get();
        config.reload();
        langconfig.reload();
        for (String key : langconfig.get().getSection("Lang").getKeys())
            api.setLang("SubServers", key, langconfig.get().getSection("Lang").getColoredString(key, '&'));

        if (subdata == null || // SubData Server must be reset
                !config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").equals(prevconfig.getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391")) ||
                !config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE").equals(prevconfig.getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE"))
                ) {
            if (subdata != null) subdata.destroy();

            Cipher cipher = null;
            if (!config.get().getSection("Settings").getSection("SubData").getRawString("Encryption", "NONE").equalsIgnoreCase("NONE")) {
                if (config.get().getSection("Settings").getSection("SubData").getString("Password", "").length() == 0) {
                    System.out.println("SubData > Cannot encrypt connection without a password");
                } else if (!SubDataServer.getCiphers().keySet().contains(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption").toUpperCase().replace('-', '_').replace(' ', '_'))) {
                    System.out.println("SubData > Unknown encryption type: " + config.get().getSection("Settings").getSection("SubData").getRawString("Encryption"));
                } else {
                    cipher = SubDataServer.getCipher(config.get().getSection("Settings").getSection("SubData").getRawString("Encryption"));
                }
            }
            subdata = new SubDataServer(this, Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[1]),
                    (config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0].equals("0.0.0.0"))?null:InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:4391").split(":")[0]),
                    cipher);
            System.out.println("SubServers > SubData Direct Listening on " + subdata.getServer().getLocalSocketAddress().toString());
            loop();
        } // Add new entries to Allowed-Connections
        for (String s : config.get().getSection("Settings").getSection("SubData").getStringList("Allowed-Connections", new ArrayList<String>())) {
            try {
                SubDataServer.allowConnection(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int proxies = 1;
        if (redis != null) {
            try {
                boolean first = true;
                String master = (String) redis("getServerId");
                if (!master.equals(redis.getName())) redis = new Proxy(master);
                if (!redis.getDisplayName().equals("(master)")) redis.setDisplayName("(master)");
                for (String name : (List<String>) redis("getAllServers")) {
                    if (!ukeys.contains(name.toLowerCase()) && !master.equals(name)) try {
                        if (first) System.out.println("SubServers > "+((status)?"Rel":"L")+"oading Proxies...");
                        first = false;
                        Proxy proxy = this.proxies.get(name.toLowerCase());
                        if (proxy == null) {
                            proxy = new Proxy(name);
                            getPluginManager().callEvent(new SubAddProxyEvent(proxy));
                            this.proxies.put(name.toLowerCase(), proxy);
                        }
                        ukeys.add(name.toLowerCase());
                        proxies++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ukeys.clear();

        int hosts = 0;
        System.out.println("SubServers > "+((status)?"Rel":"L")+"oading Hosts...");
        for (String name : config.get().getSection("Hosts").getKeys()) {
            if (!ukeys.contains(name.toLowerCase())) try {
                if (!hostDrivers.keySet().contains(config.get().getSection("Hosts").getSection(name).getRawString("Driver").toUpperCase().replace('-', '_').replace(' ', '_'))) throw new InvalidHostException("Invalid Driver for host: " + name);
                Host host = this.hosts.get(name.toLowerCase());
                if (host == null || // Host must be reset
                        !hostDrivers.get(config.get().getSection("Hosts").getSection(name).getRawString("Driver").toUpperCase().replace('-', '_').replace(' ', '_')).equals(host.getClass()) ||
                        !config.get().getSection("Hosts").getSection(name).getRawString("Address").equals(host.getAddress().getHostAddress()) ||
                        !config.get().getSection("Hosts").getSection(name).getRawString("Directory").equals(host.getPath()) ||
                        !config.get().getSection("Hosts").getSection(name).getRawString("Port-Range", "25500-25559").equals(prevconfig.getSection("Hosts", new YAMLSection()).getSection(name, new YAMLSection()).getRawString("Port-Range", "25500-25559")) ||
                        !config.get().getSection("Hosts").getSection(name).getRawString("Git-Bash").equals(host.getCreator().getBashDirectory())
                        ) {
                    if (host != null) api.forceRemoveHost(name);
                    host = api.addHost(config.get().getSection("Hosts").getSection(name).getRawString("Driver").toLowerCase(), name, config.get().getSection("Hosts").getSection(name).getBoolean("Enabled"), InetAddress.getByName(config.get().getSection("Hosts").getSection(name).getRawString("Address")), config.get().getSection("Hosts").getSection(name).getRawString("Directory"),
                            new NamedContainer<>(Integer.parseInt(config.get().getSection("Hosts").getSection(name).getRawString("Port-Range", "25500-25559").split("-")[0]), Integer.parseInt(config.get().getSection("Hosts").getSection(name).getRawString("Port-Range", "25500-25559").split("-")[1])), config.get().getSection("Hosts").getSection(name).getRawString("Git-Bash"));
                } else { // Host wasn't reset, so check for these changes
                    if (config.get().getSection("Hosts").getSection(name).getBoolean("Enabled") != host.isEnabled())
                        host.setEnabled(config.get().getSection("Hosts").getSection(name).getBoolean("Enabled"));
                } // Check for other changes
                if (config.get().getSection("Hosts").getSection(name).getKeys().contains("Display") && ((config.get().getSection("Hosts").getSection(name).getString("Display").length() == 0 && !host.getDisplayName().equals(host.getName())) || !config.get().getSection("Hosts").getSection(name).getString("Display").equals(host.getDisplayName())))
                    host.setDisplayName(config.get().getSection("Hosts").getSection(name).getString("Display"));
                if (config.get().getSection("Hosts").getSection(name).getKeys().contains("Extra"))
                    for (String extra : config.get().getSection("Hosts").getSection(name).getSection("Extra").getKeys()) host.addExtra(extra, config.get().getSection("Hosts").getSection(name).getSection("Extra").getObject(extra));
                ukeys.add(name.toLowerCase());
                hosts++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ukeys.clear();

        int servers = 0;
        System.out.println("SubServers > "+((status)?"Rel":"L")+"oading Servers...");
        bungeeconfig.reload();
        for (String name : bungeeconfig.get().getSection("servers").getKeys()) {
            if (!ukeys.contains(name.toLowerCase())) try {
                Server server = api.getServer(name);
                if (server == null || !(server instanceof SubServer)) {
                    if (server == null || // Server must be reset
                            bungeeconfig.get().getSection("servers").getSection(name).getRawString("address").equals(server.getAddress().getAddress().getHostAddress() + ':' + server.getAddress().getPort())
                    ) {
                        if (server != null) api.forceRemoveServer(name);
                        server = api.addServer(name, InetAddress.getByName(bungeeconfig.get().getSection("servers").getSection(name).getRawString("address").split(":")[0]),
                                Integer.parseInt(bungeeconfig.get().getSection("servers").getSection(name).getRawString("address").split(":")[1]), bungeeconfig.get().getSection("servers").getSection(name).getColoredString("motd", '&'),
                                bungeeconfig.get().getSection("servers").getSection(name).getBoolean("hidden", false), bungeeconfig.get().getSection("servers").getSection(name).getBoolean("restricted"));
                    } else { // Server wasn't reset, so check for these changes
                        if (!bungeeconfig.get().getSection("servers").getSection(name).getColoredString("motd", '&').equals(server.getMotd()))
                            server.setMotd(bungeeconfig.get().getSection("servers").getSection(name).getColoredString("motd", '&'));
                        if (bungeeconfig.get().getSection("servers").getSection(name).getBoolean("hidden", false) != server.isHidden())
                            server.setHidden(bungeeconfig.get().getSection("servers").getSection(name).getBoolean("hidden", false));
                        if (bungeeconfig.get().getSection("servers").getSection(name).getBoolean("restricted") != server.isRestricted())
                            server.setRestricted(bungeeconfig.get().getSection("servers").getSection(name).getBoolean("restricted"));
                    } // Check for other changes
                    if (bungeeconfig.get().getSection("servers").getSection(name).getKeys().contains("display") && ((bungeeconfig.get().getSection("servers").getSection(name).getRawString("display").length() == 0 && !server.getDisplayName().equals(server.getName())) || !bungeeconfig.get().getSection("servers").getSection(name).getRawString("display").equals(server.getDisplayName())))
                        server.setDisplayName(bungeeconfig.get().getSection("servers").getSection(name).getString("display"));
                    if (bungeeconfig.get().getSection("servers").getSection(name).getKeys().contains("group")) {
                        for (String group : server.getGroups()) server.removeGroup(group);
                        for (String group : bungeeconfig.get().getSection("servers").getSection(name).getStringList("group")) server.addGroup(group);
                    }
                    if (bungeeconfig.get().getSection("servers").getSection(name).getKeys().contains("extra"))
                        for (String extra : config.get().getSection("servers").getSection(name).getSection("extra").getKeys()) server.addExtra(extra, config.get().getSection("servers").getSection(name).getSection("extra").getObject(extra));
                    if (server.getSubData() != null)
                        server.getSubData().sendPacket(new PacketOutReload(null));
                    ukeys.add(name.toLowerCase());
                    servers++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ukeys.clear();

        int subservers = 0;
        System.out.println("SubServers > "+((status)?"Rel":"L")+"oading SubServers...");
        if (!posted) Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (running) {
                System.out.println("SubServers > Received request from system to shutdown");
                try {
                    shutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }));
        running = true;
        List<String> autorun = new LinkedList<String>();
        for (String name : config.get().getSection("Servers").getKeys()) {
            if (!ukeys.contains(name.toLowerCase())) try {
                if (!this.hosts.keySet().contains(config.get().getSection("Servers").getSection(name).getString("Host").toLowerCase())) throw new InvalidServerException("There is no host with this name: " + config.get().getSection("Servers").getSection(name).getString("Host"));
                if (exServers.keySet().contains(name.toLowerCase())) {
                    exServers.remove(name.toLowerCase());
                    servers--;
                }
                SubServer server = api.getSubServer(name);
                if (server != null && server.isEditable()) { // Server can edit() (May be reset depending on change severity)
                    YAMLSection edits = new YAMLSection();
                    if (config.get().getSection("Servers").getSection(name).getBoolean("Enabled") != server.isEnabled())
                        edits.set("enabled", config.get().getSection("Servers").getSection(name).getBoolean("Enabled"));
                    if (config.get().getSection("Servers").getSection(name).getKeys().contains("Display") && ((config.get().getSection("Servers").getSection(name).getRawString("Display").length() == 0 && !server.getDisplayName().equals(server.getName())) || !config.get().getSection("Servers").getSection(name).getRawString("Display").equals(server.getDisplayName())))
                        edits.set("display", config.get().getSection("Servers").getSection(name).getRawString("Display"));
                    if (!config.get().getSection("Servers").getSection(name).getString("Host").equalsIgnoreCase(server.getHost().getName()))
                        edits.set("host", config.get().getSection("Servers").getSection(name).getRawString("Host"));
                    if (!config.get().getSection("Servers").getSection(name).getStringList("Group").equals(server.getGroups()))
                        edits.set("group", config.get().getSection("Servers").getSection(name).getRawStringList("Group"));
                    if (config.get().getSection("Servers").getSection(name).getInt("Port") != server.getAddress().getPort())
                        edits.set("port", config.get().getSection("Servers").getSection(name).getInt("Port"));
                    if (!config.get().getSection("Servers").getSection(name).getColoredString("Motd", '&').equals(server.getMotd()))
                        edits.set("motd", config.get().getSection("Servers").getSection(name).getRawString("Motd"));
                    if (config.get().getSection("Servers").getSection(name).getBoolean("Log") != server.isLogging())
                        edits.set("log", config.get().getSection("Servers").getSection(name).getBoolean("Log"));
                    if (!config.get().getSection("Servers").getSection(name).getRawString("Directory").equals(server.getPath()))
                        edits.set("dir", config.get().getSection("Servers").getSection(name).getRawString("Directory"));
                    if (!new Executable(config.get().getSection("Servers").getSection(name).getRawString("Executable")).toString().equals(server.getExecutable().toString()))
                        edits.set("exec", config.get().getSection("Servers").getSection(name).getRawString("Executable"));
                    if (!config.get().getSection("Servers").getSection(name).getRawString("Stop-Command").equals(server.getStopCommand()))
                        edits.set("stop-cmd", config.get().getSection("Servers").getSection(name).getRawString("Stop-Command"));
                    SubServer.StopAction action = Util.getDespiteException(() -> SubServer.StopAction.valueOf(config.get().getSection("Servers").getSection(name).getRawString("Stop-Action", "NONE").toUpperCase().replace('-', '_').replace(' ', '_')), null);
                    if (action != null && action != server.getStopAction())
                        edits.set("stop-action", action.toString());
                    if (config.get().getSection("Servers").getSection(name).getBoolean("Restricted") != server.isRestricted())
                        edits.set("restricted", config.get().getSection("Servers").getSection(name).getBoolean("Restricted"));
                    if (config.get().getSection("Servers").getSection(name).getBoolean("Hidden") != server.isHidden())
                        edits.set("hidden", config.get().getSection("Servers").getSection(name).getBoolean("Hidden"));


                    if (edits.getKeys().size() > 0) {
                        server.edit(edits);
                        server = api.getSubServer(name);
                    }
                } else { // Server cannot edit()
                    if (server == null ||  // Server must be reset
                            !config.get().getSection("Servers").getSection(name).getString("Host").equalsIgnoreCase(server.getHost().getName()) ||
                            config.get().getSection("Servers").getSection(name).getInt("Port") != server.getAddress().getPort() ||
                            !config.get().getSection("Servers").getSection(name).getRawString("Directory").equals(server.getPath()) ||
                            !new Executable(config.get().getSection("Servers").getSection(name).getRawString("Executable")).toString().equals(server.getExecutable().toString())
                            ) {
                            if (server != null) server.getHost().forceRemoveSubServer(name);
                            server = this.hosts.get(config.get().getSection("Servers").getSection(name).getString("Host").toLowerCase()).addSubServer(name, config.get().getSection("Servers").getSection(name).getBoolean("Enabled"),
                                    config.get().getSection("Servers").getSection(name).getInt("Port"), config.get().getSection("Servers").getSection(name).getColoredString("Motd", '&'), config.get().getSection("Servers").getSection(name).getBoolean("Log"),
                                    config.get().getSection("Servers").getSection(name).getRawString("Directory"), new Executable(config.get().getSection("Servers").getSection(name).getRawString("Executable")), config.get().getSection("Servers").getSection(name).getRawString("Stop-Command"),
                                    config.get().getSection("Servers").getSection(name).getBoolean("Hidden"), config.get().getSection("Servers").getSection(name).getBoolean("Restricted"));
                    } else { // Server doesn't need to reset
                        if (config.get().getSection("Servers").getSection(name).getBoolean("Enabled") != server.isEnabled())
                            server.setEnabled(config.get().getSection("Servers").getSection(name).getBoolean("Enabled"));
                        if (!config.get().getSection("Servers").getSection(name).getColoredString("Motd", '&').equals(server.getMotd()))
                            server.setMotd(config.get().getSection("Servers").getSection(name).getColoredString("Motd", '&'));
                        if (config.get().getSection("Servers").getSection(name).getBoolean("Log") != server.isLogging())
                            server.setLogging(config.get().getSection("Servers").getSection(name).getBoolean("Log"));
                        if (!config.get().getSection("Servers").getSection(name).getRawString("Stop-Command").equals(server.getStopCommand()))
                            server.setStopCommand(config.get().getSection("Servers").getSection(name).getRawString("Stop-Command"));
                        if (config.get().getSection("Servers").getSection(name).getBoolean("Restricted") != server.isRestricted())
                            server.setRestricted(config.get().getSection("Servers").getSection(name).getBoolean("Restricted"));
                        if (config.get().getSection("Servers").getSection(name).getBoolean("Hidden") != server.isHidden())
                            server.setHidden(config.get().getSection("Servers").getSection(name).getBoolean("Hidden"));
                    } // Apply these changes regardless of reset
                    SubServer.StopAction action = Util.getDespiteException(() -> SubServer.StopAction.valueOf(config.get().getSection("Servers").getSection(name).getRawString("Stop-Action", "NONE").toUpperCase().replace('-', '_').replace(' ', '_')), null);
                    if (action != null && action != server.getStopAction())
                        server.setStopAction(action);
                    if (!status && config.get().getSection("Servers").getSection(name).getBoolean("Run-On-Launch"))
                        autorun.add(name.toLowerCase());
                    if (config.get().getSection("Servers").getSection(name).getKeys().contains("Display") && ((config.get().getSection("Servers").getSection(name).getRawString("Display").length() == 0 && !server.getDisplayName().equals(server.getName())) || !config.get().getSection("Servers").getSection(name).getRawString("Display").equals(server.getDisplayName())))
                        server.setDisplayName(config.get().getSection("Servers").getSection(name).getRawString("Display"));
                    if (config.get().getSection("Servers").getSection(name).getKeys().contains("Group")) {
                        for (String group : server.getGroups()) server.removeGroup(group);
                        for (String group : config.get().getSection("Servers").getSection(name).getStringList("Group")) server.addGroup(group);
                    }
                } // Apply these changes regardless of edit/reset
                if (config.get().getSection("Servers").getSection(name).getKeys().contains("Extra")) for (String extra : config.get().getSection("Servers").getSection(name).getSection("Extra").getKeys()) server.addExtra(extra, config.get().getSection("Servers").getSection(name).getSection("Extra").getObject(extra));
                ukeys.add(name.toLowerCase());
                subservers++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (String name : ukeys) {
            SubServer server = api.getSubServer(name);
            for (String oname : config.get().getSection("Servers").getSection(server.getName()).getRawStringList("Incompatible", new ArrayList<>())) {
                SubServer oserver = api.getSubServer(oname);
                if (oserver != null && server.isCompatible(oserver)) server.toggleCompatibility(oserver);
            }
            if (autorun.contains(name))
                server.start();
        }
        ukeys.clear();
        api.ready = true;
        legServers.clear();

        int plugins = 0;
        List<?> listeners = (status)?api.reloadListeners:api.listeners;
        if (listeners.size() > 0) {
            System.out.println("SubServers > "+((status)?"Rel":"L")+"oading SubAPI Plugins...");
            for (Object obj : listeners) {
                if (status) {
                    try {
                        ((Runnable) obj).run();
                        plugins++;
                    } catch (Throwable e) {
                        new InvocationTargetException(e, "Problem enabling plugin").printStackTrace();
                    }
                } else {
                    NamedContainer<Runnable, Runnable> listener = (NamedContainer<Runnable, Runnable>) obj;
                    try {
                        if (listener.name() != null) {
                            listener.name().run();
                            plugins++;
                        }
                    } catch (Throwable e) {
                        new InvocationTargetException(e, "Problem enabling plugin").printStackTrace();
                    }
                }
            }
        }

        if (status) {
            for (Host host : api.getHosts().values()) if (host instanceof ClientHandler && ((ClientHandler) host).getSubData() != null) ((ClientHandler) host).getSubData().sendPacket(new PacketOutReload(null));
            for (Server server : api.getServers().values()) if (server.getSubData() != null) server.getSubData().sendPacket(new PacketOutReload(null));
        }

        System.out.println("SubServers > " + ((plugins > 0)?plugins+" Plugin"+((plugins == 1)?"":"s")+", ":"") + ((proxies > 1)?proxies+" Proxies, ":"") + hosts + " Host"+((hosts == 1)?"":"s")+", " + servers + " Server"+((servers == 1)?"":"s")+", and " + subservers + " SubServer"+((subservers == 1)?"":"s")+" "+((status)?"re":"")+"loaded in " + new DecimalFormat("0.000").format((Calendar.getInstance().getTime().getTime() - begin) / 1000D) + "s");
    }

    private void post() {
        if (config.get().getSection("Settings").getBoolean("Override-Bungee-Commands", true)) {
            getPluginManager().registerCommand(null, SubCommand.BungeeServer.newInstance(this, "server").get());
            getPluginManager().registerCommand(null, new SubCommand.BungeeList(this, "glist"));
        }
        getPluginManager().registerCommand(null, SubCommand.newInstance(this, "subservers").get());
        getPluginManager().registerCommand(null, SubCommand.newInstance(this, "subserver").get());
        getPluginManager().registerCommand(null, SubCommand.newInstance(this, "sub").get());

        new Metrics(this);
        new Timer().schedule(new TimerTask() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                try {
                    YAMLSection tags = new YAMLSection(new Gson().fromJson("{\"tags\":" + Util.readAll(new BufferedReader(new InputStreamReader(new URL("https://api.github.com/repos/ME1312/SubServers-2/git/refs/tags").openStream(), Charset.forName("UTF-8")))) + '}', Map.class));
                    List<Version> versions = new LinkedList<Version>();

                    Version updversion = version;
                    int updcount = 0;
                    for (YAMLSection tag : tags.getSectionList("tags")) versions.add(Version.fromString(tag.getString("ref").substring(10)));
                    Collections.sort(versions);
                    for (Version version : versions) {
                        if (version.compareTo(updversion) > 0) {
                            updversion = version;
                            updcount++;
                        }
                    }
                    if (updcount > 0) System.out.println("SubServers > SubServers.Bungee v" + updversion + " is available. You are " + updcount + " version" + ((updcount == 1)?"":"s") + " behind.");
                } catch (Exception e) {}
            }
        }, 0, TimeUnit.DAYS.toMillis(2));
    }

    private void loop() {
        new Thread(() -> {
            while (subdata != null) {
                try {
                    subdata.addClient(subdata.getServer().accept());
                } catch (IOException e) {
                    if (!(e instanceof SocketException)) e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Reset all changes made by startListeners
     *
     * @see SubPlugin#startListeners()
     */
    @Override
    public void stopListeners() {
        try {
            legServers.clear();
            legServers.putAll(getServers());
            if (api.listeners.size() > 0) {
                System.out.println("SubServers > Resetting SubAPI Plugins...");
                for (NamedContainer<Runnable, Runnable> listener : api.listeners) {
                    try {
                        if (listener.get() != null) listener.get().run();
                    } catch (Throwable e) {
                        new InvocationTargetException(e, "Problem disabling plugin").printStackTrace();
                    }
                }
            }

            shutdown();

            subdata.destroy();

            for (ListenerInfo listener : getConfig().getListeners()) {
                if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(listener.getHost().getPort())) UPnP.closePortTCP(listener.getHost().getPort());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.stopListeners();
    } private void shutdown() throws Exception {
        api.ready = false;
        System.out.println("SubServers > Resetting Hosts and Server Data");
        List<String> hosts = new ArrayList<String>();
        hosts.addAll(this.hosts.keySet());

        for (String host : hosts) {
            api.forceRemoveHost(host);
        }
        running = false;
        this.hosts.clear();
        exServers.clear();

        for (String proxy : proxies.keySet()) {
            getPluginManager().callEvent(new SubRemoveProxyEvent(proxies.get(proxy)));
        }
        proxies.clear();

        for (ListenerInfo listener : getConfig().getListeners()) {
            if (UPnP.isUPnPAvailable() && UPnP.isMappedTCP(listener.getHost().getPort())) UPnP.closePortTCP(listener.getHost().getPort());
        }
    }

    String getNewSignature() {
        BigInteger number = (lastSignature = lastSignature.add(BigInteger.ONE));
        final String DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";
        final BigInteger BASE = BigInteger.valueOf(DIGITS.length());

        StringBuilder result = new StringBuilder();
        while (number.compareTo(BigInteger.ZERO) > 0) { // number > 0
            BigInteger[] divmod = number.divideAndRemainder(BASE);
            number = divmod[0];
            int digit = divmod[1].intValue();
            result.insert(0, DIGITS.charAt(digit));
        }
        return (result.length() == 0) ? DIGITS.substring(0, 1) : result.toString();
    }

    /**
     * Reference a RedisBungee method via reflection
     *
     * @param method Method to reference
     * @param args Method arguments
     * @return Method Response
     */
    @SuppressWarnings("unchecked")
    public Object redis(String method, NamedContainer<Class<?>, ?>... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (getPluginManager().getPlugin("RedisBungee") != null) {
            Object api = getPluginManager().getPlugin("RedisBungee").getClass().getMethod("getApi").invoke(null);
            Class<?>[] classargs = new Class<?>[args.length];
            Object[] objargs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                classargs[i] = args[i].name();
                objargs[i] = args[i].get();
                if (!classargs[i].isInstance(objargs[i])) throw new ClassCastException(classargs[i].getCanonicalName() + " != " + objargs[i].getClass().getCanonicalName());
            }
            return api.getClass().getMethod(method, classargs).invoke(api, objargs);
        } else {
            throw new IllegalStateException("RedisBungee is not installed");
        }
    }

    /**
     * Further override BungeeCord's signature when patched into the same jar
     *
     * @return Software Name
     */
    @Override
    public String getName() {
        return (isPatched)?"SubServers Platform":super.getName();
    }

    /**
     * Get the name from BungeeCord's original signature (for determining which fork is being used)
     *
     * @return BungeeCord Software Name
     */
    public String getBungeeName() {
        return super.getName();
    }

    /**
     * Emulate BungeeCord's getServers()
     *
     * @see SubAPI#getServers()
     * @return Server Map
     */
    @Override
    public Map<String, ServerInfo> getServers() {
        HashMap<String, ServerInfo> servers = new HashMap<String, ServerInfo>();
        if (!api.ready) {
            servers.putAll(super.getServers());
            servers.putAll(legServers);
        } else {
            for (ServerInfo server : exServers.values()) servers.put(server.getName(), server);
            for (Host host : this.hosts.values()) {
                for (ServerInfo server : host.getSubServers().values()) servers.put(server.getName(), server);
            }
        }
        return servers;
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    public void reroute(ServerConnectEvent e) {
        Map<String, ServerInfo> servers = new TreeMap<String, ServerInfo>(api.getServers());
        if (servers.keySet().contains(e.getTarget().getName().toLowerCase()) && e.getTarget() != servers.get(e.getTarget().getName().toLowerCase())) {
            e.setTarget(servers.get(e.getTarget().getName().toLowerCase()));
        } else {
            servers = getServers();
            if (servers.keySet().contains(e.getTarget().getName()) && e.getTarget() != servers.get(e.getTarget().getName())) {
                e.setTarget(servers.get(e.getTarget().getName()));
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = Byte.MIN_VALUE)
    public void fallback(ServerKickEvent e) {
        if (e.getPlayer().getPendingConnection().getListener().isForceDefault()) {
            NamedContainer<Integer, ServerInfo> next = null;
            for (String name : e.getPlayer().getPendingConnection().getListener().getServerPriority()) {
                if (!e.getKickedFrom().getName().equalsIgnoreCase(name)) {
                    ServerInfo server = getServerInfo(name);
                    if (server != null) {
                        int confidence = 0;
                        if (server instanceof Server) {
                            if (!((Server) server).isHidden()) confidence++;
                            if (!((Server) server).isRestricted()) confidence++;
                            if (((Server) server).getSubData() != null) confidence++;

                            if (server instanceof SubServer) {
                                if (((SubServer) server).isRunning()) confidence++;
                            } else confidence++;
                        }

                        if (next == null || confidence > next.name())
                            next = new NamedContainer<Integer, ServerInfo>(confidence, server);
                    }
                }
            }

            if (next != null) {
                e.setCancelServer(next.get());
                e.setCancelled(true);
                e.getPlayer().sendMessage(api.getLang("SubServers", "Bungee.Feature.Return").replace("$str$", (next.get() instanceof Server)?((Server) next.get()).getDisplayName():next.get().getName()).replace("$msg$", e.getKickReason()));
            }
        }
    }

    @EventHandler(priority = Byte.MIN_VALUE)
    public void resetSudo(SubStoppedEvent e) {
        if (sudo == e.getServer()) {
            sudo = null;
            System.out.println("SubServers > Reverting to the BungeeCord Console");
        }
    }
}
