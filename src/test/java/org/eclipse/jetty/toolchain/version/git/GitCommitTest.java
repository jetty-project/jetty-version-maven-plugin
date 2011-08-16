package org.eclipse.jetty.toolchain.version.git;

import java.text.ParseException;

import org.eclipse.jetty.toolchain.version.util.DateUtil;
import org.junit.Assert;
import org.junit.Test;

public class GitCommitTest
{
    @Test
    public void testParseAuthorDate() throws ParseException
    {
        String rawdate = "2011-05-12 09:02:36 +0000";
        GitCommit commit = new GitCommit();
        commit.parseAuthorDate(rawdate);

        String actual = DateUtil.formatRfc2822(commit.getAuthorDate());
        Assert.assertEquals("Author date","Thu, 12 May 2011 09:02:36 +0000",actual);
    }

    @Test
    public void testParseCommitterDate() throws ParseException
    {
        String rawdate = "2011-07-27 11:16:03 +1000";
        GitCommit commit = new GitCommit();
        commit.parseCommitterDate(rawdate);

        String actual = DateUtil.formatRfc2822(commit.getCommitterDate());
        Assert.assertEquals("Committer date","Wed, 27 Jul 2011 01:16:03 +0000",actual);
    }
}
