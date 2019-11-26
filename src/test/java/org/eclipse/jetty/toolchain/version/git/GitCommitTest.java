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
package org.eclipse.jetty.toolchain.version.git;

import java.text.ParseException;

import org.eclipse.jetty.toolchain.version.util.DateUtil;
import org.junit.Assert;
import org.junit.Test;

public class GitCommitTest
{
    @Test
    public void testParseAuthorDate() throws ParseException
    {
        String rawdate = "2011-05-12 09:02:36 +0000";
        GitCommit commit = new GitCommit();
        commit.parseAuthorDate(rawdate);

        String actual = DateUtil.formatRfc2822(commit.getAuthorDate());
        String expected = "Thu, 12 May 2011 09:02:36 +0000";
        Assert.assertEquals("Author date '" + actual + "',expected:'" + expected + "'",expected,actual);
    }

    @Test
    public void testParseCommitterDate() throws ParseException
    {
        String rawdate = "2011-07-27 11:16:03 +1000";
        GitCommit commit = new GitCommit();
        commit.parseCommitterDate(rawdate);

        String actual = DateUtil.formatRfc2822(commit.getCommitterDate());
        Assert.assertEquals("Committer date","Wed, 27 Jul 2011 01:16:03 +0000",actual);
    }
}
