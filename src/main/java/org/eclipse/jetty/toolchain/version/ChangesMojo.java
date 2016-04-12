package org.eclipse.jetty.toolchain.version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.eclipse.jetty.toolchain.version.git.GitCommand;
import org.eclipse.jetty.toolchain.version.issues.Issue;
import org.eclipse.jetty.toolchain.version.issues.IssueComparator;

/**
 * Attach the VERSION.txt to the project.
 * <p>
 * Will only attach the VERSION.txt if it exists.
 *
 * @goal changes
 * @requiresProject true
 * @phase process-resources
 */
public class ChangesMojo extends UpdateVersionTextMojo
{
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (!hasVersionTextFile("changes"))
        {
            return; // skip
        }

        try
        {
            // Pattern used in VERSION.txt
            VersionPattern verTextPattern = new VersionPattern(versionTextKey);
            // Pattern used in Git Tags
            VersionPattern verTagPattern = new VersionPattern(versionTagKey);

            VersionText versionText = new VersionText(verTextPattern);
            versionText.read(versionTextInputFile);

            String updateVersionText = verTextPattern.toVersionId(version);
            String updateVersionGit = verTagPattern.toVersionId(version);
            getLog().debug("raw version = " + version);
            getLog().debug("updateVersionText (as it appears in VERSION.txt) = " + updateVersionText);
            getLog().debug("updateVersionGit (as it appears to git tags) = " + updateVersionGit);

            Release rel = versionText.findRelease(updateVersionText);
            if (rel == null)
            {
                // Not found, create a new one
                rel = new Release(updateVersionText);
            }

            String priorTextVersion = versionText.getPriorVersion(updateVersionText);
            if (priorTextVersion == null)
            {
                // Assume its the top of the file.
                priorTextVersion = versionText.getReleases().get(0).getVersion();
            }
            getLog().debug("Prior version in VERSION.txt is " + priorTextVersion);

            GitCommand git = new GitCommand();
            git.setWorkDir(basedir);
            git.setLog(getLog());

            if (refreshTags)
            {
                getLog().info("Fetching git tags from remote ...");
                if (!git.fetchTags())
                {
                    throw new MojoFailureException("Unable to fetch git tags?");
                }
            }

            // Make sure its an expected version identifier
            if (!verTextPattern.isMatch(priorTextVersion))
            {
                StringBuilder err = new StringBuilder();
                err.append("Prior version [").append(priorTextVersion);
                err.append("] is not a valid version identifier.");
                err.append(" Does not conform to expected pattern [");
                err.append(versionTextKey).append("]");
                throw new MojoExecutionException(err.toString());
            }

            // Make it conform to git tag version identifiers
            String priorGitVersion = verTextPattern.getLastVersion(versionTagKey);
            String priorTagId = git.findTagMatching(priorGitVersion);
            if (priorTagId == null)
            {
                getLog().warn("Unable to find git tag id for prior version id [" + priorGitVersion + "] (defined in VERSION.txt as [" + priorTextVersion + "])");
                return;
            }
            getLog().debug("Tag for prior version [" + priorGitVersion + "] is " + priorTagId);

            String priorCommitId = git.getTagCommitId(priorTagId);
            getLog().debug("Commit ID from [" + priorTagId + "]: " + priorCommitId);

            String currentCommitId = "HEAD";
            if (refreshTags)
            {
                String currentTagId = git.findTagMatching(updateVersionText);
                if (currentTagId != null)
                {
                    currentCommitId = git.getTagCommitId(currentTagId);
                }
            }
            getLog().debug("Commit ID to [" + updateVersionText + "]: " + currentCommitId);

            git.populateIssuesForRange(priorCommitId,currentCommitId,rel);

            resolveIssueSubjects(rel);

            // List issues
            List<Issue> issues = new ArrayList<>();
            issues.addAll(rel.getIssues());
            Collections.sort(issues, new IssueComparator());

            System.out.printf("Changes from %s [%s]%n", priorTagId, priorCommitId);
            System.out.printf("          to %s%n", currentCommitId);
            System.out.println();

            for(Issue issue: issues)
            {
                System.out.println(issue);
            }
        }
        catch (IOException e)
        {
            throw new MojoFailureException("Unable to obtain changes",e);
        }

    }
}
