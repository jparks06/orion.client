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
package org.eclipse.orion.server.tests.servlets.site;

import static org.junit.Assert.assertEquals;

import com.meterware.httpunit.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import junit.framework.Assert;
import org.eclipse.orion.internal.server.core.IOUtilities;
import org.eclipse.orion.internal.server.servlets.ProtocolConstants;
import org.eclipse.orion.internal.server.servlets.site.SiteConfigurationConstants;
import org.eclipse.orion.server.tests.ServerTestsActivator;
import org.eclipse.orion.server.tests.servlets.files.FileSystemTest;
import org.eclipse.orion.server.tests.servlets.internal.DeleteMethodWebRequest;
import org.json.*;
import org.xml.sax.SAXException;

/**
 * Abstract base class for site and hosting tests.
 */
public abstract class CoreSiteTest extends FileSystemTest {

	public static final String SITE_SERVLET_LOCATION = "/site" + '/';
	public static final String SERVER_LOCATION = ServerTestsActivator.getServerLocation();
	public static final String SITE_CONFIG_PREF_NODE = "SiteConfigurations";

	/**
	 * Turns a Java array-of-arrays {{"/foo","/A"},{"/bar","/B"}} into a mappings array 
	 * [{Source:"/foo", Target:"/A"}, {Source:"/bar", Target:"/B"}]
	 * @param mappings Array where each element is a String[] of the form {Source,Target}
	 */
	protected static JSONArray makeMappings(String[]... mappings) throws JSONException {
		JSONArray result = new JSONArray();
		for (String[] mapping : mappings) {
			if (mapping.length != 2)
				throw new IllegalArgumentException("Not a valid mapping: " + mapping);

			JSONObject mappingObject = new JSONObject();
			mappingObject.put(SiteConfigurationConstants.KEY_SOURCE, mapping[0]);
			mappingObject.put(SiteConfigurationConstants.KEY_TARGET, mapping[1]);
			result.put(mappingObject);
		}
		return result;
	}

	/**
	 * Creates a site and asserts that it was created.
	 * @param mappings Can be null
	 * @param hostHint Can be null
	 * @param user If nonnull, string to use as username and password for auth
	 */
	protected WebResponse createSite(String name, String workspaceId, JSONArray mappings, String hostHint, String user) throws IOException, SAXException {
		WebRequest request = getCreateSiteRequest(name, workspaceId, mappings, hostHint);
		if (user == null)
			setAuthentication(request);
		else
			setAuthentication(request, user, user);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_CREATED, response.getResponseCode());
		return response;
	}

	/**
	 * Returns a request that can create a site.
	 * @param name
	 * @param workspaceId
	 * @param mappings Can be null
	 * @param hostHint Can be null
	 */
	protected WebRequest getCreateSiteRequest(String name, String workspaceId, JSONArray mappings, String hostHint) {
		try {
			String requestURI = SERVER_LOCATION + SITE_SERVLET_LOCATION;
			JSONObject json = new JSONObject();
			json.put(SiteConfigurationConstants.KEY_WORKSPACE, workspaceId);
			if (mappings != null)
				json.put(SiteConfigurationConstants.KEY_MAPPINGS, mappings);
			if (hostHint != null)
				json.put(SiteConfigurationConstants.KEY_HOST_HINT, hostHint);
			WebRequest request = new PostMethodWebRequest(requestURI, IOUtilities.toInputStream(json.toString()), "application/json");
			request.setHeaderField(ProtocolConstants.HEADER_ORION_VERSION, "1");
			request.setHeaderField(ProtocolConstants.HEADER_SLUG, name); // Put name in Slug
			setAuthentication(request);
			return request;
		} catch (UnsupportedEncodingException e) {
			Assert.fail(e.getMessage());
		} catch (JSONException e) {
			Assert.fail(e.getMessage());
		}
		return null;
	}

	/**
	 * @param locationUri
	 * @return Returns a request that will DELETE the site at the given URI.
	 * @throws URISyntaxException 
	 */
	protected WebRequest getDeleteSiteRequest(String locationUri) throws URISyntaxException {
		WebRequest request = new DeleteMethodWebRequest(makeResourceURIAbsolute(locationUri));
		request.setHeaderField(ProtocolConstants.HEADER_ORION_VERSION, "1");
		setAuthentication(request);
		return request;
	}

	/**
	 * @param locationUri
	 * @param user If nonnull, value to use as username and password for auth.
	 * @return Returns a request that will GET the site at the given URI.
	 * @throws URISyntaxException 
	 */
	protected WebRequest getRetrieveSiteRequest(String locationUri, String user) throws URISyntaxException {
		WebRequest request = new GetMethodWebRequest(makeResourceURIAbsolute(locationUri));
		request.setHeaderField(ProtocolConstants.HEADER_ORION_VERSION, "1");
		if (user == null)
			setAuthentication(request);
		else
			setAuthentication(request, user, user);
		return request;
	}

	protected WebRequest getRetrieveAllSitesRequest(String user) throws URISyntaxException {
		WebRequest request = new GetMethodWebRequest(makeResourceURIAbsolute(SERVER_LOCATION + SITE_SERVLET_LOCATION));
		request.setHeaderField(ProtocolConstants.HEADER_ORION_VERSION, "1");
		if (user == null)
			setAuthentication(request);
		else
			setAuthentication(request, user, user);
		return request;
	}

	/**
	 * Returns a request that can update a site.
	 * @param name
	 * @param workspaceId
	 * @param mappings Can be null
	 * @param hostHint Can be null
	 * @param hostingStatus Can be null
	 * @throws URISyntaxException 
	 */
	protected WebRequest getUpdateSiteRequest(String locationUri, String name, String workspaceId, JSONArray mappings, String hostHint, JSONObject hostingStatus) throws URISyntaxException {
		try {
			JSONObject json = new JSONObject();
			json.put(ProtocolConstants.KEY_NAME, name);
			json.put(SiteConfigurationConstants.KEY_WORKSPACE, workspaceId);
			json.putOpt(SiteConfigurationConstants.KEY_MAPPINGS, mappings);
			json.putOpt(SiteConfigurationConstants.KEY_HOST_HINT, hostHint);
			json.putOpt(SiteConfigurationConstants.KEY_HOSTING_STATUS, hostingStatus);
			WebRequest request = new PutMethodWebRequest(makeResourceURIAbsolute(locationUri), IOUtilities.toInputStream(json.toString()), "application/json");
			request.setHeaderField(ProtocolConstants.HEADER_ORION_VERSION, "1");
			setAuthentication(request);
			return request;
		} catch (UnsupportedEncodingException e) {
			Assert.fail(e.getMessage());
		} catch (JSONException e) {
			Assert.fail(e.getMessage());
		}
		return null;
	}
}
