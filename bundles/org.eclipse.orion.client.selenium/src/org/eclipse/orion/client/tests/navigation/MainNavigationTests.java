package org.eclipse.orion.client.tests.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.eclipse.orion.client.navigation.GetPluginsPage;
import org.eclipse.orion.client.navigation.LoginPage;
import org.eclipse.orion.client.navigation.MainNavigation;
import org.eclipse.orion.client.navigation.NavigatorPage;
import org.eclipse.orion.client.navigation.ShellPage;
import org.eclipse.orion.client.navigation.SitesPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class MainNavigationTests {
	
	private WebDriver driver;
	private LoginPage loginPage;
	private MainNavigation clientHome;

	@Before
	public void setup() throws Exception {
		driver = new FirefoxDriver();
		driver.manage().window().maximize();
		driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
		loginPage = LoginPage.navigateTo(driver);
		loginPage.orionAccountLogin("jparks", "pass");
		clientHome = MainNavigation.navigateTo(driver);
	}

	@Test
	public void testPageExists() {
		(new WebDriverWait(driver, 10)).until(ExpectedConditions.visibilityOf(clientHome.orionHomeLink));
		assertTrue(driver.getTitle().contains(MainNavigation.CLIENT_HOME_TITLE));
	}

	@Test
	public void verifyNavLinkText() {
		assertEquals(MainNavigation.NAV_HOME_LINK_TEXT, clientHome.navigatorHomeLink.getText());
		assertEquals(MainNavigation.SITES_HOME_LINK_TEXT, clientHome.sitesHomeLink.getText());
		assertEquals(MainNavigation.REPOS_HOME_LINK_TEXT, clientHome.repositoriesHomeLink.getText());
		assertEquals(MainNavigation.SHELL_HOME_LINK_TEXT, clientHome.shellHomeLink.getText());
		assertEquals(MainNavigation.PLUGINS_HOME_LINK_TEXT, clientHome.getPluginsHomeLink.getText());
	}
	
	@Test
	public void verifyNavLinkUrls() {
		assertEquals(MainNavigation.NAV_HOME_LINK_URL, clientHome.navigatorHomeLink.getAttribute("href"));
	}
	
	@Test
	public void testOrionHomeLink() {
		clientHome.orionHomeLink.click();
		assertTrue(driver.getTitle().contains(MainNavigation.CLIENT_HOME_TITLE));
	}
	
	@Test
	public void testNavigatorHomeLink() {
		clientHome.navigatorHomeLink.click();
		assertTrue(driver.getTitle().contains(NavigatorPage.NAVIGATOR_PAGE_TITLE));
	}
	
	@Test
	public void testSitesHomeLink() {
		clientHome.sitesHomeLink.click();
		assertTrue(driver.getTitle().contains(SitesPage.SITES_PAGE_TITLE));
	}
	
	@Test
	public void testShellHomeLink() {
		clientHome.shellHomeLink.click();
		assertTrue(driver.getTitle().contains(ShellPage.SHELL_PAGE_TITLE));
	}
	
	@Test
	public void testGetPluginsHomeLink() {
		clientHome.getPluginsHomeLink.click();
		assertTrue(driver.getTitle().contains(GetPluginsPage.GET_PLUGINS_PAGE_TITLE));
	}

	@After
	public void tearDown() throws Exception {
		driver.quit();
	}
}
