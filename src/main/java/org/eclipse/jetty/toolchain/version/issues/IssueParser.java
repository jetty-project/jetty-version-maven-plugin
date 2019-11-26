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

package org.eclipse.jetty.toolchain.version.issues;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.StringUtils;

public class IssueParser
{
    public static final String REGEX_ISSUE_BULLET = "^ [\\*\\+-] ";
    private static final IssuePatterns ISSUE_ID_PATTERNS;

    static
    {
        // Possible delimitors between issue id and text
        final String DELIM = "[-\\[\\]: ]*";

        ISSUE_ID_PATTERNS = new IssuePatterns();
        // Github Based
        ISSUE_ID_PATTERNS.add(IssueSyntax.GITHUB, "^[\\[\\s]*Issue #?([0-9]{2,})" + DELIM);
        ISSUE_ID_PATTERNS.add(IssueSyntax.GITHUB, "^[\\[\\s]*#?([0-9]{2,})" + DELIM);
        // Github recommended - https://help.github.com/articles/closing-issues-via-commit-messages/
        ISSUE_ID_PATTERNS.add(IssueSyntax.GITHUB_RECOMMENDED, "^[\\[\\s]*Close #([0-9]{2,})" + DELIM);
        ISSUE_ID_PATTERNS.add(IssueSyntax.GITHUB_RECOMMENDED, "^[\\[\\s]*Close[sd]* #([0-9]{2,})" + DELIM);
        ISSUE_ID_PATTERNS.add(IssueSyntax.GITHUB_RECOMMENDED, "^[\\[\\s]*Fix #([0-9]{1,})" + DELIM);
        ISSUE_ID_PATTERNS.add(IssueSyntax.GITHUB_RECOMMENDED, "^[\\[\\s]*Fixe[sd]* #([0-9]{2,})" + DELIM);
        ISSUE_ID_PATTERNS.add(IssueSyntax.GITHUB_RECOMMENDED, "^[\\[\\s]*Resolve #([0-9]{2,})" + DELIM);
        ISSUE_ID_PATTERNS.add(IssueSyntax.GITHUB_RECOMMENDED, "^[\\[\\s]*Resolve[sd]* #([0-9]{2,})" + DELIM);
        // Bugzilla Based
        ISSUE_ID_PATTERNS.add(IssueSyntax.BUGZILLA, "^[\\[\\s]*Bug ([0-9]{6,})" + DELIM);
        ISSUE_ID_PATTERNS.add(IssueSyntax.BUGZILLA, "^([0-9]{6,})" + DELIM);
        // Jira Based
        ISSUE_ID_PATTERNS.add(IssueSyntax.JIRA, "^[\\[\\s]*Bug (JETTY-[0-9]{2,})" + DELIM);
        ISSUE_ID_PATTERNS.add(IssueSyntax.JIRA, "(JETTY-[0-9]{2,})[^0-9]");
        // Bad
        ISSUE_ID_PATTERNS.add(IssueSyntax.BAD, "#([0-9]{2,})");
    }

    public IssueParser()
    {
    }

    /**
     * Parse a known issue (such as " + 341235 Bug Text Goes Here")
     *
     * @param rawissue the raw issue to parse
     * @return the issue (or null if raw issue is blank)
     */
    public Issue parseKnownIssue(String rawissue)
    {
        String raw = rawissue;

        // Eliminate known bullet types
        raw = raw.replaceFirst(REGEX_ISSUE_BULLET, "");
        if (StringUtils.isBlank(raw))
        {
            return null;
        }

        Issue issue = parsePossibleIssue(raw);
        if (issue != null)
        {
            return issue;
        }

        raw = raw.trim();

        String badId = raw.substring(0, Math.min(raw.length(), 70)).toLowerCase();
        issue = new Issue(badId, raw, IssueSyntax.BAD);
        return issue;
    }

    /**
     * Parse a possible issue, if provided line has no Issue ID pattern match, a null is returned.
     *
     * @param rawissue the raw issue text to parse
     * @return the parsed issue (or null if blank, or not matching a known issue pattern)
     */
    public Issue parsePossibleIssue(String rawissue)
    {
        if (StringUtils.isBlank(rawissue))
        {
            return null;
        }

        String subject = rawissue.trim();

        IssuePatterns.Match match = ISSUE_ID_PATTERNS.find(subject);
        if (match == null)
        {
            return null;
        }

        String id = match.group(1);
        if (match.syntax != IssueSyntax.BAD)
        {
            // Normal Syntax
            subject = subject.substring(match.end());
            subject = cleanSubjectLine(subject);
        }
        Issue issue = new Issue(id, subject, match.syntax);
        return issue;
    }

    /**
     * Parse a line, looking for possible issue ids.
     *
     * @param line the raw line of text to parse.
     * @param foundIssueIds the collection to store found issue Ids into.
     */
    public void findPossibleIssueIds(String line, Collection<String> foundIssueIds)
    {
        if (StringUtils.isBlank(line))
        {
            return;
        }

        foundIssueIds.addAll(ISSUE_ID_PATTERNS.findAllIds(line.trim()));
    }

    private String cleanSubjectLine(String subject)
    {
        if (subject.startsWith("- "))
        {
            subject = subject.substring(2);
        }

        Pattern endPunctuation = Pattern.compile("^(.*)\\s*[\\.!,]+\\s*$");
        Matcher mat = endPunctuation.matcher(subject);
        if (mat.matches())
        {
            subject = mat.group(1);
        }

        Pattern parenWrapped = Pattern.compile("^\\s*\\((.*)\\)\\s*$");
        mat = parenWrapped.matcher(subject);
        if (mat.matches())
        {
            subject = mat.group(1);
        }

        return subject.trim();
    }
}
