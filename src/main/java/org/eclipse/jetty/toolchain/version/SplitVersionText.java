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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.Instant;

import org.eclipse.jetty.toolchain.version.issues.Issue;

/**
 * Command line utility to split the VERSION.txt into separate
 * version entry files.
 */
public class SplitVersionText
{
    public static void main(String[] args)
    {
        Path versionTxtFile = null;
        Path outputDir = null;

        for (String arg: args)
        {
            if (arg.startsWith("-V="))
            {
                versionTxtFile = Paths.get(arg.substring(3));
            }
            else if (arg.startsWith("-O="))
            {
                outputDir = Paths.get(arg.substring(3));
            }
            else
            {
                throw new IllegalArgumentException("Unrecognized argument: " + arg);
            }
        }

        if (versionTxtFile == null)
            fail("VERSION.txt not specified.  (Use -V=<path-to-file> to specify)");
        if (outputDir == null)
            fail("Output directory not specified.  (Use -O=<path-to-dir> to specify)");

        if (!Files.isRegularFile(versionTxtFile))
            fail("Not a file: %s", versionTxtFile);
        if (!Files.isDirectory(outputDir))
            fail("Not a directory: %s", outputDir);

        try
        {
            VersionText versionText = new VersionText(VersionPattern.ECLIPSE);
            versionText.read(versionTxtFile);

            for (Release release: versionText.getReleases())
            {
                if (release.getVersion().contains("SNAPSHOT"))
                    continue; // skip
                Path outputFile = outputDir.resolve(String.format("%s.txt", release.getVersion()));
                try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8);
                     PrintWriter out = new PrintWriter(writer))
                {
                    out.print(release.getVersion());
                    if (release.getReleasedOn() != null)
                    {
                        SimpleDateFormat sdf = new SimpleDateFormat(" - dd MMMM yyyy");
                        out.print(sdf.format(release.getReleasedOn()));
                    }
                    out.print('\n');

                    for (Issue issue: release.getSortedIssues())
                    {
                        out.print(issue.toString());
                        out.print('\n');
                    }
                }
                // update date/time-stamp?
                if (release.getReleasedOn() != null)
                {
                    Instant instant = release.getReleasedOn().toInstant();
                    Files.setLastModifiedTime(outputFile, FileTime.from(instant));
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void fail(String format, Object ... args)
    {
        System.err.printf("FAIL: " + format + "%n", args);
        System.exit(2);
    }
}
