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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.StringUtils;

public class IssueParser
{
    public static final String REGEX_ISSUE_BULLET = "^ [\\*\\+-] ";
    private final IssuePatterns issue_id_patterns;
    
    public IssueParser()
    {
        // Possible delimitors between issue id and text
        String DELIM = "[-\\[\\]: ]*";
        
        issue_id_patterns = new IssuePatterns();
        // Github Based
        issue_id_patterns.add(IssueSyntax.GITHUB, "^[\\[\\s]*Issue #([0-9]{2,})" + DELIM);
        issue_id_patterns.add(IssueSyntax.GITHUB, "^[\\[\\s]*#([0-9]{2,})" + DELIM);
        // Github recommended - https://help.github.com/articles/closing-issues-via-commit-messages/
        issue_id_patterns.add(IssueSyntax.GITHUB_RECOMMENDED, "^[\\[\\s]*Close #([0-9]{2,})" + DELIM);
        issue_id_patterns.add(IssueSyntax.GITHUB_RECOMMENDED, "^[\\[\\s]*Close[sd]* #([0-9]{2,})" + DELIM);
        issue_id_patterns.add(IssueSyntax.GITHUB_RECOMMENDED, "^[\\[\\s]*Fix #([0-9]{2,})" + DELIM);
        issue_id_patterns.add(IssueSyntax.GITHUB_RECOMMENDED, "^[\\[\\s]*Fixe[sd]* #([0-9]{2,})" + DELIM);
        issue_id_patterns.add(IssueSyntax.GITHUB_RECOMMENDED, "^[\\[\\s]*Resolve #([0-9]{2,})" + DELIM);
        issue_id_patterns.add(IssueSyntax.GITHUB_RECOMMENDED, "^[\\[\\s]*Resolve[sd]* #([0-9]{2,})" + DELIM);
        // Bugzilla Based
        issue_id_patterns.add(IssueSyntax.BUGZILLA, "^[\\[\\s]*Bug ([0-9]{6,})" + DELIM);
        issue_id_patterns.add(IssueSyntax.BUGZILLA, "^([0-9]{6,})" + DELIM);
        // Jira Based
        issue_id_patterns.add(IssueSyntax.JIRA, "^[\\[\\s]*Bug (JETTY-[0-9]{2,})" + DELIM);
        issue_id_patterns.add(IssueSyntax.JIRA, "(JETTY-[0-9]{2,})[^0-9]");
        // Bad
        issue_id_patterns.add(IssueSyntax.BAD, "#([0-9]{2,})");
    }
    
    /**
     * Parse a known issue (such as " + 341235 Bug Text Goes Here")
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
     */
    public Issue parsePossibleIssue(String rawissue)
    {
        if (StringUtils.isBlank(rawissue))
        {
            return null;
        }
        
        String subject = rawissue.trim();
        
        IssuePatterns.Match match = issue_id_patterns.find(subject);
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
