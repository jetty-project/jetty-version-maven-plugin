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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IssueParserTest
{
    public static Stream<Arguments> issueSamples()
    {
        List<Arguments> cases = new ArrayList<>();
        cases.add(Arguments.of(" + Fixed client abort", null, "Fixed client abort"));
        cases.add(Arguments.of(" - Missing request dispatchers", null, "Missing request dispatchers"));
        cases.add(Arguments.of(" * Totally rearchitected and rebuilt", null, "Totally rearchitected and rebuilt"));
        cases.add(Arguments.of(" + JETTY-171 Fixed filter mapping", "JETTY-171", "Fixed filter mapping"));
        cases.add(Arguments.of("391623 Add option to --stop to wait for target jetty to stop", "391623", "Add option to --stop to wait for target jetty to stop"));
        cases.add(Arguments.of("388079: AbstractHttpConnection. Flush the buffer before shutting output down on error condition", "388079", "AbstractHttpConnection. Flush the buffer before shutting output down on error condition"));
        cases.add(Arguments.of("[Bug 388073] null session id from cookie causes NPE fixed", "388073", "null session id from cookie causes NPE fixed"));
        cases.add(Arguments.of("Bug 391588 - WebSocket Client does not set masking on close frames", "391588", "WebSocket Client does not set masking on close frames"));
        cases.add(Arguments.of("393385: Make hostname verification configurable in SslContextFactory and enable it by default (See http://www.ietf.org/rfc/rfc2818.txt section 3.1)",
            "393385",
            "Make hostname verification configurable in SslContextFactory and enable it by default (See http://www.ietf.org/rfc/rfc2818.txt section 3.1)"));
        cases.add(Arguments.of("+ Fixed JETTY-68. Complete request after sendRedirect", "JETTY-68", "Complete request after sendRedirect"));
        // Github specific
        cases.add(Arguments.of("Issue #354 (Spin loop in case of exception thrown during accept()).",
            "354", "Spin loop in case of exception thrown during accept()"));
        cases.add(Arguments.of("Issue #84 Ignored test", "84", "Ignored test"));
        // Bad git subject line issue
        cases.add(Arguments.of("Issue #123: foo Issue #124: bar",
            "123", "foo Issue #124: bar"));
        // Empty git subject line issue
        cases.add(Arguments.of("fix #592", "592", ""));

        return cases.stream();
    }

    @ParameterizedTest
    @MethodSource("issueSamples")
    public void assertKnownIssue(String rawline, String expectedID, String expectedText)
    {
        IssueParser issueParser = new IssueParser();
        Issue issue = issueParser.parseKnownIssue(rawline);
        assertNotNull(issue, "Should always result in a issue");
        if (expectedID == null)
        {
            assertThat("issue.syntax", issue.getSyntax(), is(IssueSyntax.BAD));
        }
        else
        {
            assertThat("issue.syntax", issue.getSyntax(), is(not(IssueSyntax.BAD)));
            assertThat("issue.id", issue.getId(), is(expectedID));
        }
        assertThat("issue.text", issue.getText(), is(expectedText));
    }
}
