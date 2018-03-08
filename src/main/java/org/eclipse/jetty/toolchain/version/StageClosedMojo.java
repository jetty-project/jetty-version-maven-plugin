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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jetty.toolchain.version.issues.GitHubIssueResolver;
import org.eclipse.jetty.toolchain.version.issues.IssueSyntax;
import org.kohsuke.github.GHIssue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Add comment in all github issues found in VERSION.txt which says
 * This issue is now available for testing in staged release [jetty-version] available in staging repository [url]
 */
@Mojo( name = "stage-closed", threadSafe = true)
public class StageClosedMojo
    extends AbstractVersionMojo
{

    @Parameter( required = true, property = "version.stageRepositoryUrl" )
    private String stageRepositoryUrl;

    @Parameter( required = true, property = "version.jettyVersion", defaultValue = "${project.version}" )
    private String jettyVersion;

    @Parameter( required = true, property = "version.stageClosed.comment", defaultValue =
        "This issue is now available for testing in staged release " //
            + " ${jettyVersion} available in staging repository ${stageRepositoryUrl}" )
    private String comment;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            getLog().debug( "jettyVersion:" + jettyVersion );
            VersionPattern verTextPattern = new VersionPattern( versionTextKey );

            VersionText versionText = new VersionText( verTextPattern );
            versionText.read( versionTextInputFile );

            getLog().debug( "versionList:" + versionText.getVersionList() );

            Optional<Release> releaseOptional = versionText.getReleases().stream() //
                .filter( release -> StringUtils.equalsIgnoreCase( release.getVersion(), jettyVersion ) //
                    || StringUtils.startsWith( release.getVersion(), "jetty-" + jettyVersion ) ) //
                .findFirst();

            if ( !releaseOptional.isPresent() )
            {
                getLog().info( "cannot find any release in VERSION.TXT with version " + jettyVersion );
                return;
            }

            Map<String, String> interpolatedValues = new HashMap<>( 2 );
            interpolatedValues.put( "jettyVersion", jettyVersion );
            interpolatedValues.put( "stageRepositoryUrl", stageRepositoryUrl );

            String resolvedComment = StrSubstitutor.replace( comment, interpolatedValues );

            GitHubIssueResolver gitHubIssueResolver = new GitHubIssueResolver( repoName ).init( getLog() );

            releaseOptional.get().getIssues().stream() //
                .filter( issue -> issue.getSyntax() == IssueSyntax.GITHUB ) //
                .forEach( issue -> {
                    try
                    {
                        GHIssue ghIssue = gitHubIssueResolver.getIssue( issue.getId() );
                        ghIssue.comment( resolvedComment );
                    }
                    catch ( IOException e )
                    {
                        getLog().warn( "fail to comment issue: " + issue.getId() );
                    }
                } );

        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
