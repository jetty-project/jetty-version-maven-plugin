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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.toolchain.version.util.VersionUtil;

/**
 * Command line utility to merge separate {@code <version>.txt} into a
 * single VERSION.txt file
 */
public class MergeVersionText
{
    public static void main(String[] args)
    {
        Path versionTxtFile = null;
        Path inputDir = null;

        for (String arg : args)
        {
            if (arg.startsWith("-V="))
            {
                versionTxtFile = Paths.get(arg.substring(3));
            }
            else if (arg.startsWith("-I="))
            {
                inputDir = Paths.get(arg.substring(3));
            }
            else
            {
                throw new IllegalArgumentException("Unrecognized argument: " + arg);
            }
        }

        if (versionTxtFile == null)
            fail("VERSION.txt not specified.  (Use -V=<path-to-file> to specify)");
        if (inputDir == null)
            fail("Input directory not specified.  (Use -I=<path-to-dir> to specify)");

        if (!Files.isRegularFile(versionTxtFile))
            fail("Not a file: %s", versionTxtFile);
        if (!Files.isDirectory(inputDir))
            fail("Not a directory: %s", inputDir);

        try
        {
            VersionPattern versionPattern = VersionPattern.ECLIPSE;
            VersionText versionText = new VersionText(versionPattern);
            versionText.read(versionTxtFile);

            // Figure out major version existing VERSION.txt supports.
            // and limit introduction of newer major versions into VERSION.txt
            final int majorVersion = findMajorVersion(versionText);
            System.out.printf("Major Version: %d%n", majorVersion);

            // Read all input version.txt files
            // Filter out when major version isn't supported (too new)
            try (Stream<Path> listing = Files.list(inputDir))
            {
                List<Release> releases = listing
                    .filter((p) -> p.getFileName().toString().endsWith(".txt"))
                    .filter((p) -> p.getFileName().toString().startsWith("jetty-"))
                    .filter((p) -> VersionUtil.parseMajorVersion(p.getFileName().toString()) <= majorVersion)
                    .map((p) ->
                    {
                        try
                        {
                            return Release.readAsText(p);
                        }
                        catch (IOException e)
                        {
                            throw new RuntimeException(e);
                        }
                    })
                    .sorted(new Release.ReleaseDateComparator())
                    .collect(Collectors.toList());

                // Merge into existing VersionText any entries
                for (Release release : releases)
                {
                    Release existingRelease = versionText.findRelease(release.getVersion());
                    if (existingRelease == null)
                        versionText.addRelease(release);
                    else
                    {
                        existingRelease.setReleasedOn(release.getReleasedOn());
                        existingRelease.mergeIssues(release.getIssues());
                    }
                }

                versionText.sortReleases();

                // Write out newly updated VersionText
                versionText.write(versionTxtFile);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static int findMajorVersion(VersionText versionText)
    {
        int majorVersion = 0;
        for (Release release : versionText.getReleases())
        {
            final String rawVersion = release.getVersion();
            int majInt = VersionUtil.parseMajorVersion(rawVersion);
            if (majorVersion < majInt)
                majorVersion = majInt;
        }
        return majorVersion;
    }

    private static void fail(String format, Object... args)
    {
        System.err.printf("FAIL: " + format + "%n", args);
        System.exit(2);
    }
}
