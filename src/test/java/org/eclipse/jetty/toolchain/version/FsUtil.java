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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jetty.toolchain.test.IO;

public final class FsUtil
{
    public static boolean existsAsFile(Path path)
    {
        return Files.exists(path) && Files.isRegularFile(path);
    }

    public static String toString(Path path) throws IOException
    {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             StringWriter writer = new StringWriter())
        {
            IO.copy(reader, writer);
            return writer.toString();
        }
    }
}
