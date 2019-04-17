/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api.v2;

import java.util.Collections;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.junit.Before;
import org.semux.api.ApiVersion;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public abstract class SemuxApiTestBase extends org.semux.api.SemuxApiTestBase {

    protected org.semux.api.v2.client.SemuxApi api;

    @Before
    public void setUp() {
        super.setUp();

        api = JAXRSClientFactory.create(
                "http://localhost:51710/" + ApiVersion.DEFAULT.prefix,
                org.semux.api.v2.client.SemuxApi.class,
                Collections.singletonList(new JacksonJsonProvider()),
                config.apiUsername(),
                config.apiPassword(),
                null);
    }
}
