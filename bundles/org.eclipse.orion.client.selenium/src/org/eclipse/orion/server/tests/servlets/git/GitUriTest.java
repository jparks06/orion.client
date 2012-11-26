/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.orion.internal.server.servlets.ProtocolConstants;
import org.eclipse.orion.internal.server.servlets.workspace.ServletTestingSupport;
import org.eclipse.orion.server.git.GitConstants;
import org.json.JSONObject;
import org.junit.Test;

public class GitUriTest extends GitTest {
	@Test
	public void testGitUrisAfterLinkingToExistingClone() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());

		String projectName = getMethodName();
		JSONObject project = createProjectOrLink(workspaceLocation, projectName, gitDir.toString());
		assertGitSectionExists(project);
		// TODO: it's a linked repo, see bug 346114
		// assertCloneUri(gitSection.optString(GitConstants.KEY_CLONE, null));
	}

	@Test
	public void testGitUrisInContentLocation() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());

		String projectName = getMethodName();
		// http://<host>/workspace/<workspaceId>/
		JSONObject newProject = createProjectOrLink(workspaceLocation, projectName, gitDir.toString());
		String contentLocation = newProject.optString(ProtocolConstants.KEY_CONTENT_LOCATION, null);
		assertNotNull(contentLocation);

		// http://<host>/file/<projectId>/
		WebRequest request = getGetFilesRequest(contentLocation);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		JSONObject project = new JSONObject(response.getText());
		assertGitSectionExists(project);
		// TODO: it's a linked repo, see bug 346114
		// assertCloneUri(gitSection.optString(GitConstants.KEY_CLONE, null));

		String childrenLocation = project.getString(ProtocolConstants.KEY_CHILDREN_LOCATION);
		assertNotNull(childrenLocation);

		// http://<host>/file/<projectId>/?depth=1
		request = getGetFilesRequest(childrenLocation);
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		List<JSONObject> children = getDirectoryChildren(new JSONObject(response.getText()));
		String[] expectedChildren = new String[] {Constants.DOT_GIT, "folder", "test.txt"};
		assertEquals("Wrong number of directory children", expectedChildren.length, children.size());
		for (JSONObject child : children) {
			assertGitSectionExists(child);
			// TODO: it's a linked repo, see bug 346114
			// assertCloneUri(gitSection.optString(GitConstants.KEY_CLONE, null));
		}
		childrenLocation = getChildByName(children, "folder").getString(ProtocolConstants.KEY_CHILDREN_LOCATION);

		// http://<host>/file/<projectId>/folder/?depth=1
		request = getGetFilesRequest(childrenLocation);
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		children = getDirectoryChildren(new JSONObject(response.getText()));
		expectedChildren = new String[] {"folder.txt"};
		assertEquals("Wrong number of directory children", expectedChildren.length, children.size());
		for (JSONObject child : children) {
			assertGitSectionExists(child);
			// TODO: it's a linked repo, see bug 346114
			// assertCloneUri(gitSection.optString(GitConstants.KEY_CLONE, null));
		}
	}

	@Test
	public void testGitUrisForEmptyDir() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());

		File emptyDir = AllGitTests.getRandomLocation().toFile();
		emptyDir.mkdir();
		ServletTestingSupport.allowedPrefixes = emptyDir.toString();

		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), emptyDir.toString());
		project.getString(ProtocolConstants.KEY_ID);
		String location = project.getString(ProtocolConstants.KEY_CONTENT_LOCATION);

		WebRequest request = getGetFilesRequest(location);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		JSONObject files = new JSONObject(response.getText());

		// FIXME: these assertions do nothing useful
		assertNull(files.optString(GitConstants.KEY_STATUS, null));
		assertNull(files.optString(GitConstants.KEY_DIFF, null));
		assertNull(files.optString(GitConstants.KEY_DIFF, null));
		assertNull(files.optString(GitConstants.KEY_COMMIT, null));
		assertNull(files.optString(GitConstants.KEY_REMOTE, null));
		assertNull(files.optString(GitConstants.KEY_TAG, null));
		assertNull(files.optString(GitConstants.KEY_CLONE, null));

		assertTrue(emptyDir.delete());
	}

	@Test
	public void testGitUrisForFile() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());

		File dir = AllGitTests.getRandomLocation().toFile();
		dir.mkdir();
		File file = new File(dir, "test.txt");
		file.createNewFile();

		ServletTestingSupport.allowedPrefixes = dir.toString();

		String projectName = getMethodName();
		JSONObject project = createProjectOrLink(workspaceLocation, projectName, dir.toString());
		String location = project.getString(ProtocolConstants.KEY_CONTENT_LOCATION);

		WebRequest request = getGetFilesRequest(location);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		JSONObject files = new JSONObject(response.getText());

		assertNull(files.optString(GitConstants.KEY_STATUS, null));
		assertNull(files.optString(GitConstants.KEY_DIFF, null));
		assertNull(files.optString(GitConstants.KEY_DIFF, null));
		assertNull(files.optString(GitConstants.KEY_COMMIT, null));
		assertNull(files.optString(GitConstants.KEY_REMOTE, null));
		assertNull(files.optString(GitConstants.KEY_TAG, null));
		assertNull(files.optString(GitConstants.KEY_CLONE, null));

		FileUtils.delete(dir, FileUtils.RECURSIVE);
	}

	@Test
	public void testGitUrisForRepositoryClonedIntoSubfolder() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());
		String workspaceId = workspaceIdFromLocation(workspaceLocation);
		JSONObject project = createProjectOrLink(workspaceLocation, getMethodName(), null);
		String folderName = "subfolder";
		WebRequest request = getPostFilesRequest("", getNewDirJSON(folderName).toString(), folderName);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_CREATED, response.getResponseCode());
		IPath clonePath = getClonePath(workspaceId, project).append(folderName).makeAbsolute();
		clone(clonePath);

		String location = project.getString(ProtocolConstants.KEY_CONTENT_LOCATION);
		request = getGetFilesRequest(location);
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		JSONObject responseJSON = new JSONObject(response.getText());

		// no Git section for /file/{projectId}
		assertNull(responseJSON.optString(GitConstants.KEY_STATUS, null));
		assertNull(responseJSON.optString(GitConstants.KEY_DIFF, null));
		assertNull(responseJSON.optString(GitConstants.KEY_DIFF, null));
		assertNull(responseJSON.optString(GitConstants.KEY_COMMIT, null));
		assertNull(responseJSON.optString(GitConstants.KEY_REMOTE, null));
		assertNull(responseJSON.optString(GitConstants.KEY_TAG, null));
		assertNull(responseJSON.optString(GitConstants.KEY_CLONE, null));

		request = getGetFilesRequest(responseJSON.getString(ProtocolConstants.KEY_CHILDREN_LOCATION));
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());

		List<JSONObject> children = getDirectoryChildren(new JSONObject(response.getText()));
		assertEquals(1, children.size());

		// expected Git section for /file/{projectId}/?depth=1
		assertGitSectionExists(children.get(0));
		JSONObject gitSection = children.get(0).getJSONObject(GitConstants.KEY_GIT);
		assertCloneUri(gitSection.optString(GitConstants.KEY_CLONE, null));

		location = children.get(0).getString(ProtocolConstants.KEY_LOCATION);
		request = getGetFilesRequest(location);
		response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		responseJSON = new JSONObject(response.getText());

		// expected Git section for /file/{projectId}/subfolder
		assertGitSectionExists(responseJSON);
		gitSection = responseJSON.getJSONObject(GitConstants.KEY_GIT);
		assertCloneUri(gitSection.optString(GitConstants.KEY_CLONE, null));
	}

}
