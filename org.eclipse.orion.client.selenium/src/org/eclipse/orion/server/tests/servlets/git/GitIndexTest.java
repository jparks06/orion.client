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

import java.net.HttpURLConnection;
import java.net.URI;

import org.eclipse.orion.internal.server.servlets.ProtocolConstants;
import org.eclipse.orion.server.git.GitConstants;
import org.eclipse.orion.server.git.objects.Index;
import org.json.JSONObject;
import org.junit.Test;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class GitIndexTest extends GitTest {
	@Test
	public void testIndexModifiedByOrion() throws Exception {
		URI workspaceLocation = createWorkspace(getMethodName());

		String projectName = getMethodName();
		JSONObject project = createProjectOrLink(workspaceLocation, projectName, gitDir.toString());

		JSONObject testTxt = getChild(project, "test.txt");
		modifyFile(testTxt, "hello");

		JSONObject gitSection = testTxt.getJSONObject(GitConstants.KEY_GIT);
		String gitIndexUri = gitSection.getString(GitConstants.KEY_INDEX);

		WebRequest request = getGetGitIndexRequest(gitIndexUri);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		assertEquals("test", response.getText());
	}

	/**
	 * Creates a request to get the content of the file stored in index.
	 * @param location Either an absolute URI, or a workspace-relative URI
	 */
	static WebRequest getGetGitIndexRequest(String location) {
		String requestURI;
		if (location.startsWith("http://"))
			requestURI = location;
		else if (location.startsWith("/"))
			requestURI = SERVER_LOCATION + location;
		else
			requestURI = SERVER_LOCATION + GIT_SERVLET_LOCATION + Index.RESOURCE + location;
		WebRequest request = new GetMethodWebRequest(requestURI);
		request.setHeaderField(ProtocolConstants.HEADER_ORION_VERSION, "1");
		setAuthentication(request);
		return request;
	}
}
