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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.toolchain.version.issues.IssueParser;

public class GitLogParser implements GitOutputParser
{
    enum State
    {
        HEADERS,
        BODY,
        FILES
    }

    private static final String AUTHOR_DATE = "#AUTHOR_DATE#:";
    private static final String AUTHOR_NAME = "#AUTHOR_NAME#:";
    private static final String BODY = "#BODY#:";
    private static final String COMMIT_ID = "#COMMIT_ID#:";
    private static final String COMMITTER_DATE = "#COMMITTER_DATE#:";
    private static final String COMMITTER_NAME = "#COMMITTER_NAME#:";
    private static final String END = "####";
    private static final String SUBJECT = "#SUBJECT#:";

    private State state = null;
    private GitCommit activeCommit;
    private final List<GitCommit> commits = new ArrayList<GitCommit>();

    public String getFormat()
    {
        StringBuilder fmt = new StringBuilder();
        fmt.append("--pretty=format:");
        fmt.append(COMMIT_ID).append("%H%n");
        fmt.append(AUTHOR_NAME).append("%an%n");
        fmt.append(AUTHOR_DATE).append("%ai%n");
        fmt.append(COMMITTER_NAME).append("%cn%n");
        fmt.append(COMMITTER_DATE).append("%ci%n");
        fmt.append(SUBJECT).append("%s%n");
        fmt.append(BODY).append("%b%n");
        fmt.append(END);
        return fmt.toString();
    }

    public GitCommit getGitCommitLog(int index)
    {
        return commits.get(index);
    }

    public List<GitCommit> getGitCommitLogs()
    {
        return commits;
    }

    public void parseEnd()
    {
        // parse the git commit subject and body for issues ids
        IssueParser issueparser = new IssueParser();
        for (GitCommit commit : commits)
        {
            Set<String> issueIds = new HashSet<>();
            issueparser.findPossibleIssueIds(commit.getSubject(), issueIds);
            for (String line : commit.getBody())
            {
                issueparser.findPossibleIssueIds(line, issueIds);
            }
            commit.setIssueIds(issueIds);
        }
    }

    public void parseLine(int linenum, String line) throws ParseException
    {
        if (line.startsWith(COMMIT_ID))
        {
            activeCommit = new GitCommit();
            commits.add(activeCommit);
            state = State.HEADERS;
            activeCommit.setCommitId(line.substring(COMMIT_ID.length()));
            return;
        }

        if (activeCommit == null)
        {
            throw new ParseException("Unexpected Git Log line: " + line, 0);
        }

        switch (state)
        {
            case HEADERS:
                if (line.startsWith(AUTHOR_NAME))
                {
                    activeCommit.setAuthorName(line.substring(AUTHOR_NAME.length()));
                }
                else if (line.startsWith(AUTHOR_DATE))
                {
                    activeCommit.parseAuthorDate(line.substring(AUTHOR_DATE.length()));
                }
                else if (line.startsWith(COMMITTER_NAME))
                {
                    activeCommit.setCommitterName(line.substring(COMMITTER_NAME.length()));
                }
                else if (line.startsWith(COMMITTER_DATE))
                {
                    activeCommit.parseCommitterDate(line.substring(COMMITTER_DATE.length()));
                }
                else if (line.startsWith(SUBJECT))
                {
                    activeCommit.setSubject(line.substring(SUBJECT.length()));
                }
                else if (line.startsWith(BODY))
                {
                    activeCommit.addBodyLine(line.substring(BODY.length()));
                    state = State.BODY;
                }
                else if (line.startsWith(END))
                {
                    state = State.FILES;
                }
                break;
            case BODY:
                if (line.startsWith(END))
                {
                    state = State.FILES;
                }
                activeCommit.addBodyLine(line.trim());
                break;
            case FILES:
                String filename = line.trim();
                if (StringUtils.isNotBlank(filename))
                {
                    activeCommit.addFilename(filename);
                }
                break;
        }
    }

    public void parseStart()
    {
        commits.clear();
    }
}
