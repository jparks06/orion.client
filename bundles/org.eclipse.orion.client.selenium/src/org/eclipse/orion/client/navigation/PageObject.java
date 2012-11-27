package org.eclipse.orion.client.navigation;

import org.openqa.selenium.WebDriver;

public class PageObject {

	protected WebDriver driver;

	public PageObject(WebDriver driver) {
		setDriver(driver);
	}

	public WebDriver getDriver() {
		return driver;
	}

	private void setDriver(WebDriver driver) {
		this.driver = driver;
	}
}