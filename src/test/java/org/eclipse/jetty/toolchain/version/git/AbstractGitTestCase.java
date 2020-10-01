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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

public abstract class AbstractGitTestCase
{
    protected void parseSampleFile(GitOutputParser parser, Path sampleFile) throws IOException, ParseException
    {
        int linenum = 0;
        parser.parseStart();
        for (String line : Files.readAllLines(sampleFile, StandardCharsets.UTF_8))
        {
            linenum++;
            parser.parseLine(linenum, line);
        }
        parser.parseEnd();
    }
}
