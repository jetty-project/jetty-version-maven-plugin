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

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;

import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GitTagParserTest extends AbstractGitTestCase
{
    @Test
    public void testGitTagParse() throws IOException, ParseException
    {
        Path sampleFile = MavenTestingUtils.getTestResourcePathFile("git-tag.txt");
        GitTagParser parser = new GitTagParser();
        parseSampleFile(parser, sampleFile);

        Assertions.assertNotNull(parser.getTagIds(), "parser.tagIds");
        Assertions.assertEquals(121, parser.getTagIds().size(), "parser.tagIds.size");
    }
}
