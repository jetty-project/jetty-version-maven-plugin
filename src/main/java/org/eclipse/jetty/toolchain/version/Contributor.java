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

import java.util.Objects;

public class Contributor
{
    private final String githubId;

    private final String githubName;

    public Contributor( String githubId, String githubName )
    {
        this.githubId = githubId;
        this.githubName = githubName;
    }

    public String getGithubId()
    {
        return githubId;
    }

    public String getGithubName()
    {
        return githubName;
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
        Contributor that = (Contributor) o;
        return Objects.equals( githubId, that.githubId );
    }

    @Override
    public int hashCode()
    {

        return Objects.hash( githubId );
    }

    @Override
    public String toString()
    {
        return "Contributor{" + "githubId='" + githubId + '\'' + ", githubName='" + githubName + '\'' + '}';
    }
}
