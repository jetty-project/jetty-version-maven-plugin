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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jetty.toolchain.version.issues.Issue;
import org.eclipse.jetty.toolchain.version.issues.IssueComparator;

/**
 * Process the commit log to produce output representing the changes
 * that would be applied to the VERSION.txt
 */
@SuppressWarnings("unused")
@Mojo( name = "effective", requiresProject = true, defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class EffectiveMojo extends UpdateVersionTextMojo
{
    @Override
    protected void updateVersionText(VersionText versionText, Release rel, String updateVersionText, String priorTagId, String priorCommitId, String currentCommitId) throws MojoFailureException, IOException
    {
        if (!hasCredentialsFile("effective"))
        {
            return; // skip
        }
    
        // List issues
        List<Issue> issues = new ArrayList<>();
        issues.addAll(rel.getIssues());
        Collections.sort(issues, new IssueComparator());
    
        System.out.printf("Update Version: %s%n", updateVersionText);
        System.out.printf("Changes from %s [%s]%n", priorTagId, priorCommitId);
        System.out.printf("          to %s%n", currentCommitId);
        System.out.println();

        issues.stream().forEach( issue -> System.out.println(issue) );
    }
}
