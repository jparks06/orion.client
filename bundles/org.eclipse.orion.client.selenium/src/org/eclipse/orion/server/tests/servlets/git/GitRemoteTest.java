/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.tests.servlets.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.meterware.httpunit.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.*;
import org.eclipse.orion.internal.server.core.IOUtilities;
import org.eclipse.orion.internal.server.servlets.ProtocolConstants;
import org.eclipse.orion.server.core.ServerStatus;
import org.eclipse.orion.server.git.GitConstants;
import org.eclipse.orion.server.git.objects.Remote;
import org.eclipse.orion.server.git.servlets.GitUtils;
import org.eclipse.orion.server.tests.servlets.internal.DeleteMethodWebRequest;
import org.json.*;
import org.junit.Test;
import org.xml.sax.SAXException;

public class GitRemoteTest extends GitTest {
	@Test
	public void testGetNoRemote() throws Exception {
		URI contentLocation = gitDir.toURI();

		JSONObject project = linkProject(contentLocation.toString(), getMethodName());
		String projectContentLocation = project.getString(ProtocolConstants.KEY_CONTENT_LOCATION);
		assertNotNull(projectContentLocation);

		// http://<host>/file/<projectId>/
		WebRequest request = getGetFilesRequest(projectContentLocation);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		project = new JSONObject(response.getText());
		JSONObject gitSection = project.getJSONObject(GitConstants.KEY_GIT);
		String gitRemoteUri = gitSection.getString(GitConstants.KEY_REMOTE);

		request = getGetGitRemoteRequest(gitRemoteUri);
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		JSONObject remotes = new JSONObject(response.getText());
		JSONArray remotesArray = remotes.getJSONArray(ProtocolConstants.KEY_CHILDREN);
		assertEquals(0, remotesArray.length());
	}

	@Test
	public void testGetOrigin() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		IPath[] clonePaths = createTestProjects(workspaceLocation);

		for (IPath clonePath : clonePaths) {
			// clone a  repo
			JSONObject clone = clone(clonePath);
			String cloneContentLocation = clone.getString(ProtocolConstants.KEY_CONTENT_LOCATION);
			String gitRemoteUri = clone.getString(GitConstants.KEY_REMOTE);
			JSONObject remoteBranch = getRemoteBranch(gitRemoteUri, 1, 0, Constants.MASTER);
			assertNotNull(remoteBranch);

			// get project/folder metadata
			WebRequest request = getGetFilesRequest(cloneContentLocation);
			WebResponse response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject folder = new JSONObject(response.getText());

			// check if Git locations are in place
			JSONObject gitSection = folder.getJSONObject(GitConstants.KEY_GIT);
			assertEquals(gitRemoteUri, gitSection.getString(GitConstants.KEY_REMOTE));
		}
	}

	@Test
	public void testGetUnknownRemote() throws Exception {
		// clone a  repo
		URI workspaceLocation = createWorkspace(getMethodName());
		String workspaceId = workspaceIdFromLocation(workspaceLocation);
		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), null);
		IPath clonePath = getClonePath(workspaceId, project);
		clone(clonePath);

		// get project metadata
		WebRequest request = getGetFilesRequest(project.getString(ProtocolConstants.KEY_CONTENT_LOCATION));
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		project = new JSONObject(response.getText());
		JSONObject gitSection = project.optJSONObject(GitConstants.KEY_GIT);
		assertNotNull(gitSection);
		String gitRemoteUri = gitSection.getString(GitConstants.KEY_REMOTE);

		request = getGetGitRemoteRequest(gitRemoteUri);
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		JSONObject remotes = new JSONObject(response.getText());
		JSONArray remotesArray = remotes.getJSONArray(ProtocolConstants.KEY_CHILDREN);
		assertEquals(1, remotesArray.length());
		JSONObject remote = remotesArray.getJSONObject(0);
		assertNotNull(remote);
		String remoteLocation = remote.getString(ProtocolConstants.KEY_LOCATION);
		assertNotNull(remoteLocation);

		URI u = URI.create(remoteLocation);
		IPath p = new Path(u.getPath());
		p = p.uptoSegment(2).append("xxx").append(p.removeFirstSegments(3));
		URI nu = new URI(u.getScheme(), u.getUserInfo(), u.getHost(), u.getPort(), p.toString(), u.getQuery(), u.getFragment());

		request = getGetGitRemoteRequest(nu.toString());
		response = webConversation.getResponse(request);
		response = waitForTaskCompletionObjectResponse(response);
		if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
			JSONObject responseObject = new JSONObject(response.getText());
			assertTrue(responseObject.has("Result"));
			assertEquals(HttpURLConnection.HTTP_NOT_FOUND, responseObject.getJSONObject("Result").getInt("HttpCode"));
		} else {
			assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getResponseCode());
		}

		p = new Path(u.getPath());
		p = p.uptoSegment(3).append("xxx").append(p.removeFirstSegments(3));
		nu = new URI(u.getScheme(), u.getUserInfo(), u.getHost(), u.getPort(), p.toString(), u.getQuery(), u.getFragment());

		request = getGetGitRemoteRequest(nu.toString());
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getResponseCode());
	}

	@Test
	public void testGetRemoteCommits() throws Exception {
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
			String gitIndexUri = gitSection.getString(GitConstants.KEY_INDEX);
			String gitHeadUri = gitSection.getString(GitConstants.KEY_HEAD);

			JSONObject remoteBranch = getRemoteBranch(gitRemoteUri, 1, 0, Constants.MASTER);
			assertNotNull(remoteBranch);
			String remoteBranchLocation = remoteBranch.getString(ProtocolConstants.KEY_LOCATION);
			request = getGetGitRemoteRequest(remoteBranchLocation);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			remoteBranch = new JSONObject(response.getText());

			String commitLocation = remoteBranch.getString(GitConstants.KEY_COMMIT);
			JSONArray commitsArray = log(commitLocation);
			assertEquals(1, commitsArray.length());

			// change
			JSONObject testTxt = getChild(folder, "test.txt");
			modifyFile(testTxt, "change");

			// add
			request = GitAddTest.getPutGitIndexRequest(gitIndexUri);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

			// commit
			request = GitCommitTest.getPostGitCommitRequest(gitHeadUri, "new change commit", false);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

			// push
			ServerStatus pushStatus = push(gitRemoteUri, 1, 0, Constants.MASTER, Constants.HEAD, false);
			assertEquals(true, pushStatus.isOK());

			remoteBranch = getRemoteBranch(gitRemoteUri, 1, 0, Constants.MASTER);
			assertNotNull(remoteBranch);
			remoteBranchLocation = remoteBranch.getString(ProtocolConstants.KEY_LOCATION);
			request = getGetGitRemoteRequest(remoteBranchLocation);
			response = webConversation.getResponse(request);
			assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
			remoteBranch = new JSONObject(response.getText());

			commitLocation = remoteBranch.getString(GitConstants.KEY_COMMIT);
			commitsArray = log(commitLocation);
			assertEquals(2, commitsArray.length());

			// TODO: test pushing change from another repo and fetch here

			// creating a fresh repo: we push changes during this tests so the repo would be dirty for the next iteration
			createRepository();
		}
	}

	@Test
	public void testGetRemoteBranches() throws Exception {
		// clone a  repo
		URI workspaceLocation = createWorkspace(getMethodName());
		String workspaceId = workspaceIdFromLocation(workspaceLocation);
		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), null);
		IPath clonePath = getClonePath(workspaceId, project);
		JSONObject clone = clone(clonePath);
		String cloneContentLocation = clone.getString(ProtocolConstants.KEY_CONTENT_LOCATION);
		String cloneLocation = clone.getString(ProtocolConstants.KEY_LOCATION);
		String branchesLocation = clone.getString(GitConstants.KEY_BRANCH);

		// get project metadata
		WebRequest request = getGetFilesRequest(project.getString(ProtocolConstants.KEY_CONTENT_LOCATION));
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		project = new JSONObject(response.getText());

		JSONObject gitSection = project.getJSONObject(GitConstants.KEY_GIT);
		String gitRemoteUri = gitSection.getString(GitConstants.KEY_REMOTE);

		Repository db1 = getRepositoryForContentLocation(cloneContentLocation);
		Git git = new Git(db1);
		int localBefore = git.branchList().call().size();
		int remoteBefore = git.branchList().setListMode(ListMode.REMOTE).call().size();
		int allBefore = git.branchList().setListMode(ListMode.ALL).call().size();
		branch(branchesLocation, "a");

		assertEquals(1, git.branchList().call().size() - localBefore);
		assertEquals(0, git.branchList().setListMode(ListMode.REMOTE).call().size() - remoteBefore);
		assertEquals(1, git.branchList().setListMode(ListMode.ALL).call().size() - allBefore);

		// push all
		// TODO: replace with REST API when bug 339115 is fixed
		git.push().setPushAll().call();

		assertEquals(1, git.branchList().call().size() - localBefore);
		assertEquals(1, git.branchList().setListMode(ListMode.REMOTE).call().size() - remoteBefore);
		assertEquals(2, git.branchList().setListMode(ListMode.ALL).call().size() - allBefore);

		checkoutBranch(cloneLocation, Constants.MASTER);
		JSONObject remoteBranch = getRemoteBranch(gitRemoteUri, 2, 0, Constants.MASTER);
		assertNotNull(remoteBranch);

		checkoutBranch(cloneLocation, "a");
		remoteBranch = getRemoteBranch(gitRemoteUri, 2, 0, "a");
		assertNotNull(remoteBranch);
	}

	@Test
	public void testAddRemoveRemote() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		String workspaceId = workspaceIdFromLocation(workspaceLocation);
		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), null);
		IPath clonePath = getClonePath(workspaceId, project);
		JSONObject clone = clone(clonePath);
		String remotesLocation = clone.getString(GitConstants.KEY_REMOTE);

		// expect only origin
		getRemote(remotesLocation, 1, 0, Constants.DEFAULT_REMOTE_NAME);

		// create remote
		WebResponse response = addRemote(remotesLocation, "a", "https://a.b");
		String remoteLocation = response.getHeaderField(ProtocolConstants.HEADER_LOCATION);
		String remoteLocationFromBody = new JSONObject(response.getText()).getString(ProtocolConstants.KEY_LOCATION);
		assertEquals(remoteLocation, remoteLocationFromBody);

		// check details
		WebRequest request = getGetRequest(remoteLocation);
		response = webConversation.getResponse(request);
		waitForTaskCompletion(response);

		// list remotes
		request = getGetRequest(remotesLocation);
		response = webConversation.getResponse(request);
		JSONObject remotes = waitForTaskCompletion(response);
		JSONArray remotesArray = remotes.getJSONArray(ProtocolConstants.KEY_CHILDREN);
		// expect origin and new remote
		assertEquals(2, remotesArray.length());

		// remove remote
		request = getDeleteGitRemoteRequest(remoteLocation);
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

		// list remotes again, make sure it's gone
		request = getGetRequest(remotesLocation);
		response = webConversation.getResponse(request);
		remotes = waitForTaskCompletion(response);
		remotesArray = remotes.getJSONArray(ProtocolConstants.KEY_CHILDREN);
		// expect origin only
		assertEquals(1, remotesArray.length());
		getRemote(remotesLocation, 1, 0, Constants.DEFAULT_REMOTE_NAME);
	}

	@Test
	public void testRemoteProperties() throws IOException, SAXException, JSONException, CoreException, URISyntaxException {
		URI workspaceLocation = createWorkspace(getMethodName());
		String workspaceId = workspaceIdFromLocation(workspaceLocation);
		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), null);
		IPath clonePath = getClonePath(workspaceId, project);
		JSONObject clone = clone(clonePath);
		String remotesLocation = clone.getString(GitConstants.KEY_REMOTE);
		project.getString(ProtocolConstants.KEY_CONTENT_LOCATION);

		// create remote
		final String remoteName = "remote1";
		final String remoteUri = "remote1.com";
		final String fetchRefSpec = "+refs/heads/*:refs/remotes/%s/*";
		final String pushUri = "remote2.com";
		final String pushRefSpec = "refs/heads/*:refs/heads/*";
		addRemote(remotesLocation, remoteName, remoteUri, fetchRefSpec, pushUri, pushRefSpec);

		Repository db2 = new FileRepository(GitUtils.getGitDir(clonePath));
		StoredConfig config = db2.getConfig();
		RemoteConfig rc = new RemoteConfig(config, remoteName);

		assertNotNull(rc);
		// main uri
		assertEquals(1, rc.getURIs().size());
		assertEquals(new URIish(remoteUri), rc.getURIs().get(0));
		// fetchRefSpec
		assertEquals(1, rc.getFetchRefSpecs().size());
		assertEquals(new RefSpec(fetchRefSpec), rc.getFetchRefSpecs().get(0));
		// pushUri
		assertEquals(1, rc.getPushURIs().size());
		assertEquals(new URIish(pushUri), rc.getPushURIs().get(0));
		// pushRefSpec
		assertEquals(1, rc.getPushRefSpecs().size());
		assertEquals(new RefSpec(pushRefSpec), rc.getPushRefSpecs().get(0));
	}

	static WebRequest getGetGitRemoteRequest(String location) {
		String requestURI;
		if (location.startsWith("http://"))
			requestURI = location;
		else if (location.startsWith("/"))
			requestURI = SERVER_LOCATION + location;
		else
			requestURI = SERVER_LOCATION + GIT_SERVLET_LOCATION + Remote.RESOURCE + location;
		WebRequest request = new GetMethodWebRequest(requestURI);
		request.setHeaderField(ProtocolConstants.HEADER_ORION_VERSION, "1");
		setAuthentication(request);
		return request;
	}

	static WebRequest getPostGitRemoteRequest(String location, String name, String uri, String fetchRefSpec, String pushUri, String pushRefSpec) throws JSONException, UnsupportedEncodingException {
		String requestURI;
		if (location.startsWith("http://"))
			requestURI = location;
		else if (location.startsWith("/"))
			requestURI = SERVER_LOCATION + location;
		else
			requestURI = SERVER_LOCATION + GIT_SERVLET_LOCATION + Remote.RESOURCE + location;
		JSONObject body = new JSONObject();
		body.put(GitConstants.KEY_REMOTE_NAME, name);
		body.put(GitConstants.KEY_REMOTE_URI, uri);
		body.put(GitConstants.KEY_REMOTE_FETCH_REF, fetchRefSpec);
		body.put(GitConstants.KEY_REMOTE_PUSH_URI, pushUri);
		body.put(GitConstants.KEY_REMOTE_PUSH_REF, pushRefSpec);
		WebRequest request = new PostMethodWebRequest(requestURI, IOUtilities.toInputStream(body.toString()), "UTF-8");
		request.setHeaderField(ProtocolConstants.HEADER_ORION_VERSION, "1");
		setAuthentication(request);
		return request;
	}

	static WebRequest getDeleteGitRemoteRequest(String location) {
		String requestURI;
		if (location.startsWith("http://")) {
			requestURI = location;
		} else {
			requestURI = SERVER_LOCATION + GIT_SERVLET_LOCATION + Remote.RESOURCE + location;
		}
		WebRequest request = new DeleteMethodWebRequest(requestURI);
		request.setHeaderField(ProtocolConstants.HEADER_ORION_VERSION, "1");
		setAuthentication(request);
		return request;
	}

	static void assertOnBranch(Git git, String branch) throws IOException {
		assertNotNull(git.getRepository().getRef(branch));
	}
}