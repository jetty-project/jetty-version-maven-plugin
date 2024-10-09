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
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.emory.mathcs.backport.java.util.Collections;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.jetty.toolchain.version.issues.Issue;
import org.eclipse.jetty.toolchain.version.issues.IssueComparator;
import org.eclipse.jetty.toolchain.version.issues.IssueParser;
import org.eclipse.jetty.toolchain.version.util.VersionUtil;

public class Release
{
    private static final List<String> RELEASED_ON_FORMATS;

    static
    {
        RELEASED_ON_FORMATS = new ArrayList<>();
        RELEASED_ON_FORMATS.add("M/d/yyyy"); // USA Format (shorthand)
        RELEASED_ON_FORMATS.add("EEE d MMMM yyyy"); // USA Format w/Weekday
        RELEASED_ON_FORMATS.add("d MMMM yyyy"); // USA Format
        RELEASED_ON_FORMATS.add("MMMM d yyyy"); // Month Day Year
        RELEASED_ON_FORMATS.add("MMMM yyyy"); // Month Year
    }

    private boolean existing = false;
    private List<Issue> issues = new ArrayList<>();
    private Date releasedOn;
    private String version;

    public Release()
    {
        /* anonymous version */
    }

    public Release(String ver)
    {
        this.version = ver;
    }

    public void addIssue(Issue issue)
    {
        if (issue == null)
        {
            return;
        }

        if (!issues.contains(issue))
        {
            this.issues.add(issue);
        }
    }

    public void addIssues(List<Issue> moreIssues)
    {
        for (Issue issue : moreIssues)
        {
            addIssue(issue);
        }
    }

    public void addIssueIfNotExists(Issue incomingIssue)
    {
        Issue existingIssue = null;
        for (Issue issue : issues)
        {
            if (issue.getId().equals(incomingIssue.getId()))
            {
                existingIssue = issue;
            }
        }

        if (existingIssue == null)
            addIssue(incomingIssue);
    }

    public void dropIssue(Issue issue)
    {
        this.issues.remove(issue);
    }

    public void dropIssues(List<Issue> dropIssues)
    {
        for (Issue issue : dropIssues)
        {
            dropIssue(issue);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        Release other = (Release)obj;
        if (version == null)
        {
            if (other.version != null)
            {
                return false;
            }
        }
        else if (!version.equals(other.version))
        {
            return false;
        }
        return true;
    }

    public List<Issue> getIssues()
    {
        return issues;
    }

    public Date getReleasedOn()
    {
        return releasedOn;
    }

    public String getReleasedOnString()
    {
        if (releasedOn == null)
            return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
        return sdf.format(releasedOn);
    }

    public String getReleasedOnISOString()
    {
        if (releasedOn == null)
            return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(releasedOn);
    }

    public List<Issue> getSortedIssues()
    {
        Collections.sort(issues, new IssueComparator());
        return issues;
    }

    public String getVersion()
    {
        return version;
    }

    public int getMajorVersion()
    {
        return VersionUtil.parseMajorVersion(version);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    public boolean isExisting()
    {
        return existing;
    }

    public void mergeIssues(List<Issue> incomingIssues)
    {
        for (Issue incomingIssue : incomingIssues)
        {
            addIssueIfNotExists(incomingIssue);
        }
    }

    public void parseReleasedOn(int linenum, String rawdateStr)
    {
        if (StringUtils.isBlank(rawdateStr))
        {
            releasedOn = null;
            return;
        }

        String rawdate = rawdateStr.trim();
        if (rawdate.startsWith("- "))
        {
            rawdate = rawdate.substring(2);
        }

        // Strip Ordinals
        String[] ordinals = new String[]{"st", "nd", "rd", "th"};
        String simp;
        Pattern ordPat;
        Matcher mat;
        for (String ordinal : ordinals)
        {
            ordPat = Pattern.compile("[0-9]" + ordinal);
            mat = ordPat.matcher(rawdate);
            if (mat.find())
            {
                simp = rawdate.substring(0, mat.start()) + rawdate.charAt(mat.start()) + rawdate.substring(mat.end());
                rawdate = simp;
            }
        }

        // Attempt to parse date
        SimpleDateFormat sdf;
        for (String format : RELEASED_ON_FORMATS)
        {
            sdf = new SimpleDateFormat(format);
            sdf.setDateFormatSymbols( DateFormatSymbols.getInstance( Locale.US ) );
            try
            {
                releasedOn = sdf.parse(rawdate);
                if (releasedOn != null)
                {
                    return; // Got a valid date.
                }
            }
            catch (ParseException ignore)
            {
                /* ignore */
            }
        }

        if (releasedOn == null)
        {
            System.err.printf("ERROR: Unable to parse raw date string [%s] on line #%d%n", rawdate, linenum);
        }
    }

    public void setExisting(boolean existing)
    {
        this.existing = existing;
    }

    public void setIssues(List<Issue> issues)
    {
        this.issues.clear();
        this.issues.addAll(issues);
    }

    public void setReleasedOn(Date releasedOn)
    {
        this.releasedOn = releasedOn;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public static Release readAsText(Path file) throws IOException
    {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
        {
            return readAsText(reader);
        }
    }

    public static Release readAsText(BufferedReader reader) throws IOException
    {
        VersionPattern versionPattern = VersionPattern.ECLIPSE;
        Pattern patBullet = Pattern.compile(IssueParser.REGEX_ISSUE_BULLET);
        Matcher mat;

        Release release = null;
        String line = reader.readLine();

        // First line should always be the release version & date
        if (versionPattern.isMatch(line))
        {
            // Build a clean and consistent version string
            String cleanVersion = versionPattern.getLastVersion();
            release = new Release(cleanVersion);
            release.setExisting(true);

            String on = versionPattern.getRemainingText();
            release.parseReleasedOn(1, on);
        }
        else
        {
            throw new IOException("Not a valid Release notes (missing release version & date)");
        }

        Issue issue = null;
        IssueParser issueParser = new IssueParser();

        // All other lines are the changes.
        while ((line = reader.readLine()) != null)
        {
            if (StringUtils.isBlank(line))
                continue; // skip

            mat = patBullet.matcher(line);
            if (mat.find())
            {
                if (issue != null)
                    release.addIssue(issue);
                // Start of an issue text.
                issue = issueParser.parseKnownIssue(line);
            }
            else // handle multi-line issue text
            {
                if (issue == null)
                    issue = issueParser.parseKnownIssue(line);
                else
                    issue.appendText(line);
            }
        }
        if (issue != null)
            release.addIssue(issue);
        return release;
    }

    public void writeAsText(PrintWriter out)
    {
        out.print(getVersion());
        if (getReleasedOn() != null)
        {
            out.print(" - ");
            out.append(getReleasedOnString());
        }
        out.print('\n');

        for (Issue issue: getSortedIssues())
        {
            out.print(issue.toString());
            out.print('\n');
        }
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("Release[");
        buf.append("version=").append(version);
        buf.append(",releasedOn=");
        if (releasedOn == null)
        {
            buf.append("<null>");
        }
        else
        {
            buf.append(getReleasedOnString());
        }
        buf.append(",issues.size=").append(issues.size());
        buf.append("]");
        return buf.toString();
    }

    public static class ReleaseDateComparator implements Comparator<Release>
    {
        @Override
        public int compare(Release r1, Release r2)
        {
            if (r1.getVersion().contains("SNAPSHOT"))
                return 1;
            if (r2.getVersion().contains("SNAPSHOT"))
                return -1;
            int diff = r1.getReleasedOnISOString().compareTo(r2.getReleasedOnISOString());
            if (diff != 0)
                return diff;
            return r1.getVersion().compareTo(r2.getVersion());
        }
    }
}
