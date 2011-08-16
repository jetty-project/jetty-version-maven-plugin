package org.eclipse.jetty.toolchain.version.git;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.toolchain.version.issues.Issue;
import org.eclipse.jetty.toolchain.version.issues.IssueParser;

public class GitLogParser implements GitOutputParser
{
    private static final String AUTHOR_DATE = "#AUTHOR_DATE#:";
    private static final String AUTHOR_NAME = "#AUTHOR_NAME#:";
    private static final String BODY = "#BODY#:";
    private static final String COMMIT_ID = "#COMMIT_ID#:";
    private static final String COMMITTER_DATE = "#COMMITTER_DATE#:";
    private static final String COMMITTER_NAME = "#COMMITTER_NAME#:";
    private static final String END = "####";
    private static final String SUBJECT = "#SUBJECT#:";

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

    public List<Issue> getIssues()
    {
        IssueParser issueparser = new IssueParser();
        List<Issue> issues = new ArrayList<Issue>();

        Issue issue;
        for (GitCommit commit : commits)
        {
            issue = issueparser.parsePossibleIssue(commit.getSubject());
            if (issue != null)
            {
                issues.add(issue);
            }
        }

        return issues;
    }

    @Override
    public void parseEnd()
    {
        if (activeCommit != null)
        {
            commits.add(activeCommit);
        }
    }

    @Override
    public void parseLine(int linenum, String line) throws IOException
    {
        if (activeCommit == null)
        {
            activeCommit = new GitCommit();
        }

        if (line.startsWith(COMMIT_ID))
        {
            activeCommit.setCommitId(line.substring(COMMIT_ID.length()));
        }
        else if (line.startsWith(AUTHOR_NAME))
        {
            activeCommit.setAuthorName(line.substring(AUTHOR_NAME.length()));
        }
        else if (line.startsWith(AUTHOR_DATE))
        {
            try
            {
                activeCommit.parseAuthorDate(line.substring(AUTHOR_DATE.length()));
            }
            catch (ParseException e)
            {
                throw new IOException("Unable to parse author date at line #" + linenum,e);
            }
        }
        else if (line.startsWith(COMMITTER_NAME))
        {
            activeCommit.setCommitterName(line.substring(COMMITTER_NAME.length()));
        }
        else if (line.startsWith(COMMITTER_DATE))
        {
            try
            {
                activeCommit.parseCommitterDate(line.substring(COMMITTER_DATE.length()));
            }
            catch (ParseException e)
            {
                throw new IOException("Unable to parse committer date at line #" + linenum,e);
            }
        }
        else if (line.startsWith(SUBJECT))
        {
            activeCommit.setSubject(line.substring(SUBJECT.length()));
        }
        else if (line.startsWith(BODY))
        {
            activeCommit.setBody(line.substring(BODY.length()));
        }
        else if (line.equals(END))
        {
            commits.add(activeCommit);
            activeCommit = null;
        }
        else
        {
            activeCommit.appendBody(line.trim());
        }
    }

    @Override
    public void parseStart()
    {
        commits.clear();
    }
}
