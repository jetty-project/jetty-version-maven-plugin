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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.toolchain.test.IO;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDir;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDirExtension;
import org.eclipse.jetty.toolchain.version.issues.Issue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(WorkDirExtension.class)
public class VersionTextTest
{
    public WorkDir testdir;

    private void assertPriorVersion(String startVersion, String expectedPriorVersion) throws IOException
    {
        Path sampleVerText = MavenTestingUtils.getTestResourcePathFile("VERSION.txt");
        VersionText vt = new VersionText(VersionPattern.ECLIPSE);
        vt.read(sampleVerText);

        String prior = vt.getPriorVersion(startVersion);
        assertEquals(expectedPriorVersion, prior, "Prior version");
    }

    private void assertVersionList(List<String> expectedVersions, List<String> actualVersions)
    {
        boolean sizeMismatch = (expectedVersions.size() != actualVersions.size());

        int mismatchIdx = -1;
        int len = Math.min(expectedVersions.size(), actualVersions.size());
        for (int i = 0; i < len; i++)
        {
            if (!expectedVersions.get(i).equals(actualVersions.get(i)))
            {
                mismatchIdx = i;
                break;
            }
        }

        if ((mismatchIdx >= 0) || (sizeMismatch))
        {
            if (sizeMismatch && (mismatchIdx < 0))
            {
                mismatchIdx = len;
            }
            System.out.printf("Mismatch Index: %d%n", mismatchIdx);
            dumpStringListSection("Expected Versions", expectedVersions, mismatchIdx);
            dumpStringListSection("Actual Versions", actualVersions, mismatchIdx);

            StringBuilder err = new StringBuilder();
            err.append("Mismatch in lists encountered. [at index ").append(mismatchIdx);
            err.append("]");
            if (sizeMismatch)
            {
                err.append(" (expected.length=").append(expectedVersions.size());
                err.append(", actual.length=").append(actualVersions.size());
                err.append(")");
            }
            assertThat(err.toString(), mismatchIdx, lessThanOrEqualTo(0));
        }
    }

    private void dumpStringListSection(String header, List<String> strs, int offset)
    {
        if (strs == null)
        {
            System.out.printf("%s: <NULL>%n", header);
        }
        else
        {
            int start = Math.max(0, offset - 5);
            int end = Math.min(strs.size(), offset + 5);

            System.out.printf("%s: %d entries%n", header, strs.size());
            for (int i = start; i < end; i++)
            {
                System.out.printf("[%d] %s%n", i, strs.get(i));
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private List<String> getExpectedVersions(String expectedTextPath) throws IOException
    {
        FileReader reader = null;
        BufferedReader buf = null;
        List<String> versions = new ArrayList<>();
        try
        {
            File expectedFile = MavenTestingUtils.getTestResourceFile(expectedTextPath);
            reader = new FileReader(expectedFile);
            buf = new BufferedReader(reader);
            String line;
            while ((line = buf.readLine()) != null)
            {
                if (StringUtils.isBlank(line))
                {
                    // empty. skip
                    continue;
                }
                versions.add(line.trim());
            }
        }
        finally
        {
            IO.close(buf);
            IO.close(reader);
        }
        return versions;
    }

    @Test
    public void testGetPriorVersion_Middle() throws IOException
    {
        assertPriorVersion("jetty-7.4.0.RC0", "jetty-7.3.1.v20110307");
    }

    @Test
    public void testGetPriorVersion_Top() throws IOException
    {
        assertPriorVersion("jetty-7.5.0-v20110808", "jetty-7.4.4.v20110707");
    }

    @Test
    public void testReadCodehausVersionText() throws IOException
    {
        Path sampleVerText = MavenTestingUtils.getTestResourcePathFile("version-codehaus.txt");
        VersionText vt = new VersionText(VersionPattern.CODEHAUS);
        vt.read(sampleVerText);

        assertEquals(30, vt.getReleases().size(), "Number of Releases");

        Release r740 = vt.findRelease("jetty@codehaus-7.4.0.v20110414");
        assertNotNull(r740, "Should have found release");
        assertEquals(2, r740.getIssues().size(), "[7.4.0.v20110414].issues.size");

        Release r720rc0 = vt.findRelease("jetty@codehaus-7.2.0.RC0");
        assertNotNull(r720rc0, "Should have found release");
        assertEquals(9, r720rc0.getIssues().size(), "[7.2.0.RC0].issues.size");
    }

    @Test
    public void testReadEclipseVersionText() throws IOException
    {
        Path sampleVerText = MavenTestingUtils.getTestResourcePathFile("VERSION.txt");
        VersionText vt = new VersionText(VersionPattern.ECLIPSE);
        vt.read(sampleVerText);

        List<String> actualVersions = vt.getVersionList();
        List<String> expectedVersions = getExpectedVersions("expected-versions-eclipse.txt");

        assertVersionList(expectedVersions, actualVersions);

        Release r31rc9 = vt.findRelease("jetty-3.1.rc9");
        assertNotNull(r31rc9, "Should have found release");
        assertEquals(10, r31rc9.getIssues().size(), "[3.1.rc9].issues.size");

        Release r20a2 = vt.findRelease("jetty-2.0Alpha2");
        assertNotNull(r20a2, "Should have found release");
        assertEquals(9, r20a2.getIssues().size(), "[2.0Alpha1].issues.size");
    }

    @Test
    public void testReadEclipseVersion11Text() throws IOException
    {
        Path sampleVerText = MavenTestingUtils.getTestResourcePathFile("VERSION-11.0.x.txt");
        VersionText vt = new VersionText(VersionPattern.ECLIPSE);
        vt.read(sampleVerText);

        List<String> actualVersions = vt.getVersionList();
        List<String> expectedVersions = getExpectedVersions("expected-versions-11.0.x-eclipse.txt");

        assertVersionList(expectedVersions, actualVersions);

        Release r31rc9 = vt.findRelease("jetty-3.1.rc9");
        assertNotNull(r31rc9, "Should have found release");
        assertEquals(10, r31rc9.getIssues().size(), "[3.1.rc9].issues.size");

        Release r20a2 = vt.findRelease("jetty-2.0Alpha2");
        assertNotNull(r20a2, "Should have found release");
        assertEquals(9, r20a2.getIssues().size(), "[2.0Alpha1].issues.size");
    }

    @Test
    public void testReadVersion20Alpha2Text() throws IOException
    {
        Path sampleVerText = MavenTestingUtils.getTestResourcePathFile("version-2.0Alpha2.txt");
        VersionText vt = new VersionText(VersionPattern.ECLIPSE);
        vt.read(sampleVerText);

        assertEquals(1, vt.getReleases().size(), "Number of Releases");

        Release r20a2 = vt.findRelease("jetty-2.0Alpha2");
        assertNotNull(r20a2, "Should have found release");
        assertEquals(9, r20a2.getIssues().size(), "[2.0Alpha1].issues.size");
    }

    /**
     * Test bug that crops up with when the combination of parse/write(with wrapping) results in a line that starts with
     * "-D" that is mistakenly interpreted as the start of another issue by the parsing step.
     */
    @Test
    public void testReadWriteVersion715Text() throws IOException
    {
        Path sampleVerText = MavenTestingUtils.getTestResourcePathFile("version-7.1.5.txt");
        testdir.ensureEmpty();

        // Read first time
        VersionText vt = new VersionText(VersionPattern.ECLIPSE);
        vt.read(sampleVerText);

        // Write it out
        Path out1 = testdir.getPathFile("version-7.1.5-a.txt");
        vt.write(out1);

        // Read generated
        vt = new VersionText(VersionPattern.ECLIPSE);
        vt.read(out1);

        // Write it out again
        Path out2 = testdir.getPathFile("version-7.1.5-b.txt");
        vt.write(out2);

        // Read it in one last time
        vt = new VersionText(VersionPattern.ECLIPSE);
        vt.read(out2);

        assertEquals(1, vt.getReleases().size(), "Number of Releases");

        Release rel = vt.findRelease("jetty-7.1.5.v20100705");
        assertNotNull(rel, "Should have found release");
        assertEquals(21, rel.getIssues().size(), "[7.1.5].issues.size");

        // Find the "pdate ecj to 3.6" entry
        String ecjText = "pdate ecj to 3.6";
        Issue ecjIssue = null;
        for (Issue issue : rel.getIssues())
        {
            if (issue.getText().contains(ecjText))
                ecjIssue = issue;
        }
        assertThat("ECJ Issue found", ecjIssue, notNullValue());
        int idx = ecjIssue.getText().indexOf(ecjText);
        assertTrue(idx >= 0, "ECJ Issue has first occurrence");
        idx = ecjIssue.getText().indexOf(ecjText, idx + ecjText.length());
        assertTrue(idx < 0, "ECJ Issue should not have text twice!");
    }

    @Test
    public void testWriteCodehausVersionText() throws IOException
    {
        Path sampleVerText = MavenTestingUtils.getTestResourcePathFile("version-codehaus.txt");
        VersionText vt = new VersionText(VersionPattern.CODEHAUS);
        vt.read(sampleVerText);

        testdir.ensureEmpty();
        Path outver = testdir.getPathFile("version-out.txt");
        vt.write(outver);

        assertTrue(FsUtil.existsAsFile(outver), "Output version.txt exist");
    }

    @Test
    public void testWriteEclipseVersionText() throws IOException
    {
        Path sampleVerText = MavenTestingUtils.getTestResourcePathFile("VERSION.txt");
        VersionText vt = new VersionText(VersionPattern.ECLIPSE);
        vt.read(sampleVerText);

        testdir.ensureEmpty();
        Path outver = testdir.getPathFile("version-out.txt");
        vt.write(outver);

        assertTrue(FsUtil.existsAsFile(outver), "Output version.txt exist");
    }

    @Test
    public void testWriteSorted() throws IOException
    {
        Path sampleVerText = MavenTestingUtils.getTestResourcePathFile("version-19-sort.txt");
        VersionText vt = new VersionText(VersionPattern.ECLIPSE);
        vt.setSortExisting(true);
        vt.read(sampleVerText);

        testdir.ensureEmpty();
        Path outver = testdir.getPathFile("version-out.txt");
        vt.write(outver);

        assertTrue(FsUtil.existsAsFile(outver), "Output version.txt exist");
        String outputVersion = FsUtil.toString(outver);
        String expectedVersion = FsUtil.toString(MavenTestingUtils.getTestResourcePathFile("version-19-sorted-result.txt"));

        assertThat("Output sorted/generation", outputVersion, is(expectedVersion));
    }
}
