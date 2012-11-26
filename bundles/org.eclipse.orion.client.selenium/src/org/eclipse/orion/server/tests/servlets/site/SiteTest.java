package org.eclipse.orion.server.tests.servlets.site;

import static org.junit.Assert.assertEquals;

import com.meterware.httpunit.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.orion.internal.server.servlets.ProtocolConstants;
import org.eclipse.orion.internal.server.servlets.site.SiteConfigurationConstants;
import org.eclipse.orion.server.core.users.OrionScope;
import org.json.*;
import org.junit.*;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.xml.sax.SAXException;

/**
 * Tests for the site configurations API.
 * 
 * Basic tests:
 * - Create (POST)
 * - Retrieve (GET)
 * - Update (PUT)
 * - Delete (DELETE)
 * 
 * Security tests:
 * - User A tries to access user B's site
 */
public class SiteTest extends CoreSiteTest {

	private WebResponse workspaceResponse;
	private JSONObject workspaceObject;

	@BeforeClass
	public static void setUpWorkspace() {
		initializeWorkspaceLocation();
	}

	@Before
	/**
	 * Before each test, create a workspace and prepare fields for use by test methods.
	 */
	public void setUp() throws CoreException, SAXException, IOException, JSONException {
		clearWorkspace();
		webConversation = new WebConversation();
		webConversation.setExceptionsThrownOnErrorStatus(false);
		setUpAuthorization();
		workspaceResponse = basicCreateWorkspace(this.getClass().getName());
		workspaceObject = new JSONObject(workspaceResponse.getText());
	}

	@Test
	/**
	 * Create site via POST, check that the response has the parameters we expected.
	 */
	public void testCreateSite() throws IOException, SAXException, JSONException {
		final String siteName = "My great website";
		final String workspaceId = workspaceObject.getString(ProtocolConstants.KEY_ID);
		final String hostHint = "mySite";
		final String source = "/fizz";
		final String target = "/buzz";
		final JSONArray mappings = makeMappings(new String[][] {{source, target}});

		WebRequest request = getCreateSiteRequest(siteName, workspaceId, mappings, hostHint);
		WebResponse siteResponse = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_CREATED, siteResponse.getResponseCode());

		JSONObject respObject = new JSONObject(siteResponse.getText());
		JSONArray respMappings = respObject.getJSONArray(SiteConfigurationConstants.KEY_MAPPINGS);
		assertEquals(siteName, respObject.get(ProtocolConstants.KEY_NAME));
		assertEquals(workspaceId, respObject.get(SiteConfigurationConstants.KEY_WORKSPACE));
		assertEquals(hostHint, respObject.get(SiteConfigurationConstants.KEY_HOST_HINT));
		assertEquals(1, respMappings.length());
		assertEquals(source, respMappings.getJSONObject(0).get(SiteConfigurationConstants.KEY_SOURCE));
		assertEquals(target, respMappings.getJSONObject(0).get(SiteConfigurationConstants.KEY_TARGET));
	}

	@Test
	/**
	 * Attempt to create site with no name, expect 400 Bad Request
	 */
	public void testCreateSiteNoName() throws SAXException, IOException {
		final String siteName = "My great website";
		final String hostHint = "mySite";
		WebRequest request = getCreateSiteRequest(siteName, null, null, hostHint);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.getResponseCode());
	}

	@Test
	/**
	 * Attempt to create site with no workspace, expect 400 Bad Request
	 */
	public void testCreateSiteNoWorkspace() throws SAXException, IOException {
		final String siteName = "My great website";
		final String hostHint = "mySite";
		WebRequest request = getCreateSiteRequest(siteName, null, null, hostHint);
		WebResponse response = webConversation.getResponse(request);
		assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.getResponseCode());
	}

	@Test
	/**
	 * Create a site, then fetch its resource via a GET and check the result.
	 */
	public void testRetrieveSite() throws SAXException, JSONException, IOException, URISyntaxException {
		// Create site
		final String name = "Bob's site";
		final String workspaceId = workspaceObject.getString(ProtocolConstants.KEY_ID);
		final JSONArray mappings = makeMappings(new String[][] { {"/foo", "/A"}, {"/bar", "/B"}});
		final String hostHint = "bobhost";
		WebResponse createResp = createSite(name, workspaceId, mappings, hostHint, null);
		JSONObject site = new JSONObject(createResp.getText());
		final String location = site.getString(ProtocolConstants.HEADER_LOCATION);

		// Fetch site using its Location and ensure that what we find matches what was POSTed
		WebRequest fetchReq = getRetrieveSiteRequest(location, null);
		WebResponse fetchResp = webConversation.getResponse(fetchReq);
		assertEquals(HttpURLConnection.HTTP_OK, fetchResp.getResponseCode());
		JSONObject fetchedSite = new JSONObject(fetchResp.getText());
		assertEquals(name, fetchedSite.optString(ProtocolConstants.KEY_NAME));
		assertEquals(workspaceId, fetchedSite.optString(SiteConfigurationConstants.KEY_WORKSPACE));
		assertEquals(mappings.toString(), fetchedSite.getJSONArray(SiteConfigurationConstants.KEY_MAPPINGS).toString());
		assertEquals(hostHint, fetchedSite.optString(SiteConfigurationConstants.KEY_HOST_HINT));
	}

	@Test
	/**
	 * Create a site, then update it via PUT and check the result.
	 */
	public void testUpdateSite() throws SAXException, JSONException, IOException, URISyntaxException {
		// Create site
		final String name = "A site to update";
		final String workspaceId = workspaceObject.getString(ProtocolConstants.KEY_ID);
		final JSONArray mappings = makeMappings(new String[] {"/", "http://www.google.com"});
		final String hostHint = "orion-is-awesome";
		WebResponse createResp = createSite(name, workspaceId, mappings, hostHint, null);
		JSONObject site = new JSONObject(createResp.getText());
		final String location = site.getString(ProtocolConstants.HEADER_LOCATION);

		// Update site
		final String newName = "A site that was updated";
		final String newWorkspaceId = "" + Math.random(); // Doesn't matter since we won't start it
		final JSONArray newMappings = makeMappings(new String[] {"/some/path", "/XYZ/webRoot"});
		final String newHostHint = "orion-is-awesomer";

		WebRequest updateReq = getUpdateSiteRequest(location, newName, newWorkspaceId, newMappings, newHostHint, null);
		WebResponse updateResp = webConversation.getResponse(updateReq);
		assertEquals(HttpURLConnection.HTTP_OK, updateResp.getResponseCode());
		JSONObject updatedSite = new JSONObject(updateResp.getText());
		assertEquals(newName, updatedSite.optString(ProtocolConstants.KEY_NAME));
		assertEquals(newWorkspaceId, updatedSite.optString(SiteConfigurationConstants.KEY_WORKSPACE));
		assertEquals(newMappings.toString(), updatedSite.getJSONArray(SiteConfigurationConstants.KEY_MAPPINGS).toString());
		assertEquals(newHostHint, updatedSite.optString(SiteConfigurationConstants.KEY_HOST_HINT));
	}

	@Test
	/**
	 * Create a site, then delete it and make sure it's gone.
	 */
	public void testDeleteSite() throws SAXException, JSONException, IOException, URISyntaxException, BackingStoreException {
		// Create site
		final String name = "A site to delete";
		final String workspaceId = workspaceObject.getString(ProtocolConstants.KEY_ID);
		WebResponse createResp = createSite(name, workspaceId, null, null, null);
		JSONObject site = new JSONObject(createResp.getText());
		final String siteId = site.getString(ProtocolConstants.KEY_ID);
		final String location = site.getString(ProtocolConstants.HEADER_LOCATION);

		OrionScope prefs = new OrionScope();
		IEclipsePreferences userSites = prefs.getNode("Users" + "/" + testUserId + "/" + SITE_CONFIG_PREF_NODE);
		IEclipsePreferences sites = prefs.getNode(SITE_CONFIG_PREF_NODE);

		assertEquals(true, sites.nodeExists(siteId));
		assertEquals(true, userSites.nodeExists(siteId));

		// Delete site
		WebRequest deleteReq = getDeleteSiteRequest(location);
		WebResponse deleteResp = webConversation.getResponse(deleteReq);
		assertEquals(HttpURLConnection.HTTP_OK, deleteResp.getResponseCode());

		// GET should fail now
		WebRequest getReq = getRetrieveSiteRequest(location, null);
		WebResponse getResp = webConversation.getResponse(getReq);
		assertEquals(HttpURLConnection.HTTP_NOT_FOUND, getResp.getResponseCode());

		assertEquals(false, sites.nodeExists(siteId));
		assertEquals(false, userSites.nodeExists(siteId));

		// GET all sites should not include the deleted site
		WebRequest getAllReq = getRetrieveAllSitesRequest(null);
		WebResponse getAllResp = webConversation.getResponse(getAllReq);
		JSONObject allSitesJson = new JSONObject(getAllResp.getText());
		JSONArray allSites = allSitesJson.getJSONArray(SiteConfigurationConstants.KEY_SITE_CONFIGURATIONS);
		for (int i = 0; i < allSites.length(); i++) {
			assertEquals(false, allSites.getJSONObject(i).getString(ProtocolConstants.KEY_ID).equals(siteId));
		}
	}

	@Test
	/**
	 * Try to access site created by another user, verify that we can't.
	 */
	public void testDisallowedAccess() throws SAXException, JSONException, IOException, URISyntaxException {
		createUser("alice", "alice");
		createUser("bob", "bob");

		// Alice: Create site
		final String name = "A site to delete";
		final String workspaceId = workspaceObject.getString(ProtocolConstants.KEY_ID);
		WebResponse createResp = createSite(name, workspaceId, null, null, "alice");
		JSONObject site = new JSONObject(createResp.getText());
		final String location = site.getString(ProtocolConstants.HEADER_LOCATION);

		// Alice: Get site
		WebRequest getReq = getRetrieveSiteRequest(location, "alice");
		WebResponse getResp = webConversation.getResponse(getReq);
		assertEquals(HttpURLConnection.HTTP_OK, getResp.getResponseCode());

		// Bob: Attempt to get Alice's site
		getReq = getRetrieveSiteRequest(location, "bob");
		getResp = webConversation.getResponse(getReq);
		assertEquals(HttpURLConnection.HTTP_NOT_FOUND, getResp.getResponseCode());
	}

}
