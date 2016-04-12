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

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.logging.Log;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.OkHttpConnector;

public class GitHubIssueResolver implements IssueResolver
{
    private Log log;
    // TODO: make repo name configurable
    private String repoName = "eclipse/jetty.project";
    private Path cacheDirectory;
    private GitHub github;

    public void init(Log log) throws IOException
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
                .withConnector(new OkHttpConnector(new OkUrlFactory(new OkHttpClient().setCache(cache))))
                .build();

        // Test access
        if (!this.github.isCredentialValid())
        {
            this.github = null;
            throw new IOException("Unable to access github, invalid credentials in ~/.github ?");
        }
    }

    @Override
    public String getIssueSubject(String issueRef) throws IOException
    {
        int issueNum = Integer.parseInt(issueRef);

        GHIssue issue = github.getRepository(repoName).getIssue(issueNum);
        if (issue == null)
        {
            return null;
        }

        return issue.getTitle();
    }

    @Override
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
