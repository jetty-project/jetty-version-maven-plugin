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

import java.text.CollationKey;
import java.text.Collator;
import java.util.Comparator;

public class IssueComparator implements Comparator<Issue>
{
    private final Collator collator = Collator.getInstance();

    public int compare(Issue o1, Issue o2)
    {
        CollationKey key1 = toKey(o1);
        CollationKey key2 = toKey(o2);
        return key1.compareTo(key2);
    }

    private CollationKey toKey(Issue issue)
    {
        if ((issue == null) || (issue.getId() == null))
        {
            return collator.getCollationKey("");
        }

        String id = issue.getId();

        if (issue.getId().startsWith("JETTY-"))
        {
            id = issue.getId().substring("JETTY-".length());
        }

        try
        {
            Long numId = Long.parseLong(id);
            return collator.getCollationKey(String.format("%010d", numId));
        }
        catch (NumberFormatException e)
        {
            return collator.getCollationKey(issue.getId());
        }
    }
}
