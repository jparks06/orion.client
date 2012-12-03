package org.eclipse.orion.client.navigation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class GetPluginsPage extends PageObject {

	public static final String GET_PLUGINS_PAGE_URL = "http://mamacdon.github.com/#?target=http://localhost:8080/settings/settings.html&version=1.0&OrionHome=http://localhost:8080";
	public static final String GET_PLUGINS_PAGE_TITLE = "Orion Plugins and Tools";

	public GetPluginsPage(WebDriver driver) {
		super(driver);
	}

	public static GetPluginsPage navigateTo(WebDriver driver) {
		driver.get(GET_PLUGINS_PAGE_URL);
		return PageFactory.initElements(driver, GetPluginsPage.class);
	}
}
