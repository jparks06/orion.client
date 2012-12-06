/**
 * 
 */
package org.eclipse.orion.client.navigation;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The main pane in the Navigator page.
 */
public class NavigatorMainPane extends NavigatorPage {	

	public static final String NAVIGATOR_PAGE_URL = "http://localhost:8080/navigate/table.html";

	@FindBy(xpath = "/html/body/div/div[3]/div/div/ul/li/span")
	WebElement newButton;
	@FindBy(id="Folder")
	WebElement newFolderButton;	
	WebElement linkToServerButton;
	
	@FindBy(xpath="/html/body/div/div[3]/div/div/ul[2]/li/span")
	WebElement actionsButton;
	@FindBy(id="Delete")
	WebElement deleteMenuItem;
	@FindBy(id="Rename")
	WebElement renameMenuItem;
	
	@FindBy(id="nameparameterCollector")
	WebElement nameInput;	
	@FindBy(xpath="/html/body/div/div[3]/div/div/div[3]/span[2]/span")
	public WebElement submitButton;
	
	@FindBy(id="explorer-treeinnerTree")
	public WebElement folderTree;	
	
	Actions action = new Actions(driver);
	WebDriverWait webWait = new WebDriverWait(driver, 10);
	
	public NavigatorMainPane(WebDriver driver) {
		super(driver);
	}
	
	public static NavigatorMainPane navigateTo(WebDriver driver) {
		driver.get(NAVIGATOR_PAGE_URL);
		return PageFactory.initElements(driver, NavigatorMainPane.class);
	}
	
	/**
	 * Creates a new folder with the given name, 
	 * by clicking New->Folder.
	 */
	public void createNewFolder(String folderName){		
		
		action.click(newButton).perform();
		webWait.until(ExpectedConditions.visibilityOf(newFolderButton));
		
		action.click(newFolderButton).perform();
		webWait.until(ExpectedConditions.visibilityOf(nameInput));
		
		nameInput.clear();
		nameInput.sendKeys(folderName);
		
		action.click(submitButton).perform();
		
		webWait.until(ExpectedConditions.presenceOfElementLocated(By.linkText(folderName)));	
	}
	
	/**
	 * Deletes a folder of the given name,
	 * by first selecting the row containing the folder,
	 * then clicking Action -> Delete.
	 */
	public void deleteFolder(String folderName){
		//Select the column containing the text TestFolder
		selectFolderRow(folderName);
		
		//Click on Actions button
		action.click(actionsButton).perform();			
		webWait.until(ExpectedConditions.presenceOfElementLocated(By.id("Delete")));
		
		//Click on Delete
		action.click(deleteMenuItem).perform();
		webWait.until(ExpectedConditions.alertIsPresent());
		
		//Click OK in the confirm dialog
		driver.switchTo().alert().accept();
		//driver.findElements(By.linkText("TestFolder")).size() == 0
		//webWait.until(ExpectedConditions.stalenessOf(driver.findElement(By.linkText(folderName))));
	}

	/**
	 * Renames the given folder to the given name,
	 * by first selecting the row containing the folder,
	 * then clicking Action -> Rename
	 */
	public void renameFolder(String folderName, String newName){
		//Select the column containing the text TestFolder
		selectFolderRow(folderName);		
		WebElement parent = getParentOfFolder(folderName);
		
		//Click on Actions button
		action.click(actionsButton).perform();			
		webWait.until(ExpectedConditions.presenceOfElementLocated(By.id("Rename")));				
		
		action.click(renameMenuItem).perform();			

		WebElement input = parent.findElement(By.xpath("div/div[2]/input"));
		
		input.clear();
		input.sendKeys(newName);
		input.sendKeys(Keys.RETURN);
		
		webWait.until(ExpectedConditions.presenceOfElementLocated(By.linkText(newName)));	
	}
	
	/**
	 * Selects the row containing the given folder.
	 * @param folderName
	 */
	public void selectFolderRow(String folderName){
		WebElement parent = getParentOfFolder(folderName);
		action.moveToElement(parent).click(parent).perform();
	}
	
	public WebElement getParentOfFolder(String folderName){
		WebElement folder = driver.findElement(By.linkText(folderName));
		return folder.findElement(By.xpath(".."));		
	}
}
