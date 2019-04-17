/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.semux.util.SystemUtil.OsName.LINUX;
import static org.semux.util.SystemUtil.OsName.MACOS;
import static org.semux.util.SystemUtil.OsName.WINDOWS;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;

import org.junit.Test;
import org.semux.util.exception.UnreachableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemUtilTest {

    private Logger logger = LoggerFactory.getLogger(SystemUtilTest.class);

    @Test
    public void testCompareVersion() {
        assertEquals(0, SystemUtil.compareVersion("1.0.0", "1.0.0"));
        assertEquals(1, SystemUtil.compareVersion("1.0.0", "1.0.0-alpha"));
        assertEquals(1, SystemUtil.compareVersion("2.0.1", "1.0.2"));
        assertEquals(-1, SystemUtil.compareVersion("2.0.1-beta", "2.0.1-beta.1"));
    }

    @Test
    public void testGetIp() {
        Instant begin = Instant.now();
        String ip = SystemUtil.getIp();
        logger.info("IP address = {}, took {} ms", ip, Duration.between(begin, Instant.now()).toMillis());

        assertNotEquals("127.0.0.1", ip);
    }

    @Test
    public void testGetAvailableMemorySize() {
        long size = SystemUtil.getAvailableMemorySize();
        logger.info("Available memory size = {} MB", size / 1024L / 1024L);

        assertTrue(size > 0);
        assertTrue(size < 64L * 1024L * 1024L * 1024L);
        assertTrue(size != 0xffffffffL);
    }

    @Test
    public void testGetTotalMemorySize() {
        long size = SystemUtil.getTotalMemorySize();
        logger.info("Total memory size = {} MB", size / 1024L / 1024L);

        assertTrue(size > 0);
        assertTrue(size < 64L * 1024L * 1024L * 1024L);
        assertTrue(size != 0xffffffffL);
    }

    @Test
    public void testGetUsedHeapSize() {
        long size = SystemUtil.getUsedHeapSize();
        logger.info("Used heap size = {} MB", size / 1024L / 1024L);

        assertTrue(size > 0);
        assertTrue(size < 4L * 1024L * 1024L * 1024L);
    }

    @Test
    public void testBench() {
        logger.info("System benchmark result = {}", SystemUtil.bench());
    }

    @Test
    public void testIsWindowsVCRedist2012Installed() {
        assumeTrue(SystemUtil.getOsName() == WINDOWS);
        assertTrue(SystemUtil.isWindowsVCRedist2012Installed());
    }

    @Test
    public void testIsJavaPlatformModuleSystemAvailable() {
        switch (ManagementFactory.getRuntimeMXBean().getSpecVersion()) {
        case "1.8":
            assertFalse(SystemUtil.isJavaPlatformModuleSystemAvailable());
            break;
        case "9":
        case "10":
        case "11":
            assertTrue(SystemUtil.isJavaPlatformModuleSystemAvailable());
            break;
        default:
            throw new UnreachableException();
        }
    }

    @Test
    public void testIsPosixTrue() {
        SystemUtil.OsName os = SystemUtil.getOsName();
        assumeTrue(os.equals(LINUX) || os.equals(MACOS));
        assertTrue(SystemUtil.isPosix());
    }

    @Test
    public void testIsPosixFalse() {
        SystemUtil.OsName os = SystemUtil.getOsName();
        assumeTrue(os.equals(WINDOWS));
        assertFalse(SystemUtil.isPosix());
    }
}
