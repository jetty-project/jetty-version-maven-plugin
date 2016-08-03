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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

public abstract class AbstractVersionMojo extends AbstractMojo
{
    /**
     * The project basedir.
     *
     * @parameter property="project.basedir"
     * @required
     */
    protected File basedir;

    /**
     * The existing VERSION.txt file.
     * <p>
     *
     * @parameter property="version.text.file" default-value="${project.basedir}/VERSION.txt"
     */
    protected File versionTextInputFile;

    /**
     * The classifier to use for attaching the generated VERSION.txt artifact
     *
     * @parameter property="version.text.output.classifier" default-value="version"
     */
    protected String classifier = "version";

    /**
     * Credentials directory for github issue resolver
     *
     * @parameter property="version.text.credential.file" default-value="${user.home}/.github"
     */
    protected File credentialsFile;

    /**
     * The type to use for the attaching the generated VERSION.txt artifact
     *
     * @parameter property="version.text.output.type" default-value="txt"
     */
    protected String type = "txt";

    /**
     * Maven ProjectHelper. (internal component)
     *
     * @component
     * @readonly
     * @required
     */
    protected MavenProjectHelper projectHelper;

    /**
     * Maven Project.
     *
     * @parameter property="project"
     * @readonly
     * @required
     */
    protected MavenProject project;

    protected void ensureDirectoryExists(File dir) throws MojoFailureException
    {
        if (dir.exists() && dir.isDirectory())
        {
            return; // done
        }

        if (dir.mkdirs() == false)
        {
            throw new MojoFailureException("Unable to create directory: " + dir.getAbsolutePath());
        }
    }

    protected boolean hasVersionTextFile(String goal)
    {
        if (versionTextInputFile == null)
        {
            getLog().debug("Skipping :" + goal + " - the <versionTextInputFile> was not specified.");
            return false; // skipping build,
        }

        if (!versionTextInputFile.exists())
        {
            getLog().debug("Skipping :" + goal + " - file not found: " + versionTextInputFile.getAbsolutePath());
            return false; // skipping build,
        }

        return true;
    }

    protected boolean hasCredentialsFile(String goal)
    {
        if (credentialsFile == null)
        {
            getLog().debug("Skipping :" + goal + " - the ${user.home}/.github file was not specified.");
            return false; // skipping build
        }

        if (!credentialsFile.exists())
        {
            getLog().debug("Skipping :" + goal + " - file not found: " + credentialsFile.getAbsolutePath());
            return false; // skipping build
        }

        return true;
    }
}
