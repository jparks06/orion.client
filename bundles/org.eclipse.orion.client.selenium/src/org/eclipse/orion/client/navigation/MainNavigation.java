package org.eclipse.orion.client.navigation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class MainNavigation extends PageObject {

	public static final String CLIENT_HOME_URL = "http://localhost:8080/navigate/table.html";
	public static final String CLIENT_HOME_TITLE = "Navigator";

	public static final String NAV_HOME_LINK_TEXT = "Navigator";
	public static final String SITES_HOME_LINK_TEXT = "Sites";
	public static final String REPOS_HOME_LINK_TEXT = "Repositories";
	public static final String SHELL_HOME_LINK_TEXT = "Shell";
	public static final String PLUGINS_HOME_LINK_TEXT = "Get Plugins";
	
	public static final String ORION_HOME_LINK_URL = "http://localhost:8080/navigate/table.html";
	public static final String NAV_HOME_LINK_URL = "http://localhost:8080/navigate/table.html#";
	public static final String SITES_HOME_LINK_URL = "http://localhost:8080/sites/sites.html";
	public static final String REPOS_HOME_LINK_URL = "http://localhost:8080/git/git-repository.html#";
	public static final String SHELL_HOME_LINK_URL = "http://localhost:8080/shell/shellPage.html";
	public static final String PLUGINS_HOME_LINK_URL = "http://mamacdon.github.com/#?target=http://localhost:8080/settings/settings.html&version=1.0&OrionHome=http://localhost:8080";
	
	@FindBy(id = "home")
	public WebElement orionHomeLink;
	@FindBy(xpath = "//nav[@id='primaryNav']/a")
	public WebElement navigatorHomeLink;
	@FindBy(xpath = "//nav[@id='primaryNav']/a[2]")
	public WebElement sitesHomeLink;
	@FindBy(xpath = "//nav[@id='primaryNav']/a[3]")
	public WebElement repositoriesHomeLink;
	@FindBy(xpath = "//nav[@id='primaryNav']/a[4]")
	public WebElement shellHomeLink;
	@FindBy(xpath = "//nav[@id='primaryNav']/a[5]")
	public WebElement getPluginsHomeLink;
	
	public MainNavigation(WebDriver driver) {
		super(driver);
	}

	public static MainNavigation navigateTo(WebDriver driver) {
		driver.get(CLIENT_HOME_URL);
		return PageFactory.initElements(driver, MainNavigation.class);
	}

	public void getNavLinksText() {
		orionHomeLink.getText();
		navigatorHomeLink.getText();
		sitesHomeLink.getText();
		repositoriesHomeLink.getText();
		shellHomeLink.getText();
		getPluginsHomeLink.getText();		
	}
	
	public void getNavLinksLink() {
		orionHomeLink.getAttribute("href");
		navigatorHomeLink.getAttribute("href");
		sitesHomeLink.getAttribute("href");
		repositoriesHomeLink.getAttribute("href");
		shellHomeLink.getAttribute("href");
		getPluginsHomeLink.getAttribute("href");
	}
}