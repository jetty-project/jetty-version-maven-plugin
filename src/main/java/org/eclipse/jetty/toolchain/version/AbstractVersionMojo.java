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
import java.nio.file.Files;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

public abstract class AbstractVersionMojo extends AbstractMojo
{
    /**
     * The project basedir.
     */
    @Parameter(required = true, defaultValue = "${project.basedir}")
    protected File basedir;

    /**
     * The existing VERSION.txt file.
     */
    @Parameter(property = "version.text.file", defaultValue = "${project.basedir}/VERSION.txt")
    protected File versionTextInputFile;

    /**
     * The classifier to use for attaching the generated VERSION.txt artifact
     */
    @Parameter(property = "version.text.output.classifier", defaultValue = "version")
    protected String classifier = "version";

    /**
     * Credentials directory for github issue resolver
     */
    @Parameter(property = "version.text.credential.file", defaultValue = "${user.home}/.github")
    protected File credentialsFile;

    /**
     * The type to use for the attaching the generated VERSION.txt artifact
     */
    @Parameter(property = "version.text.output.type", defaultValue = "txt")
    protected String type = "txt";

    /**
     * The git commit filename exclusions (regex form)
     */
    @Parameter(property = "version.text.filename.excludes")
    protected String[] filenameExcludes;

    /**
     * Maven ProjectHelper. (internal component)
     */
    @Component
    protected MavenProjectHelper projectHelper;

    /**
     * The github repository name.
     */
    @Parameter(property = "version.github.repoName", defaultValue = "eclipse/jetty.project")
    protected String repoName;

    /**
     * Maven Project.
     */
    @Component
    protected MavenProject project;

    /**
     * The version key to use in the VERSION.txt file.
     */
    @Parameter(required = true, property = "version.text.key", defaultValue = "jetty-VERSION")
    protected String versionTextKey;

    /**
     * The version key to use when looking up a git tag ref.
     */
    @Parameter(required = true, property = "version.tag.key", defaultValue = "jetty-VERSION")
    protected String versionTagKey;

    protected void ensureDirectoryExists(File dir) throws MojoFailureException
    {
        if (Files.exists(dir.toPath()) && Files.isDirectory(dir.toPath()))
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
            getLog().warn("Skipping :" + goal + " - the ${user.home}/.github file was not specified.");
            return false; // skipping build
        }

        if (!credentialsFile.exists())
        {
            getLog().warn("Skipping :" + goal + " - file not found: " + credentialsFile.getAbsolutePath());
            return false; // skipping build
        }

        return true;
    }
}
