package org.eclipse.orion.client.tests.navigation;

import java.util.concurrent.TimeUnit;

import org.eclipse.orion.client.navigation.LoginPage;
import org.eclipse.orion.client.navigation.ManageUsersPage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class CleanUp {
	private WebDriver driver;
	private LoginPage loginPage;
	private ManageUsersPage manageUsersPage;
	
	@Before
	public void setup() throws Exception {
		driver = new FirefoxDriver();
		driver.manage().window().maximize();
		loginPage = LoginPage.navigateTo(driver);
		driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
	}
	
	@Test
	public void cleanup(){
		loginPage.orionAccountLogin("admin", "password");
		manageUsersPage = ManageUsersPage.goToManageUsersPage(driver);		
		
		manageUsersPage.deleteUser("test");
		
	}
	
	@After
	public void tearDown() throws Exception {
		driver.quit();
	}
}
