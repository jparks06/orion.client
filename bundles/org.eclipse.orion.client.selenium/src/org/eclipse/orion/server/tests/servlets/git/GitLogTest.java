/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.tests.servlets.git;

import static org.eclipse.orion.server.tests.IsJSONArrayEqual.isJSONArrayEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import com.meterware.httpunit.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.orion.internal.server.core.IOUtilities;
import org.eclipse.orion.internal.server.servlets.ProtocolConstants;
import org.eclipse.orion.server.git.GitConstants;
import org.eclipse.orion.server.git.objects.Commit;
import org.eclipse.orion.server.git.objects.Diff;
import org.json.*;
import org.junit.Assume;
import org.junit.Test;

public class GitLogTest extends GitTest {

	@Test
	public void testLog() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		IPath[] clonePaths = createTestProjects(workspaceLocation);

		for (IPath clonePath : clonePaths) {
			// clone a  repo
			JSONObject clone = clone(clonePath);
			String cloneContentLocation = clone.getString(ProtocolConstants.KEY_CONTENT_LOCATION);

			// get project/folder metadata
			WebRequest request = getGetFilesRequest(cloneContentLocation);
			WebResponse response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject folder = new JSONObject(response.getText());

			JSONObject gitSection = folder.getJSONObject(GitConstants.KEY_GIT);
			String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

			// modify
			JSONObject testTxt = getChild(folder, "test.txt");
			modifyFile(testTxt, "first change");
			addFile(testTxt);

			// commit1
			request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "commit1", false);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

			// modify again
			modifyFile(testTxt, "second change");
			addFile(testTxt);

			// commit2
			request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "commit2", false);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

			// get the full log
			JSONArray commitsArray = log(gitHeadUri);
			assertEquals(3, commitsArray.length());

			JSONObject commit = commitsArray.getJSONObject(0);
			assertEquals("commit2", commit.get(GitConstants.KEY_COMMIT_MESSAGE));

			commit = commitsArray.getJSONObject(1);
			assertEquals("commit1", commit.get(GitConstants.KEY_COMMIT_MESSAGE));

			commit = commitsArray.getJSONObject(2);
			assertEquals("Initial commit", commit.get(GitConstants.KEY_COMMIT_MESSAGE));
			String initialGitCommitName = commit.getString(ProtocolConstants.KEY_NAME);
			String initialGitCommitURI = gitHeadUri.replaceAll(Constants.HEAD, initialGitCommitName);

			//get log for given page size
			commitsArray = log(gitHeadUri, 1, 1, false, true);
			assertEquals(1, commitsArray.length());

			commit = commitsArray.getJSONObject(0);
			assertEquals("commit2", commit.get(GitConstants.KEY_COMMIT_MESSAGE));

			//get log for second page
			commitsArray = log(gitHeadUri, 2, 1, true, true);
			assertEquals(1, commitsArray.length());

			commit = commitsArray.getJSONObject(0);
			assertEquals("commit1", commit.get(GitConstants.KEY_COMMIT_MESSAGE));

			// prepare a scoped log location
			request = getPostForScopedLogRequest(initialGitCommitURI, Constants.HEAD);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			String newGitCommitUri = response.getHeaderField(ProtocolConstants.KEY_LOCATION);
			assertEquals(newGitCommitUri, new JSONObject(response.getText()).getString(ProtocolConstants.KEY_LOCATION));

			// get a scoped log
			commitsArray = log(newGitCommitUri);
			assertEquals(2, commitsArray.length());

			commit = commitsArray.getJSONObject(0);
			assertEquals("commit2", commit.get(GitConstants.KEY_COMMIT_MESSAGE));

			commit = commitsArray.getJSONObject(1);
			assertEquals("commit1", commit.get(GitConstants.KEY_COMMIT_MESSAGE));
		}
	}

	@Test
	public void testLogOrionServerLinked() throws Exception {
		File orionServer = new File("").getAbsoluteFile().getParentFile(/*org.eclipse.orion.server.tests*/).getParentFile(/*tests*/);
		Assume.assumeTrue(new File(orionServer, Constants.DOT_GIT).exists());

		URI workspaceLocation = createWorkspace(getMethodName());
		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), orionServer.toURI().toString());
		String location = project.getString(ProtocolConstants.KEY_CONTENT_LOCATION);

		// get project/folder metadata
		WebRequest request = getGetFilesRequest(location);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		JSONObject folder = new JSONObject(response.getText());

		JSONObject gitSection = folder.getJSONObject(GitConstants.KEY_GIT);
		String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

		// reading first 50 commits should be enough to start a task
		JSONArray commitsArray = log(gitHeadUri, 1, 50, false, true);
		assertEquals(50, commitsArray.length());
	}

	@Test
	public void testLogRemoteBranch() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		IPath[] clonePaths = createTestProjects(workspaceLocation);

		for (IPath clonePath : clonePaths) {
			// clone a repo
			JSONObject clone = clone(clonePath);
			String cloneContentLocation = clone.getString(ProtocolConstants.KEY_CONTENT_LOCATION);

			// get project/folder metadata
			WebRequest request = getGetFilesRequest(cloneContentLocation);
			WebResponse response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject folder = new JSONObject(response.getText());

			JSONObject gitSection = folder.getJSONObject(GitConstants.KEY_GIT);
			String gitRemoteUri = gitSection.getString(GitConstants.KEY_REMOTE);
			JSONObject originMasterDetails = getRemoteBranch(gitRemoteUri, 1, 0, Constants.MASTER);
			String originMasterCommitUri = originMasterDetails.getString(GitConstants.KEY_COMMIT);

			JSONArray commitsArray = log(originMasterCommitUri);
			assertEquals(1, commitsArray.length());
		}
	}

	@Test
	public void testLogWithTag() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		IPath[] clonePaths = createTestProjects(workspaceLocation);

		for (IPath clonePath : clonePaths) {
			// clone a  repo
			JSONObject clone = clone(clonePath);
			String cloneContentLocation = clone.getString(ProtocolConstants.KEY_CONTENT_LOCATION);

			// get project metadata
			WebRequest request = getGetFilesRequest(cloneContentLocation);
			WebResponse response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject project = new JSONObject(response.getText());

			JSONObject gitSection = project.getJSONObject(GitConstants.KEY_GIT);
			String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

			JSONArray commitsArray = log(gitHeadUri);
			assertEquals(1, commitsArray.length());

			String commitUri = commitsArray.getJSONObject(0).getString(ProtocolConstants.KEY_LOCATION);
			JSONObject updatedCommit = tag(commitUri, "tag");
			JSONArray tagsArray = updatedCommit.getJSONArray(GitConstants.KEY_TAGS);
			assertEquals(1, tagsArray.length());
			assertEquals(Constants.R_TAGS + "tag", tagsArray.getJSONObject(0).get(ProtocolConstants.KEY_FULL_NAME));

			commitsArray = log(gitHeadUri);
			assertEquals(1, commitsArray.length());

			tagsArray = commitsArray.getJSONObject(0).getJSONArray(GitConstants.KEY_TAGS);
			assertEquals(1, tagsArray.length());
			assertEquals(Constants.R_TAGS + "tag", tagsArray.getJSONObject(0).get(ProtocolConstants.KEY_FULL_NAME));
		}
	}

	@Test
	public void testLogWithBranch() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		IPath[] clonePaths = createTestProjects(workspaceLocation);

		for (IPath clonePath : clonePaths) {
			// clone a  repo
			JSONObject clone = clone(clonePath);
			String cloneContentLocation = clone.getString(ProtocolConstants.KEY_CONTENT_LOCATION);
			String branchesLocation = clone.getString(GitConstants.KEY_BRANCH);

			// get project metadata
			WebRequest request = getGetFilesRequest(cloneContentLocation);
			WebResponse response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject project = new JSONObject(response.getText());

			JSONObject gitSection = project.getJSONObject(GitConstants.KEY_GIT);
			String gitCommitUri = gitSection.getString(GitConstants.KEY_COMMIT);

			JSONArray commitsArray = log(gitCommitUri);
			assertEquals(1, commitsArray.length());

			branch(branchesLocation, "branch");

			commitsArray = log(gitCommitUri);
			assertEquals(1, commitsArray.length());

			JSONArray branchesArray = commitsArray.getJSONObject(0).getJSONArray(GitConstants.KEY_BRANCHES);
			assertEquals(3, branchesArray.length());
			assertEquals(Constants.R_HEADS + "branch", branchesArray.getJSONObject(0).get(ProtocolConstants.KEY_FULL_NAME));
			assertEquals(Constants.R_HEADS + Constants.MASTER, branchesArray.getJSONObject(1).get(ProtocolConstants.KEY_FULL_NAME));
			assertEquals(Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER, branchesArray.getJSONObject(2).get(ProtocolConstants.KEY_FULL_NAME));
		}
	}

	@Test
	public void testLogWithParents() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		IPath[] clonePaths = createTestProjects(workspaceLocation);

		for (IPath clonePath : clonePaths) {
			// clone a  repo
			JSONObject clone = clone(clonePath);
			String cloneContentLocation = clone.getString(ProtocolConstants.KEY_CONTENT_LOCATION);

			// get project metadata
			WebRequest request = getGetFilesRequest(cloneContentLocation);
			WebResponse response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject project = new JSONObject(response.getText());

			JSONObject gitSection = project.getJSONObject(GitConstants.KEY_GIT);
			String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

			// modify
			JSONObject testTxt = getChild(project, "test.txt");
			modifyFile(testTxt, "first change");
			addFile(testTxt);

			// commit1
			request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "commit1", false);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

			// modify again
			modifyFile(testTxt, "second change");
			addFile(testTxt);

			// commit2
			request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "commit2", false);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

			// get the full log
			JSONArray commitsArray = log(gitHeadUri);
			assertEquals(3, commitsArray.length());

			JSONObject commit = commitsArray.getJSONObject(0);
			assertEquals("commit2", commit.get(GitConstants.KEY_COMMIT_MESSAGE));
			assertEquals(1, commit.getJSONArray(ProtocolConstants.KEY_PARENTS).length());
			String parent = commit.getJSONArray(ProtocolConstants.KEY_PARENTS).getJSONObject(0).getString(ProtocolConstants.KEY_NAME);

			commit = commitsArray.getJSONObject(1);
			assertEquals("commit1", commit.get(GitConstants.KEY_COMMIT_MESSAGE));
			assertEquals(parent, commit.get(ProtocolConstants.KEY_NAME));
			assertEquals(1, commit.getJSONArray(ProtocolConstants.KEY_PARENTS).length());
			parent = commit.getJSONArray(ProtocolConstants.KEY_PARENTS).getJSONObject(0).getString(ProtocolConstants.KEY_NAME);

			commit = commitsArray.getJSONObject(2);
			assertEquals("Initial commit", commit.get(GitConstants.KEY_COMMIT_MESSAGE));
			assertEquals(parent, commit.get(ProtocolConstants.KEY_NAME));
			assertEquals(0, commit.getJSONArray(ProtocolConstants.KEY_PARENTS).length());
		}
	}

	@Test
	public void testLogAllBranches() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		IPath[] clonePaths = createTestProjects(workspaceLocation);

		for (IPath clonePath : clonePaths) {
			// clone a repo
			JSONObject clone = clone(clonePath);
			String cloneLocation = clone.getString(ProtocolConstants.KEY_LOCATION);
			String cloneContentLocation = clone.getString(ProtocolConstants.KEY_CONTENT_LOCATION);
			String branchesLocation = clone.getString(GitConstants.KEY_BRANCH);
			String gitCommitUri = clone.getString(GitConstants.KEY_COMMIT);

			// get project metadata
			WebRequest request = getGetFilesRequest(cloneContentLocation);
			WebResponse response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject project = new JSONObject(response.getText());
			JSONObject gitSection = project.getJSONObject(GitConstants.KEY_GIT);
			String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

			// create branch
			final String newBranchName = "branch";
			branch(branchesLocation, newBranchName);

			// modify
			JSONObject testTxt = getChild(project, "test.txt");
			modifyFile(testTxt, "first change");
			addFile(testTxt);

			// commit1
			Thread.sleep(1000); // TODO: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=370696#c1
			request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "commit1", false);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

			// checkout "branch"
			checkoutBranch(cloneLocation, newBranchName);

			// modify again
			modifyFile(testTxt, "second change");
			addFile(testTxt);

			// commit2
			Thread.sleep(1000); // TODO: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=370696#c1
			request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "commit2", false);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

			// get standard log for HEAD - only init and commit2 should be visible
			JSONArray commitsArray = log(gitHeadUri);
			assertEquals(2, commitsArray.length());

			JSONObject commit = commitsArray.getJSONObject(0);
			assertEquals("commit2", commit.get(GitConstants.KEY_COMMIT_MESSAGE));

			commit = commitsArray.getJSONObject(1);
			assertEquals("Initial commit", commit.get(GitConstants.KEY_COMMIT_MESSAGE));

			// get log for all branches - all 3 commits should be listed
			commitsArray = log(gitCommitUri);
			assertEquals(3, commitsArray.length());

			commit = commitsArray.getJSONObject(0);
			assertEquals("commit2", commit.get(GitConstants.KEY_COMMIT_MESSAGE));
			JSONArray branchesArray = commit.getJSONArray(GitConstants.KEY_BRANCHES);
			assertEquals(1, branchesArray.length());
			assertEquals(Constants.R_HEADS + newBranchName, branchesArray.getJSONObject(0).get(ProtocolConstants.KEY_FULL_NAME));

			commit = commitsArray.getJSONObject(1);
			assertEquals("commit1", commit.get(GitConstants.KEY_COMMIT_MESSAGE));
			branchesArray = commit.getJSONArray(GitConstants.KEY_BRANCHES);
			assertEquals(1, branchesArray.length());
			assertEquals(Constants.R_HEADS + Constants.MASTER, branchesArray.getJSONObject(0).get(ProtocolConstants.KEY_FULL_NAME));

			commit = commitsArray.getJSONObject(2);
			assertEquals("Initial commit", commit.get(GitConstants.KEY_COMMIT_MESSAGE));
			branchesArray = commit.getJSONArray(GitConstants.KEY_BRANCHES);
			assertEquals(1, branchesArray.length());
			assertEquals(Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER, branchesArray.getJSONObject(0).get(ProtocolConstants.KEY_FULL_NAME));
		}
	}

	@Test
	public void testLogFolder() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());

		String projectName = getMethodName();
		JSONObject project = createProjectOrLink(workspaceLocation, projectName, gitDir.toString());

		JSONObject gitSection = project.getJSONObject(GitConstants.KEY_GIT);
		String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

		// modify
		JSONObject testTxt = getChild(project, "test.txt");
		modifyFile(testTxt, "test.txt change");
		addFile(testTxt);

		// commit1
		WebRequest request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "commit1", false);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

		// modify again
		JSONObject folder = getChild(project, "folder");
		JSONObject folderTxt = getChild(folder, "folder.txt");
		modifyFile(folderTxt, "folder.txt change");
		addFile(folderTxt);

		// commit2
		request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "commit2", false);
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

		// get log for file
		JSONObject log = logObject(gitHeadUri);

		String repositoryPath = log.getString(GitConstants.KEY_REPOSITORY_PATH);
		assertEquals("", repositoryPath);

		JSONArray commitsArray = log.getJSONArray(ProtocolConstants.KEY_CHILDREN);

		assertEquals(3, commitsArray.length());

		JSONObject commit = commitsArray.getJSONObject(0);
		assertEquals("commit2", commit.get(GitConstants.KEY_COMMIT_MESSAGE));

		commit = commitsArray.getJSONObject(1);
		assertEquals("commit1", commit.get(GitConstants.KEY_COMMIT_MESSAGE));

		commit = commitsArray.getJSONObject(2);
		assertEquals("Initial commit", commit.get(GitConstants.KEY_COMMIT_MESSAGE));

		log = logObject(folder.getJSONObject(GitConstants.KEY_GIT).getString(GitConstants.KEY_HEAD));

		repositoryPath = log.getString(GitConstants.KEY_REPOSITORY_PATH);
		assertEquals("folder/", repositoryPath);

		commitsArray = log.getJSONArray(ProtocolConstants.KEY_CHILDREN);

		assertEquals(2, commitsArray.length());

		commit = commitsArray.getJSONObject(0);
		assertEquals("commit2", commit.get(GitConstants.KEY_COMMIT_MESSAGE));

		commit = commitsArray.getJSONObject(1);
		assertEquals("Initial commit", commit.get(GitConstants.KEY_COMMIT_MESSAGE));
	}

	@Test
	public void testLogFile() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());

		String projectName = getMethodName();
		JSONObject project = createProjectOrLink(workspaceLocation, projectName, gitDir.toString());

		JSONObject gitSection = project.getJSONObject(GitConstants.KEY_GIT);
		String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

		// modify
		JSONObject testTxt = getChild(project, "test.txt");
		modifyFile(testTxt, "test.txt change");
		addFile(testTxt);

		// commit1
		WebRequest request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "commit1", false);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

		// modify again
		JSONObject folder = getChild(project, "folder");
		JSONObject folderTxt = getChild(folder, "folder.txt");
		modifyFile(folderTxt, "folder.txt change");
		addFile(folderTxt);

		// commit2
		request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "commit2", false);
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

		// get log for file
		JSONObject log = logObject(testTxt.getJSONObject(GitConstants.KEY_GIT).getString(GitConstants.KEY_HEAD));

		String repositoryPath = log.getString(GitConstants.KEY_REPOSITORY_PATH);
		assertEquals("test.txt", repositoryPath);

		JSONArray commitsArray = log.getJSONArray(ProtocolConstants.KEY_CHILDREN);

		assertEquals(2, commitsArray.length());

		JSONObject commit = commitsArray.getJSONObject(0);
		assertEquals("commit1", commit.getString(GitConstants.KEY_COMMIT_MESSAGE));

		// check commit content location
		assertEquals("test.txt change", getCommitContent(commit));

		// check commit diff location
		String[] parts = GitDiffTest.getDiff(commit.getString(GitConstants.KEY_DIFF));

		assertEquals("", parts[1]); // no diff between the commit and working tree

		// check commit location
		JSONObject jsonObject = logObject(commit.getString(ProtocolConstants.KEY_LOCATION));

		assertEquals(log.getString(GitConstants.KEY_CLONE), jsonObject.getString(GitConstants.KEY_CLONE));
		assertEquals(log.getString(GitConstants.KEY_REPOSITORY_PATH), jsonObject.getString(GitConstants.KEY_REPOSITORY_PATH));
		assertNull(jsonObject.optString(GitConstants.KEY_LOG_TO_REF, null));
		assertNull(jsonObject.optString(GitConstants.KEY_LOG_FROM_REF, null));
		assertThat(jsonObject.getJSONArray(ProtocolConstants.KEY_CHILDREN), isJSONArrayEqual(log.getJSONArray(ProtocolConstants.KEY_CHILDREN)));

		// check second commit		
		commit = commitsArray.getJSONObject(1);
		assertEquals("Initial commit", commit.getString(GitConstants.KEY_COMMIT_MESSAGE));
		// check commit content location
		assertEquals("test", getCommitContent(commit));

		// check commit diff location
		parts = GitDiffTest.getDiff(commit.getString(GitConstants.KEY_DIFF));

		StringBuilder sb = new StringBuilder();
		sb.append("diff --git a/test.txt b/test.txt").append("\n");
		sb.append("index 30d74d2..3146ed5 100644").append("\n");
		sb.append("--- a/test.txt").append("\n");
		sb.append("+++ b/test.txt").append("\n");
		sb.append("@@ -1 +1 @@").append("\n");
		sb.append("-test").append("\n");
		sb.append("\\ No newline at end of file").append("\n");
		sb.append("+test.txt change").append("\n");
		sb.append("\\ No newline at end of file").append("\n");
		assertEquals(sb.toString(), parts[1]);

		// check commit location
		jsonObject = logObject(commit.getString(ProtocolConstants.KEY_LOCATION));
		JSONObject log2 = log;
		JSONArray commitsArray2 = log2.getJSONArray(ProtocolConstants.KEY_CHILDREN);
		commitsArray2.remove(0);
		assertEquals(log.getString(GitConstants.KEY_CLONE), jsonObject.getString(GitConstants.KEY_CLONE));
		assertEquals(log.getString(GitConstants.KEY_REPOSITORY_PATH), jsonObject.getString(GitConstants.KEY_REPOSITORY_PATH));
		assertNull(jsonObject.optString(GitConstants.KEY_LOG_TO_REF, null));
		assertNull(jsonObject.optString(GitConstants.KEY_LOG_FROM_REF, null));
		assertThat(commitsArray2, isJSONArrayEqual(log.getJSONArray(ProtocolConstants.KEY_CHILDREN)));
	}

	@Test
	public void testLogNewBranch() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		String workspaceId = workspaceIdFromLocation(workspaceLocation);

		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), null);
		JSONObject clone = clone(getClonePath(workspaceId, project));
		String cloneLocation = clone.getString(ProtocolConstants.KEY_LOCATION);
		String branchesLocation = clone.getString(GitConstants.KEY_BRANCH);

		// get project metadata
		WebRequest request = getGetFilesRequest(project.getString(ProtocolConstants.KEY_CONTENT_LOCATION));
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		project = new JSONObject(response.getText());
		JSONObject gitSection = project.getJSONObject(GitConstants.KEY_GIT);
		String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

		JSONArray commitsArray = log(gitHeadUri);
		assertEquals(1, commitsArray.length());

		branch(branchesLocation, "a");
		checkoutBranch(cloneLocation, "a");

		// get project metadata again
		request = getGetFilesRequest(project.getString(ProtocolConstants.KEY_LOCATION));
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		project = new JSONObject(response.getText());
		gitSection = project.getJSONObject(GitConstants.KEY_GIT);
		gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

		log(gitHeadUri);
	}

	@Test
	public void testDiffFromLog() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		IPath[] clonePaths = createTestProjects(workspaceLocation);

		for (IPath clonePath : clonePaths) {
			// clone a  repo
			JSONObject clone = clone(clonePath);
			String cloneContentLocation = clone.getString(ProtocolConstants.KEY_CONTENT_LOCATION);

			// get project/folder metadata
			WebRequest request = getGetFilesRequest(cloneContentLocation);
			WebResponse response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject folder = new JSONObject(response.getText());

			JSONObject gitSection = folder.getJSONObject(GitConstants.KEY_GIT);
			String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

			// modify
			JSONObject testTxt = getChild(folder, "test.txt");
			modifyFile(testTxt, "hello");
			addFile(testTxt);

			// commit
			request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "2nd commit", false);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

			// get the full log
			JSONArray commits = log(gitHeadUri);
			assertEquals(2, commits.length());
			JSONObject commit = commits.getJSONObject(0);
			assertEquals("2nd commit", commit.get(GitConstants.KEY_COMMIT_MESSAGE));
			JSONArray diffs = commit.getJSONArray(GitConstants.KEY_COMMIT_DIFFS);
			assertEquals(1, diffs.length());

			// check diff object
			JSONObject diff = diffs.getJSONObject(0);
			assertEquals("test.txt", diff.getString(GitConstants.KEY_COMMIT_DIFF_NEWPATH));
			assertEquals("test.txt", diff.getString(GitConstants.KEY_COMMIT_DIFF_OLDPATH));
			assertEquals(Diff.TYPE, diff.getString(ProtocolConstants.KEY_TYPE));
			assertEquals(ChangeType.MODIFY, ChangeType.valueOf(diff.getString(GitConstants.KEY_COMMIT_DIFF_CHANGETYPE)));
			String contentLocation = diff.getString(ProtocolConstants.KEY_CONTENT_LOCATION);
			String fileContent = getFileContent(new JSONObject().put(ProtocolConstants.KEY_LOCATION, contentLocation));
			assertEquals("hello", fileContent);
			String diffLocation = diff.getString(GitConstants.KEY_DIFF);

			// check diff location
			String[] parts = GitDiffTest.getDiff(diffLocation);

			StringBuilder sb = new StringBuilder();
			sb.append("diff --git a/test.txt b/test.txt").append("\n");
			sb.append("index 30d74d2..b6fc4c6 100644").append("\n");
			sb.append("--- a/test.txt").append("\n");
			sb.append("+++ b/test.txt").append("\n");
			sb.append("@@ -1 +1 @@").append("\n");
			sb.append("-test").append("\n");
			sb.append("\\ No newline at end of file").append("\n");
			sb.append("+hello").append("\n");
			sb.append("\\ No newline at end of file").append("\n");
			assertEquals(sb.toString(), parts[1]);
		}
	}

	@Test
	public void testToRefKey() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		IPath[] clonePaths = createTestProjects(workspaceLocation);

		for (IPath clonePath : clonePaths) {
			// clone a  repo
			String contentLocation = clone(clonePath).getString(ProtocolConstants.KEY_CONTENT_LOCATION);

			// get project metadata
			WebRequest request = getGetFilesRequest(contentLocation);
			WebResponse response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject project = new JSONObject(response.getText());

			JSONObject gitSection = project.getJSONObject(GitConstants.KEY_GIT);
			String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

			// git log for HEAD
			JSONObject log = logObject(gitHeadUri);

			// project contains only initial commit, so HEAD points to master branch
			// information about master branch is expected
			JSONObject toRef = log.getJSONObject(GitConstants.KEY_LOG_TO_REF);

			assertEquals(gitHeadUri, toRef.getString(GitConstants.KEY_HEAD));
			assertEquals(gitSection.getString(GitConstants.KEY_CLONE), toRef.getString(GitConstants.KEY_CLONE));
			assertEquals(GitConstants.KEY_BRANCH_NAME, toRef.getString(ProtocolConstants.KEY_TYPE));
			assertEquals(Constants.MASTER, toRef.getString(ProtocolConstants.KEY_NAME));
			assertEquals(true, toRef.getBoolean(GitConstants.KEY_BRANCH_CURRENT));
		}
	}

	@Test
	public void testFromRefKey() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		IPath[] clonePaths = createTestProjects(workspaceLocation);

		for (IPath clonePath : clonePaths) {
			// clone a  repo
			String contentLocation = clone(clonePath).getString(ProtocolConstants.KEY_CONTENT_LOCATION);

			// get project metadata
			WebRequest request = getGetFilesRequest(contentLocation);
			WebResponse response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject project = new JSONObject(response.getText());

			JSONObject gitSection = project.getJSONObject(GitConstants.KEY_GIT);
			String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

			String logUri = gitHeadUri.replace(Constants.HEAD, Constants.HEAD + ".." + Constants.HEAD);
			// git log for HEAD..HEAD
			JSONObject log = logObject(logUri);

			// project contains only initial commit, so HEAD points to master branch
			// information about master branch is expected
			JSONObject fromRef = log.getJSONObject(GitConstants.KEY_LOG_FROM_REF);

			assertEquals(gitHeadUri, fromRef.getString(GitConstants.KEY_HEAD));
			assertEquals(gitSection.getString(GitConstants.KEY_CLONE), fromRef.getString(GitConstants.KEY_CLONE));
			assertEquals(GitConstants.KEY_BRANCH_NAME, fromRef.getString(ProtocolConstants.KEY_TYPE));
			assertEquals(Constants.MASTER, fromRef.getString(ProtocolConstants.KEY_NAME));
			assertEquals(true, fromRef.getBoolean(GitConstants.KEY_BRANCH_CURRENT));
		}
	}

	@Test
	public void testRefPropertiesForCommits() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		IPath[] clonePaths = createTestProjects(workspaceLocation);

		for (IPath clonePath : clonePaths) {
			// clone a  repo
			String contentLocation = clone(clonePath).getString(ProtocolConstants.KEY_CONTENT_LOCATION);

			// get project metadata
			WebRequest request = getGetFilesRequest(contentLocation);
			WebResponse response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject project = new JSONObject(response.getText());

			JSONObject gitSection = project.getJSONObject(GitConstants.KEY_GIT);
			String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);
			String gitCommitUri = gitSection.getString(GitConstants.KEY_COMMIT);

			// save initial commit name
			JSONArray commitsArray = log(gitCommitUri);
			JSONObject initCommit = commitsArray.getJSONObject(0);
			String initCommitName = initCommit.getString(ProtocolConstants.KEY_NAME);
			String initCommitLocation = initCommit.getString(ProtocolConstants.KEY_LOCATION);

			// modify
			JSONObject testTxt = getChild(project, "test.txt");
			modifyFile(testTxt, "test.txt change");
			addFile(testTxt);

			// commit
			request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "commit", false);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

			String logUri = initCommitLocation.replace(initCommitName, initCommitName + ".." + initCommitName);
			// git log for {initial commit}..{initial commit}
			// both fromRef and toRef shouldn't exist, as commit ref doesn't point to a branch
			JSONObject log = logObject(logUri);

			JSONObject fromRef = log.optJSONObject(GitConstants.KEY_LOG_FROM_REF);
			JSONObject toRef = log.optJSONObject(GitConstants.KEY_LOG_TO_REF);

			assertNull(fromRef);
			assertNull(toRef);
		}
	}

	@Test
	public void testGetNonExistingCommit() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		String projectName = getMethodName();
		JSONObject project = createProjectOrLink(workspaceLocation, projectName, gitDir.toString());
		JSONObject testTxt = getChild(project, "test.txt");

		// get log for file
		JSONObject log = logObject(testTxt.getJSONObject(GitConstants.KEY_GIT).getString(GitConstants.KEY_HEAD));
		JSONArray commitsArray = log.getJSONArray(ProtocolConstants.KEY_CHILDREN);
		assertEquals(1, commitsArray.length());

		JSONObject commit = commitsArray.getJSONObject(0);
		assertEquals("Initial commit", commit.getString(GitConstants.KEY_COMMIT_MESSAGE));
		String commitName = commit.getString(ProtocolConstants.KEY_NAME);
		String dummyName = "dummyName";
		assertFalse(dummyName.equals(commitName));
		String commitLocation = commit.getString(ProtocolConstants.KEY_CONTENT_LOCATION);

		// prepare the dummy commit location
		commitLocation = commitLocation.replace(commitName, dummyName).replace("?parts=body", "?page=1&pageSize=1");

		WebRequest request = getGetRequest(commitLocation);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getResponseCode());
	}

	private static WebRequest getPostForScopedLogRequest(String location, String newCommit) throws JSONException, UnsupportedEncodingException {
		String requestURI;
		if (location.startsWith("http://"))
			requestURI = location;
		else
			requestURI = SERVER_LOCATION + GIT_SERVLET_LOCATION + Commit.RESOURCE + location;

		JSONObject body = new JSONObject();
		body.put(GitConstants.KEY_COMMIT_NEW, newCommit);
		WebRequest request = new PostMethodWebRequest(requestURI, IOUtilities.toInputStream(body.toString()), "UTF-8");
		request.setHeaderField(ProtocolConstants.HEADER_ORION_VERSION, "1");
		setAuthentication(request);
		return request;
	}
}
