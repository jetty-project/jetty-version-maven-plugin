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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * <p>Attach the VERSION.txt to the project.</p>
 * <p>Will only attach the VERSION.txt if it exists.</p>
 */
@SuppressWarnings("unused")
@Mojo( name = "attach-version-text", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class AttachVersionMojo extends AbstractVersionMojo
{
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (!hasVersionTextFile("attach-version-text"))
        {
            return; // skip
        }

        projectHelper.attachArtifact(project,type,classifier,versionTextInputFile);
    }
}
