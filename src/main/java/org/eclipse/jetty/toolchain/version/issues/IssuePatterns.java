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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class IssuePatterns
{
    public static class Match
    {
        private Matcher matcher;
        public IssueSyntax syntax;

        public String group(int group)
        {
            return matcher.group(group);
        }

        public int end()
        {
            return matcher.end();
        }
    }

    private static class Entry
    {
        IssueSyntax syntax;
        Pattern pattern;
    }

    private final List<Entry> list;

    public IssuePatterns()
    {
        list = new ArrayList<>();
    }

    public void add(IssueSyntax type, String regex)
    {
        Entry entry = new Entry();
        entry.syntax = type;
        entry.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        list.add(entry);
    }

    public Match find(String line)
    {
        Matcher mat;
        for (Entry entry : list)
        {
            mat = entry.pattern.matcher(line);
            if (mat.find())
            {
                Match ret = new Match();
                ret.matcher = mat;
                ret.syntax = entry.syntax;
                return ret;
            }
        }

        return null;
    }

    public Set<String> findAllIds(String line)
    {
        if (StringUtils.isBlank(line))
        {
            return Collections.emptySet();
        }

        Set<String> ids = new HashSet<>();
        Matcher mat;

        for (Entry entry : list)
        {
            mat = entry.pattern.matcher(line);
            int offset = 0;
            while (mat.find(offset))
            {
                ids.add(mat.group(1));
                offset = mat.end();
            }
        }

        return ids;
    }
}
