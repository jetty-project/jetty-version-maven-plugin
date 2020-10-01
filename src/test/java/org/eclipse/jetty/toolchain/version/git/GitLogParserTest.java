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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDir;
import org.eclipse.jetty.toolchain.test.jupiter.WorkDirExtension;
import org.eclipse.jetty.toolchain.version.Release;
import org.eclipse.jetty.toolchain.version.VersionPattern;
import org.eclipse.jetty.toolchain.version.VersionText;
import org.eclipse.jetty.toolchain.version.issues.Issue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(WorkDirExtension.class)
public class GitLogParserTest extends AbstractGitTestCase
{
    public WorkDir testdir;

    private void assertIssuesPresent(List<GitCommit> commits, List<String> issueIds)
    {
        for (GitCommit commit : commits)
        {
            for (String commitIssueId : commit.getIssueIds())
            {
                issueIds.remove(commitIssueId);
            }
        }

        if (issueIds.size() > 0)
        {
            StringBuilder err = new StringBuilder();
            err.append("Issue parser failed to find issue id");
            if (issueIds.size() > 1)
            {
                err.append("s");
            }
            err.append(":");
            for (String id : issueIds)
            {
                err.append(" ").append(id);
            }
            err.append(".");
            assertEquals(0, issueIds.size(), err.toString());
        }
    }

    private Map<String, Integer> calcAuthorshipCounts(List<GitCommit> commits)
    {
        Map<String, Integer> authorshipCounts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (GitCommit commit : commits)
        {
            Integer count = authorshipCounts.get(commit.getAuthorName());
            if (count == null)
            {
                count = 0;
            }
            count++;
            authorshipCounts.put(commit.getAuthorName(), count);
        }

        return authorshipCounts;
    }

    private Set<String> getUniqueIssueIds(List<GitCommit> commits)
    {
        Set<String> ids = new HashSet<>();
        for (GitCommit commit : commits)
        {
            ids.addAll(commit.getIssueIds());
        }
        return ids;
    }

    @Test
    public void testParseGitLogTag() throws IOException, ParseException
    {
        Path sampleFile = MavenTestingUtils.getTestResourcePathFile("git-log-specific-tag.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);

        assertNotNull(parser.getGitCommitLogs(), "parser.gitCommitLogs");
        assertEquals(1, parser.getGitCommitLogs().size(), "parser.gitCommitLogs.size");
        GitCommit commit = parser.getGitCommitLog(0);
        assertNotNull(commit, "parser.getGitCommitLog(0)");
        assertEquals("5aa94f502e5efe68628cff0378f44ff00619c493", commit.getCommitId(), "commit.id");
    }

    @Test
    public void testParseIssueIds() throws IOException, ParseException
    {
        Path sampleFile = MavenTestingUtils.getTestResourcePathFile("git-log-to-commit.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);

        List<GitCommit> commits = parser.getGitCommitLogs();

        Set<String> uniqueIds = getUniqueIssueIds(commits);
        assertThat("Unique Issue count", uniqueIds.size(), is(70));

        List<Issue> issues = new ArrayList<>();
        for (String issueId : uniqueIds)
        {
            issues.add(new Issue(issueId));
        }

        Release rel = new Release("TEST-VERSION");
        rel.setExisting(false);
        rel.addIssues(issues);

        VersionText vt = new VersionText(VersionPattern.ECLIPSE);
        vt.addRelease(rel);

        testdir.ensureEmpty();
        Path outfile = testdir.getPathFile("test-ver.txt");
        vt.write(outfile);
    }

    @Test
    public void testParseLongGitLog() throws IOException, ParseException
    {
        Path sampleFile = MavenTestingUtils.getTestResourcePathFile("git-log-to-commit.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);

        List<GitCommit> commits = parser.getGitCommitLogs();

        assertThat("parser.gitCommitLogs", commits, notNullValue());
        assertThat("parser.gitCommitLogs.size", commits.size(), is(255));

        Map<String, Integer> authors = calcAuthorshipCounts(commits);
        assertThat("Commits by Jesse", authors.get("Jesse McConnell"), is(79));
    }

    @Test
    public void testParseJetty9GitLog() throws IOException, ParseException
    {
        Path sampleFile = MavenTestingUtils.getTestResourcePathFile("git-jetty9-log.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);

        List<GitCommit> commits = parser.getGitCommitLogs();

        assertThat("parser.gitCommitLogs", commits, notNullValue());
        assertThat("parser.gitCommitLogs.size", commits.size(), is(160));

        Map<String, Integer> authors = calcAuthorshipCounts(commits);
        assertThat("Commits by Joakim", authors.get("Joakim Erdfelt"), is(10));

        // Test for known issues
        List<String> issueIds = new ArrayList<>();
        issueIds.add("391483");
        issueIds.add("388079");
        issueIds.add("391588");
        issueIds.add("JETTY-1515");

        assertIssuesPresent(commits, issueIds);

        Set<String> uniqueIds = getUniqueIssueIds(commits);
        assertThat("Unique Issue count", uniqueIds.size(), is(39));
    }

    @Test
    public void testParseJetty93_GitHubLog() throws IOException, ParseException
    {
        Path sampleFile = MavenTestingUtils.getTestResourcePathFile("git-log-9.3.7..HEAD.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);

        List<GitCommit> commits = parser.getGitCommitLogs();

        assertThat("parser.gitCommitLogs", commits, notNullValue());
        assertThat("parser.gitCommitLogs.size", commits.size(), is(75));

        Map<String, Integer> authors = calcAuthorshipCounts(commits);
        assertThat("Commits by Joakim", authors.get("Joakim Erdfelt"), is(18));
        assertThat("Commits by Simone", authors.get("Simone Bordet"), is(25));
        assertThat("Commits by Greg", authors.get("Greg Wilkins"), is(21));

        // Test for some known issues (not all)
        List<String> issueIds = new ArrayList<>();
        // Bugzilla Ids
        issueIds.add("478918");
        issueIds.add("487714");
        issueIds.add("487158");
        issueIds.add("487750");
        issueIds.add("484446");
        issueIds.add("486511");
        issueIds.add("486394");

        // Github Issues
        issueIds.add("81");
        issueIds.add("84");
        issueIds.add("83");
        issueIds.add("80");
        issueIds.add("79");

        assertIssuesPresent(commits, issueIds);

        Set<String> uniqueIds = getUniqueIssueIds(commits);
        assertThat("Unique Issue count", uniqueIds.size(), is(37));
    }

    @Test
    public void testParseJetty9_3_11_GitHubLog() throws IOException, ParseException
    {
        Path sampleFile = MavenTestingUtils.getTestResourcePathFile("git-log-9.3.11.M0..9.3.11.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);

        List<GitCommit> commits = parser.getGitCommitLogs();

        assertThat("parser.gitCommitLogs", commits, notNullValue());
        assertThat("parser.gitCommitLogs.size", commits.size(), is(137));

        Map<String, Integer> authors = calcAuthorshipCounts(commits);
        assertThat("Commits by Joakim", authors.get("Joakim Erdfelt"), is(33));
        assertThat("Commits by Simone", authors.get("Simone Bordet"), is(20));
        assertThat("Commits by Greg", authors.get("Greg Wilkins"), is(33));

        // Test for known quirky issue syntaxes
        List<String> issueIds = new ArrayList<>();
        issueIds.add("230"); // "Extensible ErrorHandler for different mimetypes #230"
        issueIds.add("592"); // "fix #592"
        issueIds.add("631"); // "SLOTH protection #631"
        issueIds.add("658"); // " Issue  #658"
        issueIds.add("649"); // "Resolve Issue #649 by checking for null password on a"
        issueIds.add("671"); // "Incorrect default ALPN protocol #671"
        issueIds.add("666"); // "Fixing ... for JMX Dump; added ... configuration. (#666)"
        issueIds.add("723"); // "Fixes #723 - improves ... error reporting (#724)"
        issueIds.add("724"); // "Fixes #723 - improves ... error reporting (#724)"

        assertIssuesPresent(commits, issueIds);

        Set<String> uniqueIds = getUniqueIssueIds(commits);

        assertThat("Unique Issue count", uniqueIds.size(), is(69));
    }

    @Test
    public void testParseJetty9_4_7_GitHubLog() throws IOException, ParseException
    {
        Path sampleFile = MavenTestingUtils.getTestResourcePathFile("git-log-9.4.6..9.4.7-HEAD.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);

        List<GitCommit> commits = parser.getGitCommitLogs();

        assertThat("parser.commits (raw)", commits, notNullValue());
        assertThat("parser.commits.size", commits.size(), is(323));

        // Filter commits
        GitFilter gitFilter = new GitFilter();
        gitFilter.addFilenameExclude("jetty-documentation/.*");
        gitFilter.addFilenameExclude("examples/.*");
        gitFilter.addFilenameExclude("aggregates/.*");
        gitFilter.addFilenameExclude("tests/.*");
        gitFilter.addFilenameExclude(".*/test-.*");
        gitFilter.addFilenameExclude(".*/.*-test/.*");
        gitFilter.addFilenameExclude(".*/.*-tests/.*");
        gitFilter.addFilenameExclude(".*/src/test/.*");
        gitFilter.addFilenameExclude("\\.git.*");
        gitFilter.addFilenameExclude(".*\\.md$");
        gitFilter.addFilenameExclude("Jenkinsfile");
        gitFilter.addFilenameExclude(".*\\.txt$");

        List<GitCommit> filteredCommits = gitFilter.filter(commits);

        // dumpUniqueFilenames(filteredCommits);

        assertThat("parser.filtered-commits.size", filteredCommits.size(), is(217));

        Map<String, Integer> authors = calcAuthorshipCounts(filteredCommits);
        assertThat("Commits by Joakim", authors.get("Joakim Erdfelt"), is(45));
        assertThat("Commits by Simone", authors.get("Simone Bordet"), is(47));
        assertThat("Commits by Greg", authors.get("Greg Wilkins"), is(69));

        Set<String> uniqueIds = getUniqueIssueIds(commits);

        assertThat("Unique Issue count", uniqueIds.size(), is(110));

        // Test for known quirky issue syntaxes
        List<String> issueIds = new ArrayList<>();
        issueIds.add("475546"); // ClosedChannelException when connecting to HTTPS over HTTP proxy with CONNECT.
        issueIds.add("1623"); // "simplify code, add more details in junit failure #1623"
        issueIds.add("1611"); // "HTTP/2 :authority: declaration should omit default ports in jetty-client (#1611)"
        issueIds.add("1732"); // "Issue #1732 Connection Limit (#1745)"
        issueIds.add("1745"); // "Issue #1732 Connection Limit (#1745)"

        assertIssuesPresent(commits, issueIds);

        // Oddball commit with 2 values should not trigger on second value
        Assertions.assertFalse(uniqueIds.contains("724"), "Issue #724 should not be picked up");
    }

    public void dumpUniqueFilenames(List<GitCommit> commits)
    {
        SortedSet<String> uniqueFilenames = new TreeSet<>();

        for (GitCommit commit : commits)
        {
            for (String filename : commit.getFilenames())
            {
                uniqueFilenames.add(filename);
            }
        }

        for (String filename : uniqueFilenames)
        {
            System.err.printf("# %s%n", filename);
        }
    }

    @Test
    public void testParseSingleGitLog() throws IOException, ParseException
    {
        Path sampleFile = MavenTestingUtils.getTestResourcePathFile("git-log-specific-commit.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);

        List<GitCommit> commits = parser.getGitCommitLogs();

        assertThat("parser.gitCommitLogs", commits, notNullValue());
        assertThat("parser.gitCommitLogs.size", commits.size(), is(1));

        GitCommit commit = parser.getGitCommitLog(0);
        assertNotNull(commit, "parser.getGitCommitLog(0)");
        assertEquals("596fa1bd4edebc21de0389ff70b10b8060667ed1", commit.getCommitId(), "commit.id");
    }

    @Test
    @Disabled("just used to visually see tweaks to the output")
    public void testShowPrettyFormat()
    {
        GitLogParser parser = new GitLogParser();
        System.out.println(parser.getFormat());
    }
}
