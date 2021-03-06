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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.jetty.toolchain.version.Release;
import org.eclipse.jetty.toolchain.version.issues.Issue;

public class GitCommand
{
    private Log log;
    private File workDir;
    private GitFilter filter;

    private int execGitCommand(GitOutputParser outputParser, String... commands) throws IOException
    {
        if (getLog().isDebugEnabled())
        {
            StringBuilder dbg = new StringBuilder();
            for (String cmd : commands)
            {
                dbg.append(" ").append(cmd);
            }
            getLog().debug("Command Line:" + dbg.toString());
        }

        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.directory(getWorkDir());
        Process process = pb.start();

        InputStream in = null;
        try
        {
            in = process.getInputStream();
            OutputHandler handler = new OutputHandler(getLog(), in, outputParser);
            handler.start();
            int exitCode = process.waitFor();
            getLog().debug("Exit code: " + exitCode);
            handler.join();
            return exitCode;
        }
        catch (InterruptedException e)
        {
            getLog().error("Git Process didn't complete", e);
            throw new IOException("Git Process did not complete");
        }
        finally
        {
            IOUtil.close(in);
        }
    }

    public boolean fetchTags() throws IOException
    {
        Git2LogParser logout = new Git2LogParser(this.log, "fetch tags");
        int exitCode = execGitCommand(logout, "git", "fetch", "--tags");
        return (exitCode == 0);
    }

    public String findTagMatching(String version) throws IOException
    {
        this.log.debug("findTagMatching(" + version + ")");
        if (version != null)
        {
            for (String tag : getTags())
            {
                this.log.debug("[tag] \"" + tag + "\"");
                if (tag.startsWith(version))
                {
                    return "tags/" + tag;
                }
            }
        }
        return null;
    }

    public List<GitCommit> getCommitLog(String fromCommitId) throws IOException
    {
        GitLogParser logs = new GitLogParser();
        execGitCommand(logs, "git", "log", logs.getFormat(), "--name-only", fromCommitId + "..HEAD");
        return logs.getGitCommitLogs();
    }

    public GitFilter getFilter()
    {
        return filter;
    }

    public Log getLog()
    {
        if (log == null)
        {
            log = new SystemStreamLog();
        }
        return log;
    }

    public String getTagCommitId(String tagId) throws IOException
    {
        GitLogParser logs = new GitLogParser();
        execGitCommand(logs, "git", "log", "-1", tagId, logs.getFormat());
        getLog().debug("Captured " + logs.getGitCommitLogs().size() + " log entries");
        GitCommit commit = logs.getGitCommitLog(0);
        return commit.getCommitId();
    }

    public List<String> getTags() throws IOException
    {
        GitTagParser tags = new GitTagParser();
        execGitCommand(tags, "git", "tag", "-l");
        return tags.getTagIds();
    }

    public File getWorkDir()
    {
        if (workDir == null)
        {
            workDir = new File(System.getProperty("user.dir"));
        }
        return workDir;
    }

    public void populateIssuesForRange(String fromCommitId, String toCommitId, Release rel) throws IOException
    {
        GitLogParser parser = new GitLogParser();
        execGitCommand(parser, "git", "log", parser.getFormat(), "--name-only", fromCommitId + ".." + toCommitId);

        List<GitCommit> rawcommits = parser.getGitCommitLogs();
        getLog().info("Captured " + rawcommits.size() + " git log entries");

        List<GitCommit> filtered = new ArrayList<>();
        if (filter != null)
        {
            filtered = filter.filter(rawcommits);
        }

        getLog().info("Found " + rawcommits.size() + " git log entries (excluded " + (rawcommits.size() - filtered.size()) + " entries)");

        Set<String> uniqueIssueIds = new HashSet<>();
        for (GitCommit commit : filtered)
        {
            uniqueIssueIds.addAll(commit.getIssueIds());
        }
        getLog().info("Found " + uniqueIssueIds.size() + " issues in git log");

        List<Issue> issues = new ArrayList<>();
        for (String issueId : uniqueIssueIds)
        {
            issues.add(new Issue(issueId));
        }
        rel.setExisting(false);
        rel.addIssues(issues);
    }

    public void setFilter(GitFilter filter)
    {
        this.filter = filter;
    }

    public void setLog(Log log)
    {
        this.log = log;
    }

    public void setWorkDir(File basedir)
    {
        this.workDir = basedir;
    }
}
