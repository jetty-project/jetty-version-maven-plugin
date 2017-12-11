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

import java.util.Objects;

public class PullRequest
{
    private final String pullRequestId;
    private final String content;
    private final Contributor contributor;

    public PullRequest( String pullRequestId, String content, Contributor contributor )
    {
        this.pullRequestId = pullRequestId;
        this.content = content;
        this.contributor = contributor;
    }

    public String getContent()
    {
        return content;
    }

    public Contributor getContributor()
    {
        return contributor;
    }

    public String getPullRequestId()
    {
        return pullRequestId;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        PullRequest that = (PullRequest) o;
        return Objects.equals( pullRequestId, that.pullRequestId );
    }

    @Override
    public int hashCode()
    {

        return Objects.hash( pullRequestId );
    }

    @Override
    public String toString()
    {
        return "PullRequest{" + "pullRequestId='" + pullRequestId + '\'' + ", content='" + content + '\''
            + ", contributor=" + contributor + '}';
    }

    public String asText( boolean displayContributor )
    {
        StringBuilder line = new StringBuilder();
        line.append( " + pr " );
        line.append( pullRequestId ).append( ' ' ) //
            .append( content );
        if ( displayContributor )
        {
            line.append( " by " ) //
                .append( StringUtils.isEmpty( contributor.getGithubName() ) ? "" : contributor.getGithubName() ) //
                .append( " (" ).append( contributor.getGithubId() ).append( ')' );
        }
        return line.toString();
    }
}
