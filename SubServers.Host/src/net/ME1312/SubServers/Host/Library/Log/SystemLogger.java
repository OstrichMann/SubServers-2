package net.ME1312.SubServers.Host.Library.Log;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * System.out and System.err Override Class
 */
public final class SystemLogger extends OutputStream {
    private HashMap<String, Logger> stream = new HashMap<String, Logger>();
    private boolean level;
    private File dir;

    protected SystemLogger(boolean level, File dir) throws IOException {
        if (!new File(dir, SystemLogger.class.getCanonicalName().replace(".", File.separator) + ".class").exists()) {
            throw new IOException("Invalid directory for logging:" + dir.getPath());
        }
        this.level = level;
        this.dir = dir;
    }

    @Override
    public void write(int b) throws IOException {
        int i = 0;
        String origin = null;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (i > 1 && !element.getClassName().startsWith("net.ME1312.SubServers.Host.") && new File(dir, element.getClassName().replace(".", File.separator) + ".class").exists()) {
                origin = element.getClassName().replaceFirst("\\$.*", "");
                break;
            }
            i++;
        }
        if (origin == null) origin = java.lang.System.class.getCanonicalName();
        if (!stream.keySet().contains(origin)) stream.put(origin, new Logger(origin));
        if (level) {
            stream.get(origin).error.print((char) b);
        } else {
            stream.get(origin).info.print((char) b);
        }
    }
}
