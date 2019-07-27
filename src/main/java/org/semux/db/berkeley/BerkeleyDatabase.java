package org.semux.db.berkeley;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.tuple.Pair;
import org.semux.db.Database;
import org.semux.util.ClosableIterator;
import org.semux.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

public class BerkeleyDatabase implements Database {

    private static final Logger logger = LoggerFactory.getLogger(BerkeleyDatabase.class);

    private final File file;
    private final com.sleepycat.je.Database database;
    private final Environment environment;

    public BerkeleyDatabase(File file) {
        this.file = file;

        if (!file.exists() && !file.mkdirs()) {
            logger.error("Failed to create directory: {}", file);
        }

        // Open the environment. Create it if it does not already exist.
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        try {
            environment = new Environment(file, envConfig);

            // Open the database. Create it if it does not already exist.
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setAllowCreate(true);
            dbConfig.setTransactional(true);
            database = environment.openDatabase(null, file.getName(), dbConfig);
        } catch (DatabaseException e) {
            throw new org.semux.db.exception.DatabaseException(e);
        }
    }

    @Override
    public byte[] get(byte[] key) {

        DatabaseEntry theKey = new DatabaseEntry(key);
        DatabaseEntry theData = new DatabaseEntry();

        try {
            if (database.get(null, theKey, theData, LockMode.DEFAULT)
                    == OperationStatus.SUCCESS) {
                return theData.getData();
            }
        } catch (DatabaseException e) {
            throw new org.semux.db.exception.DatabaseException(e);
        }
        return null;
    }

    @Override
    public void put(byte[] key, byte[] value) {
        DatabaseEntry theKey = new DatabaseEntry(key);
        DatabaseEntry theData = new DatabaseEntry(value);
        try {
            database.put(null, theKey, theData);
        } catch (DatabaseException e) {
            throw new org.semux.db.exception.DatabaseException(e);
        }
    }

    @Override
    public void delete(byte[] key) {
        try {
            database.delete(null, new DatabaseEntry(key));
        } catch (DatabaseException e) {
            throw new org.semux.db.exception.DatabaseException(e);
        }
    }

    @Override
    public void updateBatch(List<Pair<byte[], byte[]>> pairs) {
        Transaction tx = null;
        try {

            tx = environment.beginTransaction(null, null);
            for (Pair<byte[], byte[]> pair : pairs) {
                if (pair.getRight() == null) {
                    database.delete(tx, new DatabaseEntry(pair.getLeft()));
                } else {
                    database.put(tx, new DatabaseEntry(pair.getLeft()), new DatabaseEntry(pair.getRight()));
                }
            }
            tx.commit();
        } catch (DatabaseException e) {
            if (tx != null) {
                try {
                    tx.abort();
                } catch (DatabaseException e1) {
                    throw new org.semux.db.exception.DatabaseException(e1);
                }
            }
            throw new org.semux.db.exception.DatabaseException(e);
        }
    }

    @Override
    public ClosableIterator<Map.Entry<byte[], byte[]>> iterator() {
        return iterator(null);
    }

    @Override
    public ClosableIterator<Map.Entry<byte[], byte[]>> iterator(byte[] prefix) {
        try {
            return new ClosableIterator<Map.Entry<byte[], byte[]>>() {
                final Cursor itr = database.openCursor(null, null);

                // Cursors need a pair of DatabaseEntry objects to operate. These hold
                // the key and data found at any given position in the database.
                DatabaseEntry foundKey = new DatabaseEntry();
                DatabaseEntry foundData = new DatabaseEntry();
                OperationStatus lastStatus = OperationStatus.SUCCESS;

                private ClosableIterator<Map.Entry<byte[], byte[]>> initialize() {
                    try {
                        if (prefix != null) {
                            foundKey.setData(prefix);
                            itr.getSearchKeyRange(foundKey, foundData, LockMode.DEFAULT);
                        } else {
                            lastStatus = itr.getNext(foundKey, foundData, LockMode.DEFAULT);
                        }
                    } catch (DatabaseException e) {
                        throw new org.semux.db.exception.DatabaseException(e);
                    }
                    return this;
                }

                @Override
                public boolean hasNext() {
                    return lastStatus == OperationStatus.SUCCESS;
                }

                @Override
                public Map.Entry<byte[], byte[]> next() {
                    Map.Entry<byte[], byte[]> ret = new DefaultMapEntry(foundKey.getData(), foundData.getData());
                    //populate next
                    try {
                        lastStatus = itr.getNext(foundKey, foundData, LockMode.DEFAULT);
                    } catch (DatabaseException e) {
                        throw new org.semux.db.exception.DatabaseException(e);
                    }
                    return ret;
                }

                @Override
                public void close() {
                    try {
                        itr.close();
                    } catch (DatabaseException e) {
                        throw new org.semux.db.exception.DatabaseException(e);
                    }
                }
            }.initialize();
        } catch (DatabaseException e) {
            throw new org.semux.db.exception.DatabaseException(e);
        }
    }

    @Override
    public void close() {
        try {
            database.close();
        } catch (DatabaseException e) {
            logger.error("Unable to close db", e);
        }
    }

    @Override
    public void destroy() {
        close();
        FileUtil.recursiveDelete(file);
    }

    @Override
    public Path getDataDir() {
        return file.toPath();
    }
}
