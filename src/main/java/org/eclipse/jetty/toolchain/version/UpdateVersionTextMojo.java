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
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jetty.toolchain.version.git.GitCommand;
import org.eclipse.jetty.toolchain.version.issues.GitHubIssueResolver;
import org.eclipse.jetty.toolchain.version.issues.Issue;
import org.eclipse.jetty.toolchain.version.issues.IssueSyntax;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;

/**
 * Update the active version entry in the VERSION.txt file from information present in the git logs.
 *
 * @goal update-version-text
 * @requiresProject true
 * @phase package
 */
public class UpdateVersionTextMojo extends AbstractVersionMojo
{
    /**
     * The maven project version.
     *
     * @parameter property="version.section" default-value="${project.version}"
     * @required
     */
    protected String version;
    
    /**
     * The version key to use in the VERSION.txt file.
     *
     * @parameter property="version.text.key" default-value="jetty-VERSION"
     * @required
     */
    protected String versionTextKey;
    
    /**
     * The version key to use when looking up a git tag ref.
     *
     * @parameter property="version.tag.key" default-value="jetty-VERSION"
     * @required
     */
    protected String versionTagKey;
    
    /**
     * Allow the existing issues to be sorted alphabetically.
     *
     * @parameter property="version.sort.existing" default-value="false"
     */
    private boolean sortExisting = false;
    
    /**
     * Allow the plugin to issue a 'git fetch --tags' to update the local tags from.
     *
     * @parameter property="version.refresh.tags" default-value="false"
     */
    protected boolean refreshTags = false;
    
    /**
     * Allow the plugin to update the release date for an issue (if none is provided)
     *
     * @parameter property="version.update.date" default-value="false"
     */
    private boolean updateDate = false;
    
    /**
     * Allow the plugin to replace the input VERSION.txt file
     *
     * @parameter property="version.copy.generated" default-value="false"
     */
    private boolean copyGenerated;
    
    /**
     * Allow the plugin to attach the generated VERSION.txt file to the project
     *
     * @parameter property="version.attach" default-value="false"
     */
    private boolean attachArtifact;
    
    /**
     * The generated VERSION.txt file.
     * <p/>
     *
     * @parameter property="version.text.output.file" default-value="${project.build.directory}/VERSION.txt"
     */
    private File versionTextOuputFile;
    
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (!hasVersionTextFile("gen-version-text"))
        {
            return; // skip
        }

        if (!hasCredentialsFile("gen-version-text"))
        {
            return; //skip
        }
        
        try
        {
            String commitMessage = "Updating VERSION.txt";
            
            // Pattern used in VERSION.txt
            VersionPattern verTextPattern = new VersionPattern(versionTextKey);
            // Pattern used in Git Tags
            VersionPattern verTagPattern = new VersionPattern(versionTagKey);
            
            VersionText versionText = new VersionText(verTextPattern);
            versionText.read(versionTextInputFile);
            versionText.setSortExisting(sortExisting);
            
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
                getLog().debug("Not Found, creating new rel = " + rel);
            }
            else
            {
                getLog().debug("Using existing rel = " + rel);
            }
            
            getLog().info("Updating version section: " + version);
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
            String currentCommitId = "HEAD";
            if (priorTagId == null)
            {
                getLog().warn("Unable to find git tag id for prior version id [" + priorGitVersion + "] (defined in VERSION.txt as [" + priorTextVersion + "])");
                getLog().info("Adding empty version section to top for version id [" + updateVersionText + "]");
                updateVersionText(versionText, rel, updateVersionText, priorTagId, currentCommitId, currentCommitId);
                return;
            }
            getLog().debug("Tag for prior version [" + priorGitVersion + "] is " + priorTagId);
            
            String priorCommitId = git.getTagCommitId(priorTagId);
            getLog().debug("Commit ID from [" + priorTagId + "]: " + priorCommitId);
            
            if (refreshTags)
            {
                String currentTagId = git.findTagMatching(updateVersionText);
                if (currentTagId != null)
                {
                    currentCommitId = git.getTagCommitId(currentTagId);
                }
            }
            getLog().debug("Commit ID to [" + updateVersionText + "]: " + currentCommitId);
            
            git.populateIssuesForRange(priorCommitId, currentCommitId, rel);
            
            int problemCount = resolveIssueSubjects(rel);
            if (problemCount > 0)
            {
                getLog().warn("Encounter [" + problemCount + "] issue(s) with potential problems." +
                        " A manual review of the changes to " + versionTextOuputFile +
                        " is strongly recommended!");
            }
            
            if ((rel.getReleasedOn() == null) && updateDate)
            {
                rel.setReleasedOn(new Date()); // now
            }
            
            updateVersionText(versionText, rel, updateVersionText, priorTagId, currentCommitId, currentCommitId);
        }
        catch (IOException e)
        {
            throw new MojoFailureException("Unable to generate replacement VERSION.txt", e);
        }
    }
    
    protected void updateVersionText(VersionText versionText, Release rel, String updateVersionText, String priorTagId, String priorCommitId, String currentCommitId) throws MojoFailureException, IOException
    {
        versionText.replaceOrPrepend(rel);
        generateVersion(versionText);
        String commitMessage = "Updating version " + updateVersionText + " in VERSION.txt";
        getLog().info("Update complete. Here's your git command. (Copy/Paste)\ngit commit -m \"" + commitMessage + "\" " + versionTextInputFile.getName());
    }
    
    /**
     * Attempt to resolve the issue subject lines against the issue
     * tracking system.
     *
     * @param rel the release
     * @return the count of problems when attempting to resolve github issues
     */
    protected int resolveIssueSubjects(Release rel)
    {
        GitHubIssueResolver issueResolver = new GitHubIssueResolver();
        
        int problemCount = 0;
        
        try
        {
            issueResolver.init(getLog());
            
            for (Issue issue : rel.getIssues())
            {
                if (issue.getSyntax() == IssueSyntax.BAD)
                {
                    getLog().warn("Issue with bad syntax needs review: " + issue.getId());
                    problemCount++;
                }
                String issueRef = issue.getId();
                try
                {
                    getLog().info("Resolving Subject for Issue " + issueRef);
                    GHIssue ghissue = issueResolver.getIssue(issueRef);
                    if (ghissue == null)
                    {
                        getLog().warn("Unable to find GitHub Issue " + issueRef);
                        problemCount++;
                        continue;
                    }
                    
                    // TODO: Idea, have includes/excludes of paths in the commit
                    //       should the commit have no files (of interest) changed
                    //       then skip this commit.
                    
                    // TODO: If *ONLY* Documentation label exists, skip
                    if (hasLabel(ghissue, "Documentation"))
                    {
                        getLog().warn("Found potential Documentation commit: " + ghissue);
                        problemCount++;
                    }
                    
                    issue.setText(ghissue.getTitle());
                }
                catch (IOException e)
                {
                    getLog().warn("Unable to obtain Subject for Issue " + issueRef, e);
                }
            }
        }
        catch (IOException e)
        {
            getLog().warn(e);
        }
        finally
        {
            issueResolver.destroy();
        }
        
        return problemCount;
    }
    
    private boolean hasLabel(GHIssue ghissue, String labelText)
    {
        try
        {
            Collection<GHLabel> labels = ghissue.getLabels();
            if (labels == null)
            {
                return false;
            }
            
            for (GHLabel ghlabel : labels)
            {
                if (ghlabel.getName().equalsIgnoreCase(labelText))
                {
                    return true;
                }
            }
        }
        catch (IOException ignore)
        {
            // ignore
        }
        return false;
    }
    
    private void generateVersion(VersionText versionText) throws MojoFailureException, IOException
    {
        ensureDirectoryExists(versionTextOuputFile.getCanonicalFile().getParentFile());
        versionText.write(versionTextOuputFile);
        getLog().debug("New VERSION.txt written at " + versionTextOuputFile.getAbsolutePath());
        
        if (attachArtifact)
        {
            getLog().info("Attaching generated VERSION.txt");
            getLog().debug("Classifier = " + classifier);
            getLog().debug("Type = " + type);
            projectHelper.attachArtifact(project, type, classifier, versionTextOuputFile);
        }
        
        if (copyGenerated)
        {
            getLog().info("Copying generated VERSION.txt over input VERSION.txt");
            FileUtils.copyFile(versionTextOuputFile, versionTextInputFile);
        }
    }
}
