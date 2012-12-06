package org.eclipse.orion.client.tests.navigation;

import java.util.concurrent.TimeUnit;

import org.eclipse.orion.client.navigation.LoginPage;
import org.eclipse.orion.client.navigation.NavigatorMainPane;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class NavMainPaneTests {
	private WebDriver driver;
	private LoginPage loginPage;
	NavigatorMainPane mainPane;
	
	@Before
	public void setup() throws Exception {
		driver = new FirefoxDriver();
		driver.manage().window().maximize();
		driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
		loginPage = LoginPage.navigateTo(driver);
		loginPage.orionAccountLogin("admin", "password");
	}
	
	@Test
	public void testCreateAndDeleteFolder(){
		mainPane = NavigatorMainPane.navigateTo(driver);
		mainPane.createNewFolder("TestFolder");
		mainPane.renameFolder("TestFolder", "RenamedFolder");		
		mainPane.deleteFolder("RenamedFolder");
		
//		List<WebElement> folderList = driver.findElements(By.linkText("TestFolder"));
//		new WebDriverWait(driver, 10);
//		assertTrue(folderList.size() == 0 );
	}	
	
	@After
	public void tearDown() throws Exception {
		driver.quit();
	}
}
