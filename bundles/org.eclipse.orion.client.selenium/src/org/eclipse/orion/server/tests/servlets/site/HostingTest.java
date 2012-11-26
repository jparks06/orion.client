package org.eclipse.orion.server.tests.servlets.site;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.orion.internal.server.servlets.ProtocolConstants;
import org.eclipse.orion.internal.server.servlets.site.SiteConfigurationConstants;
import org.eclipse.orion.internal.server.servlets.workspace.authorization.AuthorizationService;
import org.eclipse.orion.server.useradmin.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * Basic tests:
 * - Start, stop a site
 * - Access workspace files through paths on the running site
 * - Access a remote URL through paths on the running site
 * - Test starting site via PUT and POST (start on creation)
 * 
 * Security tests:
 * - Try to walk the workspace using ../ in hosted site path (should 404)
 * 
 * Concurrency tests (done concurrently on many threads)
 * - Create a file with unique name 
 * - Create a new site that exposes the file
 * - Start the site
 * - Access the site, verify file content.
 * - Stop the site
 * - Attempt to access the file again, verify that request 404s (or times out?)
 */
public class HostingTest extends CoreSiteTest {

	private String workspaceId;

	@BeforeClass
	public static void setUpWorkspace() {
		initializeWorkspaceLocation();
	}

	@Before
	/**
	 * Before each test, create a workspace and prepare fields for use by test methods.
	 */
	public void setUp() throws Exception {
		clearWorkspace();
		webConversation = new WebConversation();
		webConversation.setExceptionsThrownOnErrorStatus(false);
		setUpAuthorization();
		createTestProject("HostingTest");
		workspaceId = new Path(testProjectBaseLocation).segment(0);
	}

	@Test
	public void testStartSite() throws SAXException, IOException, JSONException, URISyntaxException {
		JSONArray mappings = makeMappings(new String[][] {{"/", "/A/bogusWorkspacePath"}});
		WebResponse siteResp = createSite("Fizz site", workspaceId, mappings, "fizzsite", null);
		JSONObject siteObject = new JSONObject(siteResp.getText());
		startSite(siteObject.getString(ProtocolConstants.KEY_LOCATION));
	}

	@Test
	public void testStartSiteNoMappings() throws SAXException, IOException, JSONException, URISyntaxException {
		// Empty array
		JSONArray mappings = makeMappings(new String[0][0]);
		WebResponse siteResp = createSite("Empty mappings site", workspaceId, mappings, "empty", null);
		JSONObject siteObject = new JSONObject(siteResp.getText());
		startSite(siteObject.getString(ProtocolConstants.KEY_LOCATION));

		// No mappings at all
		WebResponse siteResp2 = createSite("Null mappings site", workspaceId, null, "null", null);
		JSONObject siteObject2 = new JSONObject(siteResp2.getText());
		startSite(siteObject2.getString(ProtocolConstants.KEY_LOCATION));
	}

	@Test
	public void testStopSite() throws SAXException, IOException, JSONException, URISyntaxException {
		JSONArray mappings = makeMappings(new String[][] {{"/", "/A/bogusWorkspacePath"}});
		WebResponse siteResp = createSite("Buzz site", workspaceId, mappings, "buzzsite", null);
		JSONObject siteObject = new JSONObject(siteResp.getText());
		String location = siteObject.getString(ProtocolConstants.KEY_LOCATION);
		startSite(location);
		stopSite(location);
	}

	@Test
	/**
	 * Tests accessing a workspace file <del>and remote URL</del> that are part of a running site.
	 */
	public void testSiteAccess() throws SAXException, IOException, JSONException, URISyntaxException {
		// Create file in workspace
		final String filename = "foo.html";
		final String fileContent = "<html><body>This is a test file</body></html>";
		createFileOnServer(filename, fileContent);

		// Create a site that exposes the workspace file
		final String siteName = "My hosted site";
		//make absolute by adding test project path
		String filePath = URI.create(makeResourceURIAbsolute("/" + filename)).getPath();
		//remove /file segment to get the servlet-relative path
		Assert.assertTrue(filePath.startsWith("/file/"));
		filePath = filePath.substring(5);
		final String mountAt = "/file.html";

		final JSONArray mappings = makeMappings(new String[][] {{mountAt, filePath}});
		WebRequest createSiteReq = getCreateSiteRequest(siteName, workspaceId, mappings, null);
		WebResponse createSiteResp = webConversation.getResponse(createSiteReq);
		assertEquals(HttpURLConnection.HTTP_CREATED, createSiteResp.getResponseCode());
		JSONObject siteObject = new JSONObject(createSiteResp.getText());

		// Start the site
		final String siteLocation = siteObject.getString(ProtocolConstants.KEY_LOCATION);//createSiteResp.getHeaderField("Location");
		siteObject = startSite(siteLocation);

		final JSONObject hostingStatus = siteObject.getJSONObject(SiteConfigurationConstants.KEY_HOSTING_STATUS);
		final String hostedURL = hostingStatus.getString(SiteConfigurationConstants.KEY_HOSTING_STATUS_URL);

		// Access the workspace file through the site
		WebRequest getFileReq = new GetMethodWebRequest(hostedURL + mountAt);
		WebResponse getFileResp = webConversation.getResponse(getFileReq);
		assertEquals(fileContent, getFileResp.getText());

		// Stop the site
		stopSite(siteLocation);

		// Check that the workspace file can't be accessed anymore
		WebRequest getFile404Req = new GetMethodWebRequest(hostedURL + mountAt);
		WebResponse getFile404Resp = webConversation.getResponse(getFile404Req);
		assertEquals(HttpURLConnection.HTTP_NOT_FOUND, getFile404Resp.getResponseCode());
		assertEquals("no-cache", getFile404Resp.getHeaderField("Cache-Control").toLowerCase());
	}

	@Test
	/**
	 * Tests accessing a workspace file and that it has the expected MIME type.
	 */
	public void testSiteFileMime() throws SAXException, IOException, JSONException, URISyntaxException {
		// Expose a directory named my.css and ensure it doesn't get "text/css" content-type
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=389252
		final String filename = "foo.html";
		final String fileContent = "<html><body>This is a test file</body></html>";
		final String dirName = "/my.css";
		createDirectoryOnServer(dirName);
		createFileOnServer(dirName + "/" + filename, fileContent);

		String filePath = URI.create(makeResourceURIAbsolute("/" + dirName)).getPath();
		Assert.assertTrue(filePath.startsWith("/file/"));
		filePath = filePath.substring(5);
		final String mountAt = "/";

		final JSONArray mappings = makeMappings(new String[][] {{mountAt, filePath}});
		WebRequest createSiteReq = getCreateSiteRequest("testMime", workspaceId, mappings, null);
		WebResponse createSiteResp = webConversation.getResponse(createSiteReq);
		assertEquals(HttpURLConnection.HTTP_CREATED, createSiteResp.getResponseCode());
		JSONObject siteObject = new JSONObject(createSiteResp.getText());

		// Start site
		final String siteLocation = siteObject.getString(ProtocolConstants.KEY_LOCATION);//createSiteResp.getHeaderField("Location");
		siteObject = startSite(siteLocation);

		// Access the directory through the site
		final JSONObject hostingStatus = siteObject.getJSONObject(SiteConfigurationConstants.KEY_HOSTING_STATUS);
		final String hostedURL = hostingStatus.getString(SiteConfigurationConstants.KEY_HOSTING_STATUS_URL);
		WebRequest getFileReq = new GetMethodWebRequest(hostedURL + mountAt);
		WebResponse getFileResp = webConversation.getResponse(getFileReq);
		assertEquals(true, getFileResp.getText().contains(filename));
		assertEquals(false, "text/html".equals(getFileResp.getHeaderField("Content-Type").toLowerCase()));

		// Stop the site
		stopSite(siteLocation);
	}

	@Test
	// Test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=382760 
	public void testDisallowedSiteAccess() throws SAXException, IOException, JSONException, URISyntaxException, CoreException {
		User userBObject = createUser("userB", "userB");
		String userB = userBObject.getLogin();

		// User "test": create file in test's workspace
		final String filename = "foo.html";
		final String fileContent = "<html><body>This is a test file</body></html>";
		WebResponse createdFile = createFileOnServer(filename, fileContent);
		URL fileLocation = createdFile.getURL();
		IPath filepath = new Path(fileLocation.getPath());
		filepath = filepath.removeFirstSegments(new Path(FILE_SERVLET_LOCATION).segmentCount()); // chop off leading /file/
		filepath = filepath.removeLastSegments(1); // chop off trailing /foo.html
		filepath = filepath.makeAbsolute();
		filepath = filepath.addTrailingSeparator();
		String parentFolder = filepath.toString();

		// User B: Create a workspace that User B has access to
		WebResponse createWorkspaceResp = basicCreateWorkspace("userB");
		String bWorkspaceId = new JSONObject(createWorkspaceResp.getText()).getString(ProtocolConstants.KEY_ID);
		AuthorizationService.addUserRight(userBObject.getUid(), createWorkspaceResp.getURL().getPath());
		AuthorizationService.addUserRight(userBObject.getUid(), createWorkspaceResp.getURL().getPath() + "/*");

		// User B: create a site against B's workspace that exposes a file in test's workspace
		final String siteName = "My hosted site";
		final String filePath = parentFolder; //"/" + filename;
		final String mountAt = "/"; //"/file.html";
		final JSONArray mappings = makeMappings(new String[][] {{mountAt, filePath}});

		WebRequest createSiteReq = getCreateSiteRequest(siteName, bWorkspaceId, mappings, null);
		setAuthentication(createSiteReq, userB, userB);
		WebResponse createSiteResp = webConversation.getResponse(createSiteReq);
		assertEquals(HttpURLConnection.HTTP_CREATED, createSiteResp.getResponseCode());
		JSONObject siteObject = new JSONObject(createSiteResp.getText());

		// User B: Start the site
		final String siteLocation = siteObject.getString(ProtocolConstants.KEY_LOCATION);//createSiteResp.getHeaderField("Location");
		siteObject = startSite(siteLocation, userB, userB);

		final JSONObject hostingStatus = siteObject.getJSONObject(SiteConfigurationConstants.KEY_HOSTING_STATUS);
		final String hostedURL = hostingStatus.getString(SiteConfigurationConstants.KEY_HOSTING_STATUS_URL);

		// Attempt to access file on user B's site, should fail
		WebRequest getFileReq = new GetMethodWebRequest(hostedURL + mountAt);
		WebResponse getFileResp = webConversation.getResponse(getFileReq);
		assertEquals(HttpURLConnection.HTTP_FORBIDDEN, getFileResp.getResponseCode());
	}

	@Test
	public void testRemoteProxyRequest() throws SAXException, IOException, JSONException, URISyntaxException {
		final String siteName = "My remote hosting site";
		final String remoteRoot = "/remoteWeb", remotePrefPath = "/remotePref", remoteFilePath = "/remoteFile";

		final JSONArray mappings = makeMappings(new String[][] { {remoteRoot, SERVER_LOCATION}, {remoteFilePath, SERVER_LOCATION + FILE_SERVLET_LOCATION}, {remotePrefPath, SERVER_LOCATION + "/prefs"}});
		WebRequest createSiteReq = getCreateSiteRequest(siteName, workspaceId, mappings, null);
		WebResponse createSiteResp = webConversation.getResponse(createSiteReq);
		assertEquals(HttpURLConnection.HTTP_CREATED, createSiteResp.getResponseCode());
		JSONObject siteObject = new JSONObject(createSiteResp.getText());

		// Start the site
		final String siteLocation = siteObject.getString(ProtocolConstants.KEY_LOCATION);//createSiteResp.getHeaderField("Location");
		siteObject = startSite(siteLocation);

		final JSONObject hostingStatus = siteObject.getJSONObject(SiteConfigurationConstants.KEY_HOSTING_STATUS);
		final String hostedURL = hostingStatus.getString(SiteConfigurationConstants.KEY_HOSTING_STATUS_URL);

		// Access the remote URL through the site
		WebRequest getRemoteUrlReq = new GetMethodWebRequest(hostedURL + remoteRoot);
		WebResponse getRemoteUrlResp = webConversation.getResource(getRemoteUrlReq);
		assertEquals("GET " + getRemoteUrlReq.getURL() + " succeeds", HttpURLConnection.HTTP_OK, getRemoteUrlResp.getResponseCode());
		final String content = getRemoteUrlResp.getText();
		assertEquals("Looks like Orion", true, content.contains("Orion") && content.contains("<script"));

		// Test that we can invoke the Orion file API through the site, to create a file
		final String fileName = "fizz.txt";
		final String fileContent = "Created through a site";
		createFileOnServer(hostedURL + remoteFilePath + testProjectBaseLocation, fileName, fileContent);

		// Bugs 369813, 366098, 369811, 390732: ensure query parameters are passed through the site unmangled
		// For this we'll call the 'prefs' API which uses query parameters
		String prefKey = "foo[-]bar baz+quux";
		String prefValue = "pref value";
		String remotePrefUrl = hostedURL + remotePrefPath + "/user";
		WebRequest putPrefReq = createSetPreferenceRequest(remotePrefUrl, prefKey, prefValue);
		setAuthentication(putPrefReq);
		WebResponse putPrefResp = webConversation.getResponse(putPrefReq);
		assertEquals(HttpURLConnection.HTTP_NO_CONTENT, putPrefResp.getResponseCode());

		// Check pref value
		WebRequest getPrefReq = new GetMethodWebRequest(remotePrefUrl + "?key=" + URLEncoder.encode(prefKey));
		setAuthentication(getPrefReq);
		WebResponse getPrefResp = webConversation.getResponse(getPrefReq);
		assertEquals(HttpURLConnection.HTTP_OK, getPrefResp.getResponseCode());
		JSONObject prefObject = new JSONObject(getPrefResp.getText());
		assertEquals("Pref obtained through site has correct value", prefObject.optString(prefKey), prefValue);

		// Stop the site
		stopSite(siteLocation);

		// Check that remote URL can't be accessed anymore
		WebRequest getRemoteUrl404Req = new GetMethodWebRequest(hostedURL + remoteRoot);
		WebResponse getRemoteUrl404Resp = webConversation.getResponse(getRemoteUrl404Req);
		assertEquals(HttpURLConnection.HTTP_NOT_FOUND, getRemoteUrl404Resp.getResponseCode());
	}

	/**
	 * Starts the site at <code>siteLocation</code>, and asserts that it was started.
	 * @throws URISyntaxException 
	 * @returns The JSON representation of the started site.
	 */
	private JSONObject startSite(String siteLocation, String user, String password) throws JSONException, IOException, SAXException, URISyntaxException {
		JSONObject hostingStatus = new JSONObject();
		hostingStatus.put(SiteConfigurationConstants.KEY_HOSTING_STATUS_STATUS, "started");
		WebRequest launchSiteReq = getUpdateSiteRequest(siteLocation, null, null, null, null, hostingStatus);
		if (user != null && password != null)
			setAuthentication(launchSiteReq, user, password);
		WebResponse launchSiteResp = webConversation.getResponse(launchSiteReq);
		assertEquals(launchSiteResp.getText(), HttpURLConnection.HTTP_OK, launchSiteResp.getResponseCode());

		// Check that it's started
		JSONObject siteObject = new JSONObject(launchSiteResp.getText());
		hostingStatus = siteObject.getJSONObject(SiteConfigurationConstants.KEY_HOSTING_STATUS);
		assertEquals("started", hostingStatus.getString(SiteConfigurationConstants.KEY_HOSTING_STATUS_STATUS));
		return siteObject;
	}

	private JSONObject startSite(String siteLocation) throws JSONException, IOException, SAXException, URISyntaxException {
		return startSite(siteLocation, null, null);
	}

	/**
	 * Stops the site at <code>siteLocation</code>, and asserts that it was stopped.
	 * @throws URISyntaxException 
	 */
	private void stopSite(final String siteLocation) throws JSONException, IOException, SAXException, URISyntaxException {
		JSONObject hostingStatus = new JSONObject();
		hostingStatus.put(SiteConfigurationConstants.KEY_HOSTING_STATUS_STATUS, "stopped");
		WebRequest stopReq = getUpdateSiteRequest(siteLocation, null, null, null, null, hostingStatus);
		WebResponse stopResp = webConversation.getResponse(stopReq);
		assertEquals(HttpURLConnection.HTTP_OK, stopResp.getResponseCode());

		// Check that it's stopped
		JSONObject siteObject = new JSONObject(stopResp.getText());
		hostingStatus = siteObject.getJSONObject(SiteConfigurationConstants.KEY_HOSTING_STATUS);
		assertEquals("stopped", hostingStatus.getString(SiteConfigurationConstants.KEY_HOSTING_STATUS_STATUS));
	}

	/**
	 * Creates a file using the file POST API, then sets its contents with PUT.
	 * @param filename
	 * @param fileContent
	 */
	private WebResponse createFileOnServer(String filename, String fileContent) throws SAXException, IOException, JSONException, URISyntaxException {
		return createFileOnServer("/", filename, fileContent);
	}

	private void createDirectoryOnServer(String dirname) throws SAXException, IOException, JSONException {
		webConversation.setExceptionsThrownOnErrorStatus(false);
		WebRequest createDirReq = getPostFilesRequest("/", getNewDirJSON(dirname).toString(), dirname);
		WebResponse createDirResp = webConversation.getResponse(createDirReq);
		assertEquals(HttpURLConnection.HTTP_CREATED, createDirResp.getResponseCode());
	}

	private WebResponse createFileOnServer(String fileServletLocation, String filename, String fileContent) throws SAXException, IOException, JSONException, URISyntaxException {
		webConversation.setExceptionsThrownOnErrorStatus(false);
		WebRequest createFileReq = getPostFilesRequest(fileServletLocation, getNewFileJSON(filename).toString(), filename);
		WebResponse createFileResp = webConversation.getResponse(createFileReq);
		assertEquals(HttpURLConnection.HTTP_CREATED, createFileResp.getResponseCode());
		createFileReq = getPutFileRequest(createFileResp.getHeaderField("Location"), fileContent);
		createFileResp = webConversation.getResponse(createFileReq);
		assertEquals(HttpURLConnection.HTTP_OK, createFileResp.getResponseCode());
		return createFileResp;
	}

	private WebRequest createSetPreferenceRequest(String location, String key, String value) {
		String body = "key=" + URLEncoder.encode(key) + "&value=" + URLEncoder.encode(value);
		return new PutMethodWebRequest(location, new ByteArrayInputStream(body.getBytes()), "application/x-www-form-urlencoded");
	}
}
