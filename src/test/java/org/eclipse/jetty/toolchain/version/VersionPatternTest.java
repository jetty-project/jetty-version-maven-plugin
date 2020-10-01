/*
 *  ========================================================================
 *  Copyright (c) 1995-2016 Mort Bay Consulting Pty. Ltd.
 *  ------------------------------------------------------------------------
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *      The Eclipse Public License is available at
 *      http://www.eclipse.org/legal/epl-v10.html
 *
 *      The Apache License v2.0 is available at
 *      http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 *  ========================================================================
 */
package org.eclipse.jetty.toolchain.version;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Test version pattern
 */
public class VersionPatternTest
{
    private static final String ECLIPSE_ID = "Eclipse";
    private static final String ECLIPSE_KEY = "jetty-VERSION";
    private static final String CODEHAUS_ID = "Codehaus";
    private static final String CODEHAUS_KEY = "jetty@codehaus-VERSION";

    public static Stream<Arguments> versionData()
    {
        List<Arguments> data = new ArrayList<>();
        // Eclipse Format
        data.add(Arguments.of(ECLIPSE_ID, ECLIPSE_KEY, "jetty-7.5.0.RC2", "jetty-7.5.0.RC2"));
        data.add(Arguments.of(ECLIPSE_ID, ECLIPSE_KEY, "Jetty-6.1.2", "jetty-6.1.2"));
        data.add(Arguments.of(ECLIPSE_ID, ECLIPSE_KEY, "Jetty 2.0Alpha2", "jetty-2.0Alpha2"));
        // Codehaus Format
        data.add(Arguments.of(CODEHAUS_ID, CODEHAUS_KEY, "jetty@codehaus 7.4.2.v20110526", "jetty@codehaus-7.4.2.v20110526"));
        data.add(Arguments.of(CODEHAUS_ID, CODEHAUS_KEY, "jetty@codehaus 7.0.0.RC4", "jetty@codehaus-7.0.0.RC4"));
        data.add(Arguments.of(CODEHAUS_ID, CODEHAUS_KEY, "Jetty@Codehaus 7.4.4.v20110707", "jetty@codehaus-7.4.4.v20110707"));
        return data.stream();
    }

    @ParameterizedTest
    @MethodSource("versionData")
    public void testVersion(String id, String key, String rawVer, String expectedVersion)
    {
        VersionPattern pat = new VersionPattern(key);
        assertThat("VersionPattern(" + id + ").isMatch(\"" + rawVer + "\")", pat.isMatch(rawVer), is(true));
        assertThat("VersionPattern(" + id + ").getLastVersion()", expectedVersion, is(pat.getLastVersion()));

        String expectedAltVersion = expectedVersion.replaceFirst("^.*-", "alt-");
        String altKey = "alt-VERSION";
        assertThat("VersionPattern(" + id + ").getLastVersion(ALTKEY)", expectedAltVersion, is(pat.getLastVersion(altKey)));
    }
}
