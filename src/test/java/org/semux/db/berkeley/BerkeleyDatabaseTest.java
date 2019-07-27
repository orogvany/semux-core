package org.semux.db.berkeley;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.semux.config.Constants;
import org.semux.util.Bytes;
import org.semux.util.ClosableIterator;

public class BerkeleyDatabaseTest {
    private byte[] key = Bytes.of("key");
    private byte[] value = Bytes.of("value");

    public BerkeleyDatabase openDatabase() {
        return new BerkeleyDatabase(
                new File(Constants.DEFAULT_DATA_DIR, Constants.DATABASE_DIR + File.separator + "test"));
    }

    @Test
    public void testRecover() {
//        BerkeleyDatabase db = openDatabase();
//        try {
//            db.recover(db.createOptions());
//        } finally {
//            db.destroy();
//        }
    }

    @Test
    public void testGetAndPut() {
        BerkeleyDatabase db = openDatabase();
        try {
            assertNull(db.get(key));
            db.put(key, value);
            assertTrue(Arrays.equals(value, db.get(key)));
        } finally {
            db.destroy();
        }
    }

    @Test
    public void testUpdateBatch() {
        BerkeleyDatabase db = openDatabase();
        try {
            db.put(Bytes.of("a"), Bytes.of("1"));

            List<Pair<byte[], byte[]>> update = new ArrayList<>();
            update.add(Pair.of(Bytes.of("a"), null));
            update.add(Pair.of(Bytes.of("b"), Bytes.of("2")));
            update.add(Pair.of(Bytes.of("c"), Bytes.of("3")));
            db.updateBatch(update);

            assertNull(db.get(Bytes.of("a")));
            assertArrayEquals(db.get(Bytes.of("b")), Bytes.of("2"));
            assertArrayEquals(db.get(Bytes.of("c")), Bytes.of("3"));
        } finally {
            db.destroy();
        }
    }

    @Test
    public void testIterator() {
        BerkeleyDatabase db = openDatabase();
        try {
            db.put(Bytes.of("a"), Bytes.of("1"));
            db.put(Bytes.of("b"), Bytes.of("2"));
            db.put(Bytes.of("c"), Bytes.of("3"));

            ClosableIterator<Map.Entry<byte[], byte[]>> itr = db.iterator(Bytes.of("a1"));
            assertTrue(itr.hasNext());
            assertArrayEquals(Bytes.of("b"), itr.next().getKey());
            assertTrue(itr.hasNext());
            assertArrayEquals(Bytes.of("c"), itr.next().getKey());
            itr.close();
        } finally {
            db.destroy();
        }
    }

    @Test
    public void testLevelDBFactory() {
//        BerkeleyDatabase.LeveldbFactory factory = new BerkeleyDatabase.LeveldbFactory(new File(Constants.DEFAULT_DATA_DIR, Constants.DATABASE_DIR));
//        for (DatabaseName name : DatabaseName.values()) {
//            assertNotNull(factory.getDB(name));
//        }
//        factory.close();

        // NOTE: empty databases are created
    }

    @Test
    public void testClose() {
        BerkeleyDatabase db = openDatabase();
        try {
            db.close();
        } finally {
            db.destroy();
        }
    }

    @Test
    public void testDestroy() {
        BerkeleyDatabase db = openDatabase();
        db.destroy();

        assertFalse(db.getDataDir().toFile().exists());
    }
}
