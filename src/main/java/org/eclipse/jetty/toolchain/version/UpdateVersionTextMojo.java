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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jetty.toolchain.version.git.GitCommand;
import org.eclipse.jetty.toolchain.version.git.GitFilter;
import org.eclipse.jetty.toolchain.version.issues.GitHubIssueResolver;
import org.eclipse.jetty.toolchain.version.issues.Issue;
import org.eclipse.jetty.toolchain.version.issues.IssueParser;
import org.eclipse.jetty.toolchain.version.issues.IssueSyntax;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Update the active version entry in the VERSION.txt file from information present in the git logs.
 *
 */
@SuppressWarnings("unused")
@Mojo( name = "update-version-text", defaultPhase = LifecyclePhase.PACKAGE)
public class UpdateVersionTextMojo extends AbstractVersionMojo
{
    /**
     * The maven project version.
     */
    @Parameter(property="version.section", defaultValue="${project.version}", required = true)
    protected String version;
    
    /**
     * Allow the existing issues to be sorted alphabetically.
     */
    @Parameter(property="version.sort.existing", defaultValue="false")
    protected boolean sortExisting = false;
    
    /**
     * Allow the plugin to issue a 'git fetch --tags' to update the local tags from.
     */
    @Parameter(property="version.refresh.tags", defaultValue="false")
    protected boolean refreshTags = false;
    
    /**
     * Allow the plugin to update the release date for an issue (if none is provided)
     *
     */
    @Parameter(property="version.update.date", defaultValue="false")
    protected boolean updateDate = false;
    
    /**
     * Allow the plugin to replace the input VERSION.txt file
     */
    @Parameter(property="version.copy.generated", defaultValue="false")
    protected boolean copyGenerated;
    
    /**
     * Allow the plugin to attach the generated VERSION.txt file to the project
     *
     */
    @Parameter(property="version.attach", defaultValue="false")
    protected boolean attachArtifact;
    
    /**
     * The generated VERSION.txt file.
     */
    @Parameter(property="version.text.output.file", defaultValue="${project.build.directory}/VERSION.txt")
    protected File versionTextOutputFile;

    private GitHubIssueResolver issueResolver;

    private List<String> knowsContributors =
        Arrays.asList( "gregw", "janbartel", "jmcc0nn3ll", "joakime", "olamy", "sbordet", "WalkerWatch" );
    
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (!hasVersionTextFile("update-version-text"))
        {
            return; // skip
        }

        if (!hasCredentialsFile("update-version-text"))
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
            versionText.setKnowsContributors( knowsContributors );
            
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

            GitFilter gitFilter = new GitFilter();
            for(String filenameExclude: filenameExcludes)
            {
                gitFilter.addFilenameExclude(filenameExclude);
            }
            
            GitCommand git = new GitCommand();
            git.setWorkDir(basedir);
            git.setLog(getLog());
            git.setFilter(gitFilter);
            
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

            issueResolver = new GitHubIssueResolver(repoName);

            int problemCount = resolveIssueSubjects(rel);
            if (problemCount > 0)
            {
                getLog().warn("Encounter [" + problemCount + "] issue(s) with potential problems." +
                        " A manual review of the changes to " + versionTextOutputFile +
                        " is strongly recommended!");
            }
            
            if ((rel.getReleasedOn() == null) && updateDate)
            {
                rel.setReleasedOn(new Date()); // now
            }

            List<GHPullRequest> pullRequests = collectPullRequest( updateVersionGit, priorGitVersion, versionText, rel);

            for (GHPullRequest ghPullRequest : pullRequests)
            {
                GHUser ghUser = ghPullRequest.getUser();
                Contributor contributor = new Contributor( ghUser.getLogin(), ghUser.getName() );
                rel.addPullRequest( new PullRequest( Integer.toString( ghPullRequest.getNumber()), //
                                                     ghPullRequest.getTitle(), contributor) );
            }

            updateVersionText(versionText, rel, updateVersionText, priorTagId, currentCommitId, currentCommitId);
        }
        catch (IOException e)
        {
            throw new MojoFailureException("Unable to generate replacement VERSION.txt", e);
        }
    }

    /**
     * we need to know the branch name to filter pull requests we are reading.
     * FIXME better configurable (but yeah doesn't change so often :-) )
     * @return the branch name of the version
     */
    protected String getBranchName( )
    {
        // branch name is jetty-9.4 or jetty-9.3 depending on version
        // jetty-10 is master

        if ( StringUtils.startsWith( version, "9.3" ))
        {
            return "jetty-9.3.x";
        }
        if ( StringUtils.startsWith( version, "9.4" ))
        {
            return "jetty-9.4.x";
        }
        return "master";
    }
    
    protected void updateVersionText(VersionText versionText, Release rel, String updateVersionText, String priorTagId, String priorCommitId, String currentCommitId) throws MojoFailureException, IOException
    {
        versionText.replaceOrPrepend(rel);
        generateVersion(versionText);
        String commitMessage = "Updating version " + updateVersionText + " in VERSION.txt";
        getLog().info("Update complete. Here's your git command. (Copy/Paste)\ngit commit -m \"" + commitMessage + "\" " + versionTextInputFile.getName());
    }

    /**
     * collect pull request without any issue id and populate Release with contributors
     * @param updateVersionGit
     * @param priorGitVersion
     * @param versionText
     * @param release
     * @return
     * @throws IOException
     */
    protected List<GHPullRequest> collectPullRequest( String updateVersionGit, String priorGitVersion, VersionText versionText, Release release)
        throws IOException
    {
        List<GHPullRequest> ghPullRequests = issueResolver //
            .getPullRequests( updateVersionGit, priorGitVersion, getBranchName() );

        // collect all contributors except usual suspects
        ghPullRequests.stream().forEach( ghPullRequest -> {
            try
            {
                GHUser ghUser = ghPullRequest.getUser();
                if (!knowsContributors.contains( ghUser.getLogin() ))
                {
                    release.addContributor( new Contributor( ghUser.getLogin(), ghUser.getName() ) );
                }
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e.getMessage(), e );
            }

        } );


        IssueParser issueParser = new IssueParser();

        return ghPullRequests.stream() //
            // first filter only merged pr
            .filter( ghPullRequest -> {
                try
                {
                    return ghPullRequest.isMerged();
                }
                catch ( IOException e )
                {
                    throw new RuntimeException( e.getMessage(), e );
                }
            } )
            // filter pull request not containing issue id in title or comment
            .filter(  ghPullRequest ->
                  issueParser.parsePossibleIssue( ghPullRequest.getTitle() ) == null &&
                    issueParser.parsePossibleIssue( ghPullRequest.getBody() ) == null
            )
            .collect( Collectors.toList() );
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

        List<Issue> filtered = new ArrayList<>();

        int problemCount = 0;
        
        try
        {
            issueResolver.init(getLog());

            for (Issue issue : rel.getIssues())
            {

                String issueRef = issue.getId();
                try
                {
                    getLog().debug("Resolving Subject for Issue " + issueRef);
                    GHIssue ghissue = issueResolver.getIssue(issueRef);
                    if (ghissue == null)
                    {
                        getLog().info("Unable to find GitHub Issue " + issueRef);
                        problemCount++;
                        continue;
                    }

                    // Filter out pull requests, properly formatted commits should show up under the actual issue ID
                    if (ghissue.isPullRequest())
                    {
                        getLog().info("Filtering Pull Request: " + issue.getId());
                        filtered.add(issue);
                        continue;
                    }

                    // If the issue only has the Documentation tag we can filter it out
                    if (hasLabel(ghissue, "Documentation"))
                    {
                        if (ghissue.getLabels().size() == 1)
                        {
                            getLog().info("Filtering 'Documentation' only tagged issue: " + ghissue.getTitle());
                            filtered.add(issue);
                            continue;
                        }
                    }
    
                    if (hasLabel(ghissue, "Invalid"))
                    {
                        if (ghissue.getLabels().size() == 1)
                        {
                            getLog().info("Filtering 'Invalid' only tagged issue: " + ghissue.getTitle());
                            filtered.add(issue);
                            continue;
                        }
                    }

                    issue.setText(ghissue.getTitle());
                }
                catch (IOException e)
                {
                    getLog().warn("Unable to obtain Subject for Issue " + issueRef, e);
                }
                catch (NumberFormatException e)
                {
                    getLog().warn("Bad Issue # crept in: " + e.getMessage() );
                }

                if (issue.getSyntax() == IssueSyntax.BAD)
                {
                    getLog().warn("Issue with bad syntax needs review: " + issue.getId());
                    problemCount++;
                }
            }

            // Drop filtered issues
            rel.dropIssues(filtered);

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
        ensureDirectoryExists(versionTextOutputFile.getCanonicalFile().getParentFile());
        versionText.write(versionTextOutputFile);
        getLog().debug("New VERSION.txt written at " + versionTextOutputFile.getAbsolutePath());
        
        if (attachArtifact)
        {
            getLog().info("Attaching generated VERSION.txt");
            getLog().debug("Classifier = " + classifier);
            getLog().debug("Type = " + type);
            projectHelper.attachArtifact(project, type, classifier, versionTextOutputFile);
        }
        
        if (copyGenerated)
        {
            getLog().info("Copying generated VERSION.txt over input VERSION.txt");
            FileUtils.copyFile(versionTextOutputFile, versionTextInputFile);
        }
    }
}
