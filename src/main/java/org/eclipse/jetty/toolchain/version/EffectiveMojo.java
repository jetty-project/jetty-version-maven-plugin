package org.eclipse.jetty.toolchain.version;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.jetty.toolchain.version.issues.Issue;
import org.eclipse.jetty.toolchain.version.issues.IssueComparator;

/**
 * Process the commit log to produce output representing the changes
 * that would be applied to the VERSION.txt
 *
 * @goal effective
 * @requiresProject true
 * @phase process-resources
 */
@SuppressWarnings("unused")
public class EffectiveMojo extends UpdateVersionTextMojo
{
    /**
     * The generated version-tag.txt file.
     * <p/>
     *
     * @parameter property="version.tag.output.file"
     */
    private File versionTagOutputFile;
    
    /**
     * The genreated version-tag.txt header text
     *
     * @parameter property="version.tag.header"
     */
    private String versionTagHeader;
    
    @Override
    protected void updateVersionText(VersionText versionText, Release rel, String updateVersionText, String priorTagId, String priorCommitId, String currentCommitId) throws MojoFailureException, IOException
    {
        if (!hasCredentialsFile("effective"))
        {
            return; // skip
        }
        
        // List issues
        List<Issue> issues = new ArrayList<>();
        issues.addAll(rel.getIssues());
        Collections.sort(issues, new IssueComparator());
        
        System.out.printf("Update Version: %s%n", updateVersionText);
        System.out.printf("Changes from %s [%s]%n", priorTagId, priorCommitId);
        System.out.printf("          to %s%n", currentCommitId);
        System.out.println();
        
        for (Issue issue : issues)
        {
            System.out.println(issue);
        }
        
        if (versionTagOutputFile != null)
        {
            try (FileWriter writer = new FileWriter(versionTagOutputFile);
                 PrintWriter out = new PrintWriter(writer))
            {
                if (versionTagHeader != null)
                {
                    out.println(versionTagHeader);
                    out.println();
                }
                
                for (Issue issue : issues)
                {
                    out.println(issue);
                }
            }
        }
    }
}
