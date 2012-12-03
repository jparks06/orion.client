package org.eclipse.orion.client.navigation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class SitesPage extends PageObject {

	public static final String SITES_PAGE_URL = "http://localhost:8080/sites/sites.html";
	public static final String SITES_PAGE_TITLE = "Sites";

	public SitesPage(WebDriver driver) {
		super(driver);
	}

	public static SitesPage navigateTo(WebDriver driver) {
		driver.get(SITES_PAGE_URL);
		return PageFactory.initElements(driver, SitesPage.class);
	}

}
