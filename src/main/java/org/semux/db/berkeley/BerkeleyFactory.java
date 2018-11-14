package org.semux.db.berkeley;

import org.semux.db.Database;
import org.semux.db.DatabaseFactory;
import org.semux.db.DatabaseName;

import java.io.File;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class BerkeleyFactory implements DatabaseFactory {

    private final EnumMap<DatabaseName, Database> databases = new EnumMap<>(DatabaseName.class);

    private final File dataDir;
    private final AtomicBoolean open;

    public BerkeleyFactory(File dataDir) {
        this.dataDir = dataDir;
        this.open = new AtomicBoolean(false);

        open();
    }

    @Override
    public void open() {
        if (open.compareAndSet(false, true)) {
            for (DatabaseName name : DatabaseName.values()) {
                File file = new File(dataDir.getAbsolutePath() + "/database/berk", name.toString().toLowerCase(Locale.ROOT));
                databases.put(name, new BerkeleyDatabase(file));
            }
        }
    }

    @Override
    public Database getDB(DatabaseName name) {
        open();
        return databases.get(name);
    }

    @Override
    public void close() {
        if (open.compareAndSet(true, false)) {
            for (Database db : databases.values()) {
                db.close();
            }
        }
    }

    @Override
    public Path getDataDir() {
        return dataDir.toPath();
    }
}
