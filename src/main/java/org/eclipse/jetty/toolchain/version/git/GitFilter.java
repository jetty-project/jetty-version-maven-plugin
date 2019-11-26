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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GitFilter
{
    private List<Pattern> filenameExcludes = new ArrayList<>();

    public void addFilenameExclude(String excludePathRegex)
    {
        filenameExcludes.add(Pattern.compile(excludePathRegex));
    }

    public List<GitCommit> filter(List<GitCommit> commits)
    {
        List<GitCommit> results = new ArrayList<>();
        for (GitCommit commit : commits)
        {
            List<String> filteredFilenames = new ArrayList<>();

            for (String filename : commit.getFilenames())
            {
                boolean isExcluded = false;
                for (Pattern exclude : filenameExcludes)
                {
                    if (exclude.matcher(filename).matches())
                    {
                        isExcluded = true;
                        break;
                    }
                }
                if (!isExcluded)
                {
                    filteredFilenames.add(filename);
                }
            }

            if (filteredFilenames.size() > 0)
            {
                commit.setFilenames(filteredFilenames);
                results.add(commit);
            }
        }

        return results;
    }
}
