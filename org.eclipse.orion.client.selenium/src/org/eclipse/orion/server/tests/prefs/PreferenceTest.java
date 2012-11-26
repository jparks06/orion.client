/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.tests.prefs;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.meterware.httpunit.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.orion.internal.server.core.IOUtilities;
import org.eclipse.orion.server.core.users.OrionScope;
import org.eclipse.orion.server.tests.AbstractServerTest;
import org.eclipse.orion.server.tests.ServerTestsActivator;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Tests for the preference servlet.
 */
public class PreferenceTest extends AbstractServerTest {
	WebConversation webConversation;

	@Before
	public void setUp() throws BackingStoreException, CoreException {
		OrionScope prefs = new OrionScope();
		clearPreferences(prefs.getNode("Users"));
		clearPreferences(prefs.getNode("Workspaces"));
		clearPreferences(prefs.getNode("Projects"));
		webConversation = new WebConversation();
		webConversation.setExceptionsThrownOnErrorStatus(false);
		setUpAuthorization();
	}

	private void clearPreferences(IEclipsePreferences prefs) throws BackingStoreException {
		//clear  children
		for (String child : prefs.childrenNames()) {
			prefs.node(child).removeNode();
		}
		prefs.clear();
		prefs.flush();
	}

	@Test
	public void testGetSingle() throws IOException, JSONException {
		List<String> locations = getTestPreferenceNodes();
		for (String location : locations) {
			//unknown key should return 404
			WebRequest request = new GetMethodWebRequest(location + "?key=Name");
			setAuthentication(request);
			WebResponse response = webConversation.getResource(request);
			assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getResponseCode());

			//put a value
			request = createSetPreferenceRequest(location, "Name", "Frodo");
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals("1." + location, HttpURLConnection.HTTP_NO_CONTENT, response.getResponseCode());

			//now doing a get should succeed
			request = new GetMethodWebRequest(location + "?key=Name");
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals("2." + location, HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject result = new JSONObject(response.getText());
			assertEquals("3." + location, "Frodo", result.optString("Name"));

			//getting another key on the same resource should still 404
			request = new GetMethodWebRequest(location + "?key=Address");
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getResponseCode());
		}
	}

	@Test
	public void testPutSingle() throws IOException, JSONException {
		List<String> locations = getTestPreferenceNodes();
		for (String location : locations) {
			//put a value that isn't currently defined
			WebRequest request = createSetPreferenceRequest(location, "Name", "Frodo");
			setAuthentication(request);
			WebResponse response = webConversation.getResource(request);
			assertEquals("1." + location, HttpURLConnection.HTTP_NO_CONTENT, response.getResponseCode());

			//doing a get should succeed
			request = new GetMethodWebRequest(location + "?key=Name");
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals("2." + location, HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject result = new JSONObject(response.getText());
			assertEquals("3." + location, "Frodo", result.optString("Name"));

			//setting a value to the empty string
			request = createSetPreferenceRequest(location, "Name", "");
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals("4." + location, HttpURLConnection.HTTP_NO_CONTENT, response.getResponseCode());
			request = new GetMethodWebRequest(location + "?key=Name");
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals("5." + location, HttpURLConnection.HTTP_OK, response.getResponseCode());
			result = new JSONObject(response.getText());
			assertEquals("6." + location, "", result.optString("Name"));

			//putting with forbidden URL characters in key and value
			request = createSetPreferenceRequest(location, "Na=me", "Fr&do");
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals("1." + location, HttpURLConnection.HTTP_NO_CONTENT, response.getResponseCode());

			//doing a get should succeed
			request = new GetMethodWebRequest(location + "?key=Na%3Dme");
			setAuthentication(request);
			response = webConversation.getResource(request);
			result = new JSONObject(response.getText());
			assertEquals("3." + location, "Fr&do", result.optString("Na=me"));
		}
	}

	/**
	 * Tests setting JSON objects as preference values
	 * 
	 * @throws IOException
	 * @throws JSONException
	 */
	@Test
	public void testPutJSON() throws IOException, JSONException {
		List<String> locations = getTestPreferenceNodes();
		for (String location : locations) {
			//PUT http://myserver:8080/prefs/user/cm/configurations/jslint.config
			//{"properties":{"options":"foo:true, bar:false"}}
			JSONObject value = new JSONObject();
			String options = "foo:true, bar:false";
			value.put("options", options);
			JSONObject prefs = new JSONObject();
			prefs.put("properties", value);
			String inString = prefs.toString();
			WebRequest request = new PutMethodWebRequest(location, IOUtilities.toInputStream(inString), "application/json");
			setAuthentication(request);
			WebResponse response = webConversation.getResource(request);
			assertEquals("1." + location, HttpURLConnection.HTTP_NO_CONTENT, response.getResponseCode());

			//GET http://myserver:8080/prefs/user/cm/configurations/jslint.config
			//should return: same value we put in
			request = new GetMethodWebRequest(location);
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals("2." + location, HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject resultObject = new JSONObject(response.getText());
			assertTrue("3." + location, resultObject.has("properties"));
			JSONObject resultValue = resultObject.getJSONObject("properties");
			Object resultOptions = resultValue.get("options");
			assertEquals("4." + location, options, resultOptions);

		}
		//
		//but...
		//
		//GET http://myserver:8080/prefs/user/cm/configurations/jslint.config
		//{
		//  "properties" : "{\"options\":\"foo:true, bar:false\"}"
		//}
	}

	@Test
	public void testPutNode() throws IOException, JSONException {
		List<String> locations = getTestPreferenceNodes();
		for (String location : locations) {
			//put a node that isn't currently defined
			JSONObject prefs = new JSONObject();
			prefs.put("Name", "Frodo");
			prefs.put("Address", "Bag End");
			WebRequest request = new PutMethodWebRequest(location, IOUtilities.toInputStream(prefs.toString()), "application/json");
			setAuthentication(request);
			WebResponse response = webConversation.getResource(request);
			assertEquals("1." + location, HttpURLConnection.HTTP_NO_CONTENT, response.getResponseCode());

			//doing a get should succeed
			request = new GetMethodWebRequest(location + "?key=Address");
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals("2." + location, HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject result = new JSONObject(response.getText());
			assertEquals("3." + location, "Bag End", result.optString("Address"));

			//setting a node with disjoint values should clear values not in common
			prefs = new JSONObject();
			prefs.put("Name", "Barliman");
			prefs.put("Occupation", "Barkeep");
			request = new PutMethodWebRequest(location, IOUtilities.toInputStream(prefs.toString()), "application/json");
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals("4." + location, HttpURLConnection.HTTP_NO_CONTENT, response.getResponseCode());
			request = new GetMethodWebRequest(location);
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals("5." + location, HttpURLConnection.HTTP_OK, response.getResponseCode());
			result = new JSONObject(response.getText());
			assertEquals("6." + location, "Barliman", result.optString("Name"));
			assertFalse("7." + location, result.has("Address"));//this value was previously defined but the put node should clean it
			assertEquals("8." + location, "Barkeep", result.optString("Occupation"));
		}
	}

	@Test
	public void testDeleteSingle() {
		//TODO not implemented
	}

	@Test
	public void testDeleteNode() {

	}

	@Test
	public void testValueWithSpaces() throws IOException, JSONException {
		//put a value
		String location = getTestPreferenceNodes().get(0);
		WebRequest request = createSetPreferenceRequest(location, "Name", "Frodo Baggins");
		setAuthentication(request);
		WebResponse response = webConversation.getResource(request);
		assertEquals(HttpURLConnection.HTTP_NO_CONTENT, response.getResponseCode());

		//now doing a get should succeed
		request = new GetMethodWebRequest(location + "?key=Name");
		setAuthentication(request);
		response = webConversation.getResource(request);
		assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
		JSONObject result = new JSONObject(response.getText());
		assertEquals("Frodo Baggins", result.optString("Name"));
	}

	/**
	 * Tests whether a client can access workspace metadata via the preferences servlet.
	 * @throws IOException 
	 */
	@Test
	public void testAccessingMetadata() throws IOException {
		List<String> locations = getIllegalPreferenceNodes();
		for (String location : locations) {
			//get should return 405
			WebRequest request = new GetMethodWebRequest(location);
			setAuthentication(request);
			WebResponse response = webConversation.getResource(request);
			assertEquals(HttpURLConnection.HTTP_BAD_METHOD, response.getResponseCode());

			//put a value should be 403
			request = createSetPreferenceRequest(location, "Name", "Frodo");
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals(HttpURLConnection.HTTP_BAD_METHOD, response.getResponseCode());
		}
	}

	@Test
	public void testGetNode() throws IOException, JSONException {
		List<String> locations = getTestPreferenceNodes();
		for (String location : locations) {
			//unknown node should return 404
			WebRequest request = new GetMethodWebRequest(location);
			setAuthentication(request);
			WebResponse response = webConversation.getResource(request);
			assertEquals("1." + location, HttpURLConnection.HTTP_NOT_FOUND, response.getResponseCode());

			//put a value
			request = createSetPreferenceRequest(location, "Name", "Frodo");
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals("2." + location, HttpURLConnection.HTTP_NO_CONTENT, response.getResponseCode());

			//now doing a get should succeed
			request = new GetMethodWebRequest(location);
			setAuthentication(request);
			response = webConversation.getResource(request);
			assertEquals("3." + location, HttpURLConnection.HTTP_OK, response.getResponseCode());
			JSONObject result = new JSONObject(response.getText());
			assertEquals("4." + location, "Frodo", result.optString("Name"));
		}
	}

	private WebRequest createSetPreferenceRequest(String location, String key, String value) {
		String body = "key=" + URLEncoder.encode(key) + "&value=" + URLEncoder.encode(value);
		return new PutMethodWebRequest(location, new ByteArrayInputStream(body.getBytes()), "application/x-www-form-urlencoded");
	}

	private List<String> getTestPreferenceNodes() {
		return Arrays.asList(ServerTestsActivator.getServerLocation() + "/prefs/user/testprefs", ServerTestsActivator.getServerLocation() + "/prefs/workspace/myworkspace/testprefs", ServerTestsActivator.getServerLocation() + "/prefs/project/myproject/testprefs");
	}

	/**
	 * Returns preference nodes the client should not have access to.
	 */
	private List<String> getIllegalPreferenceNodes() {
		return Arrays.asList(ServerTestsActivator.getServerLocation() + "/prefs/Users", ServerTestsActivator.getServerLocation() + "/prefs/Workspaces", ServerTestsActivator.getServerLocation() + "/prefs/Projects");
	}
}
