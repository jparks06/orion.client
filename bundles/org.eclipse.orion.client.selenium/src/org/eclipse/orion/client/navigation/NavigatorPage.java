package org.eclipse.orion.client.navigation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class NavigatorPage extends PageObject {

	public static final String NAVIGATOR_PAGE_URL = "http://localhost:8080/navigate/table.html";
	public static final String NAVIGATOR_PAGE_TITLE = "Navigator";
	
	public NavigatorPage(WebDriver driver) {
		super(driver);
	}

	public static NavigatorPage navigateTo(WebDriver driver) {
		driver.get(NAVIGATOR_PAGE_URL);
		return PageFactory.initElements(driver, NavigatorPage.class);
	}
}
