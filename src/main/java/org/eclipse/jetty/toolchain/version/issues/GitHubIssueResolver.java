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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.OkUrlFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.OkHttp3Connector;

// TODO turn it as a component
public class GitHubIssueResolver
{
    private Log log;
    private final String repoName;
    private Path cacheDirectory;
    private GitHub github;

    public GitHubIssueResolver(String repoName)
    {
        this.repoName = repoName;
    }

    public GitHubIssueResolver init(Log log) throws IOException
    {

        this.log = log;
        Path userHome = new File(System.getProperty("user.home")).toPath();
        cacheDirectory = userHome.resolve(".cache/github/jetty");
        if (!Files.exists(cacheDirectory))
        {
            Files.createDirectories(cacheDirectory);
        }

        Cache cache = new Cache(cacheDirectory.toFile(), 10 * 1024 * 1024); // 10MB cache

        this.github = GitHubBuilder.fromCredentials()
            .withConnector(new OkHttp3Connector(new OkUrlFactory(new OkHttpClient.Builder().cache(cache).build())))
            .build();

        // Test access
        if (!this.github.isCredentialValid())
        {
            this.github = null;
            throw new IOException("Unable to access github, invalid credentials in ~/.github ?");
        }

        // list current rate limits
        log.info("Github API Rate Limits: " + this.github.getRateLimit().toString());
        return this;
    }

    public GHIssue getIssue(String issueRef) throws IOException
    {
        int issueNum = Integer.parseInt(issueRef);

        try
        {
            return github.getRepository(repoName).getIssue(issueNum);
        }
        catch (FileNotFoundException fnfe)
        {
            log.warn("error find issue with ref: " + issueRef, fnfe);
            return null;
        }
    }

    /**
     * @param milestone the milestone to create (if not present)
     * @return <code>null</code> if the milestone has not been created
     * @throws IOException if unable to create milestone
     */
    public GHMilestone createMilestone(String milestone) throws IOException
    {
        GHRepository ghRepository = github.getRepository(repoName);
        List<GHMilestone> ghMilestones = ghRepository.listMilestones(GHIssueState.ALL).asList();
        Optional<GHMilestone> ghMilestoneOptional = ghMilestones.stream() //
            .filter(ghMilestone -> StringUtils.equalsIgnoreCase(milestone, ghMilestone.getTitle())) //
            .findFirst();
        if (ghMilestoneOptional.isPresent())
        {
            return ghMilestoneOptional.get();
        }
        GHMilestone ghMilestone = ghRepository
            .createMilestone(milestone, "Milestone for version " + milestone);
        return ghMilestone;
    }

    public void assignMillestone(GHMilestone ghMilestone, GHIssue ghIssue)
    {
        // edit("milestone",milestone.getNumber());
        // new Requester( root)._with( key, value).method( "PATCH").to( getIssuesApiRoute());
        //github.getConnector().
    }

    public void destroy()
    {
        try
        {
            if (github != null)
                log.info("Github Rate Limit: " + github.getRateLimit());
        }
        catch (IOException ignore)
        {
            // ignore
        }
    }
}
