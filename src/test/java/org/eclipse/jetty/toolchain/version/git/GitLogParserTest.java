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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.toolchain.test.TestingDir;
import org.eclipse.jetty.toolchain.version.Release;
import org.eclipse.jetty.toolchain.version.VersionPattern;
import org.eclipse.jetty.toolchain.version.VersionText;
import org.eclipse.jetty.toolchain.version.issues.Issue;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class GitLogParserTest extends AbstractGitTestCase
{
    @Rule
    public TestingDir testdir = new TestingDir();
    
    private void assertIssuesPresent(GitLogParser parser, List<String> issueIds)
    {
        for (Issue issue : parser.getIssues())
        {
            if (issueIds.contains(issue.getId()))
            {
                issueIds.remove(issue.getId());
            }
            // System.out.printf("Issue[%s] %s%n", issue.getId(), issue.getText());
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
            Assert.assertEquals(err.toString(), 0, issueIds.size());
        }
    }
    
    private Map<String, Integer> calcAuthorshipCounts(GitLogParser parser)
    {
        Map<String, Integer> authorshipCounts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        
        for (GitCommit commit : parser.getGitCommitLogs())
        {
            Integer count = authorshipCounts.get(commit.getAuthorName());
            if (count == null)
            {
                count = new Integer(0);
            }
            count++;
            authorshipCounts.put(commit.getAuthorName(), count);
        }
        
        return authorshipCounts;
    }
    
    private Set<String> getUniqueIssueIds(GitLogParser parser)
    {
        Set<String> ids = new HashSet<>();
        for (Issue issue : parser.getIssues())
        {
            ids.add(issue.getId());
        }
        return ids;
    }
    
    @Test
    public void testParseGitLogTag() throws IOException, ParseException
    {
        File sampleFile = MavenTestingUtils.getTestResourceFile("git-log-specific-tag.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);
        
        Assert.assertNotNull("parser.gitCommitLogs", parser.getGitCommitLogs());
        Assert.assertEquals("parser.gitCommitLogs.size", 1, parser.getGitCommitLogs().size());
        GitCommit commit = parser.getGitCommitLog(0);
        Assert.assertNotNull("parser.getGitCommitLog(0)", commit);
        Assert.assertEquals("commit.id", "5aa94f502e5efe68628cff0378f44ff00619c493", commit.getCommitId());
    }
    
    @Test
    public void testParseIssueIds() throws IOException, ParseException
    {
        File sampleFile = MavenTestingUtils.getTestResourceFile("git-log-to-commit.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);
        
        List<Issue> issues = parser.getIssues();
        Assert.assertEquals("Commit entries with Issue IDs", 116, issues.size());
        
        Release rel = new Release("TEST-VERSION");
        rel.setExisting(false);
        rel.addIssues(issues);
        
        VersionText vt = new VersionText(VersionPattern.ECLIPSE);
        vt.addRelease(rel);
        
        testdir.ensureEmpty();
        Path outfile = testdir.getPathFile("test-ver.txt");
        vt.write(outfile.toFile());
        
        // TODO: compare output
    }
    
    @Test
    public void testParseLongGitLog() throws IOException, ParseException
    {
        File sampleFile = MavenTestingUtils.getTestResourceFile("git-log-to-commit.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);
        
        assertThat("parser.gitCommitLogs", parser.getGitCommitLogs(), notNullValue());
        assertThat("parser.gitCommitLogs.size", parser.getGitCommitLogs().size(), is(255));
        
        Map<String, Integer> authors = calcAuthorshipCounts(parser);
        assertThat("Commits by Jesse", authors.get("Jesse McConnell"), is(79));
    }
    
    @Test
    public void testParseJetty9GitLog() throws IOException, ParseException
    {
        File sampleFile = MavenTestingUtils.getTestResourceFile("git-jetty9-log.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);
        
        assertThat("parser.gitCommitLogs", parser.getGitCommitLogs(), notNullValue());
        assertThat("parser.gitCommitLogs.size", parser.getGitCommitLogs().size(), is(160));
        
        Map<String, Integer> authors = calcAuthorshipCounts(parser);
        assertThat("Commits by Joakim", authors.get("Joakim Erdfelt"), is(10));
        
        // Test for known issues
        List<String> issueIds = new ArrayList<String>();
        issueIds.add("391483");
        issueIds.add("388079");
        issueIds.add("391588");
        issueIds.add("JETTY-1515");
        
        assertIssuesPresent(parser, issueIds);
        
        Assert.assertEquals("Issue count", 42, parser.getIssues().size());
    }
    
    @Test
    public void testParseJetty93_GitHubLog() throws IOException, ParseException
    {
        File sampleFile = MavenTestingUtils.getTestResourceFile("git-log-9.3.7..HEAD.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);
        
        assertThat("parser.gitCommitLogs", parser.getGitCommitLogs(), notNullValue());
        assertThat("parser.gitCommitLogs.size", parser.getGitCommitLogs().size(), is(75));
        
        Map<String, Integer> authors = calcAuthorshipCounts(parser);
        assertThat("Commits by Joakim", authors.get("Joakim Erdfelt"), is(18));
        assertThat("Commits by Simone", authors.get("Simone Bordet"), is(25));
        assertThat("Commits by Greg", authors.get("Greg Wilkins"), is(21));
        
        // Test for some known issues (not all)
        List<String> issueIds = new ArrayList<String>();
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
        
        assertIssuesPresent(parser, issueIds);
        
        Assert.assertEquals("Issue count", 48, parser.getIssues().size());
    }
    
    @Test
    public void testParseJetty9_3_11_GitHubLog() throws IOException, ParseException
    {
        File sampleFile = MavenTestingUtils.getTestResourceFile("git-log-9.3.11.M0..9.3.11.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);
        
        assertThat("parser.gitCommitLogs", parser.getGitCommitLogs(), notNullValue());
        assertThat("parser.gitCommitLogs.size", parser.getGitCommitLogs().size(), is(137));
        
        Map<String, Integer> authors = calcAuthorshipCounts(parser);
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
        
        assertIssuesPresent(parser, issueIds);
        
        Set<String> uniqueIds = getUniqueIssueIds(parser);
        
        assertThat("Unique Issue count", uniqueIds.size(), is(68));
        
        // Oddball commit with 2 values should not trigger on second value
        assertFalse("Issue #724 should not be picked up", uniqueIds.contains("724"));
    }
    
    @Test
    public void testParseSingleGitLog() throws IOException, ParseException
    {
        File sampleFile = MavenTestingUtils.getTestResourceFile("git-log-specific-commit.txt");
        GitLogParser parser = new GitLogParser();
        parseSampleFile(parser, sampleFile);
        
        assertThat("parser.gitCommitLogs", parser.getGitCommitLogs(), notNullValue());
        assertThat("parser.gitCommitLogs.size", parser.getGitCommitLogs().size(), is(1));
        
        GitCommit commit = parser.getGitCommitLog(0);
        Assert.assertNotNull("parser.getGitCommitLog(0)", commit);
        Assert.assertEquals("commit.id", "596fa1bd4edebc21de0389ff70b10b8060667ed1", commit.getCommitId());
    }
    
    @Test
    @Ignore("just used to visually see tweaks to the output")
    public void testShowPrettyFormat()
    {
        GitLogParser parser = new GitLogParser();
        System.out.println(parser.getFormat());
    }
}
