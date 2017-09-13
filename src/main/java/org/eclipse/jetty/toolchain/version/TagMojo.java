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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.jetty.toolchain.version.issues.Issue;
import org.eclipse.jetty.toolchain.version.issues.IssueComparator;

/**
 * Produce a target/version-tag.txt which represents the changes
 * for this particular release.  A file suitable to use as the git tag
 * message body, in tagging the release.
 *
 * @goal tag
 * @requiresProject true
 * @phase process-resources
 */
@SuppressWarnings("unused")
public class TagMojo extends UpdateVersionTextMojo
{
    /**
     * The generated version-tag.txt file.
     * <p/>
     *
     * @parameter property="version.tag.output.file" default="${project.build.directory}/version-tag.txt"
     */
    protected File versionTagOutputFile;
    
    /**
     * The generated version-tag.txt header text
     *
     * @parameter property="version.tag.header" default="Tag for release."
     */
    protected String versionTagHeader;
    
    @Override
    protected void updateVersionText(VersionText versionText, Release rel, String updateVersionText, String priorTagId, String priorCommitId, String currentCommitId) throws MojoFailureException, IOException
    {
        if (!hasCredentialsFile("tag"))
        {
            return; // skip
        }
        
        // List issues
        List<Issue> issues = new ArrayList<>();
        issues.addAll(rel.getIssues());
        Collections.sort(issues, new IssueComparator());
        
        try (FileWriter writer = new FileWriter(versionTagOutputFile);
             PrintWriter out = new PrintWriter(writer))
        {
            out.println(versionTagHeader);
            out.println();
            
            for (Issue issue : issues)
            {
                out.println(issue);
            }
        }
    }
}
