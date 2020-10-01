/*
 *  ========================================================================
 *  Copyright (c) 1995-2017 Mort Bay Consulting Pty. Ltd.
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

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jetty.toolchain.version.issues.GitHubIssueResolver;
import org.eclipse.jetty.toolchain.version.issues.IssueSyntax;
import org.kohsuke.github.GHMilestone;

/**
 * Add comment in all github issues found in VERSION.txt which says
 * This issue is now available for testing in staged release [jetty-version] available in staging repository [url]
 */
@Mojo(name = "stage-released", threadSafe = true)
public class StageReleasedMojo
    extends AbstractVersionMojo
{

    @Parameter(required = true, property = "version.jettyVersion", defaultValue = "${project.version}")
    private String jettyVersion;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            getLog().debug("jettyVersion:" + jettyVersion);
            VersionPattern verTextPattern = new VersionPattern(versionTextKey);

            VersionText versionText = new VersionText(verTextPattern);
            versionText.read(versionTextInputFile.toPath());

            getLog().debug("versionList:" + versionText.getVersionList());

            GitHubIssueResolver gitHubIssueResolver = new GitHubIssueResolver(repoName)
                .init(getLog());

            GHMilestone ghMilestone = gitHubIssueResolver.createMilestone(jettyVersion);
            getLog().info("ghMilestone: " + ghMilestone);

            Optional<Release> releaseOptional = versionText.getReleases().stream()
                .filter(release -> StringUtils.equalsIgnoreCase(release.getVersion(), jettyVersion))
                .findFirst();

            if (!releaseOptional.isPresent())
            {
                getLog().info("cannot find any release in VERSION.TXT with version " + jettyVersion);
                return;
            }

            releaseOptional.get().getIssues().stream() //
                .filter(issue -> issue.getSyntax() == IssueSyntax.GITHUB) //
                .forEach(issue ->
                {
                    //GHIssue ghIssue = gitHubIssueResolver.getIssue( issue.getId() );

                });
        }
        catch (Exception e)
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
