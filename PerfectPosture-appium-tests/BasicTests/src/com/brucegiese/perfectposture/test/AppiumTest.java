package com.brucegiese.perfectposture.test;

import io.appium.java_client.AppiumDriver;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AppiumTest {
	
	private AppiumDriver driver;
	
	@BeforeMethod
	public void setUp() throws Exception {
		
		// set up appium
		File classpathRoot = new File(System.getProperty("user.dir"));
		File appDir = new File(classpathRoot, "Application");
		File app = new File(appDir, "PerfectPosture.apk");
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability("platformName", "Android");
		capabilities.setCapability(CapabilityType.VERSION, "4.4");
		capabilities.setCapability("deviceName", "9ecad15a");
		capabilities.setCapability("app", app.getAbsolutePath());
		capabilities.setCapability("appPackage", "com.brucegiese.perfectposture");
		capabilities.setCapability("appActivity", ".PerfectPostureActivity");
		// Capabilities needed for these tests in particular.
		capabilities.setCapability("databaseEnabled", true);
		capabilities.setCapability("rotatable", true);
		capabilities.setCapability("device","selendroid");

		driver = new AppiumDriver(new URL("http://127.0.0.1:4723/wd/hub"),capabilities);
	}
	
	@AfterMethod
	public void tearDown() throws Exception {
		driver.quit();
	}
	
	/**
	 * Basic sanity test to make sure things are working.
	 * @throws InterruptedException
	 */
	@Test
	public void smokeTest() throws InterruptedException {
		startService();
		goToScreen("DATA");
		verifyRealTimeChartData();
		goToScreen("SETTINGS");

		// uncheck all the checkboxes
		changeCheckboxSetting("Send notification icons to the top of the screen to alert bad posture", false);
		changeCheckboxSetting("Allow short vibrations to indicate bad posture", false);
		changeCheckboxSetting("Turn on/off LED to alert bad posture (not implemented yet)", false);
		changeCheckboxSetting("Get an alert every 15 minutes to do a chin tuck exercise", false);
		// check all the checkboxes
		changeCheckboxSetting("Send notification icons to the top of the screen to alert bad posture", true);
		changeCheckboxSetting("Allow short vibrations to indicate bad posture", true);
		changeCheckboxSetting("Turn on/off LED to alert bad posture (not implemented yet)", true);
		changeCheckboxSetting("Get an alert every 15 minutes to do a chin tuck exercise", true);
		
		goToScreen("DATA");
		goToScreen("INTRO");
		stopService();
	}
	
	
	/**
	 * Test rotations		(CURRENTLY DISABLED)
	 * @throws InterruptedException
	 */
//	@Test
	public void rotations() throws InterruptedException {
		
		// Make sure this we can rotate the screen
		Capabilities actualCapabilities = ((RemoteWebDriver)driver).getCapabilities();
		assert( actualCapabilities.getCapability("rotatable").equals(true));
		
		startService();
		driver.rotate(ScreenOrientation.LANDSCAPE);
		stopService();
		driver.rotate(ScreenOrientation.PORTRAIT);
		startService();
		goToScreen("DATA");
		verifyRealTimeChartData();
		driver.rotate(ScreenOrientation.LANDSCAPE);
		verifyRealTimeChartData();
		goToScreen("SETTINGS");
		driver.rotate(ScreenOrientation.PORTRAIT);
		goToScreen("DATA");
		verifyRealTimeChartData();
		goToScreen("INTRO");
		stopService();
	}
	
	
	private void sleepTwoSeconds() {
		// wait for 2 seconds
		try {
			Thread.sleep(2000);
		} catch( InterruptedException e) { }
	}
	
	/**
	 * This only works if you're already on the INTRO page and the service is not already started.
	 */
	private void startService() {
		driver.findElement(By.id("com.brucegiese.perfectposture:id/start_stop_button")).click();
	}
	
	/**
	 * This only works if you're already on the INTRO page and the service is already running.
	 */
	private void stopService() {
		driver.findElement(By.name("Stop Posture Detection")).click();
	}
	
	/**
	 * This only works if you're already on an adjacent screen.
	 */
	private void goToScreen(String name) {
		driver.findElement(By.name(name)).click();
	}
	
	/**
	 * This only works if you're already on the DATA screen.
	 */
	private void verifyRealTimeChartData() {
		WebElement chart1 = driver.findElement(By.id("com.brucegiese.perfectposture:id/chart"));
		String text1 = chart1.getAttribute("name");
		
		sleepTwoSeconds();
		
		WebElement chart2 = driver.findElement(By.id("com.brucegiese.perfectposture:id/chart"));
		String text2 = chart2.getAttribute("name");
		
		assert(text1.contains("index is "));
		assert(text2.contains(", value is "));
		
		// format of text is "index is " + integer + ", value is " + float
		Pattern p = Pattern.compile("index is (\\d+), value is (\\d*.\\d+)");
		Matcher m1 = p.matcher(text1);
		Matcher m2 = p.matcher(text2);

		m1.find();
		int index1 = Integer.valueOf(m1.group(1));
		float value1 = Float.valueOf(m1.group(2));
		m2.find();
		int index2 = Integer.valueOf(m2.group(1));
		float value2 = Float.valueOf(m2.group(2));

		assert( index1 < index2);
		assert(value1 == value2);		// This assumes no one is handling the device during testing.
	}
	
	
	/**
	 * Checks the setting checkbox with the given string as text.
	 * @param checkboxText	look for the checkbox with this string
	 * @param check			true means to check the checkbox.  false means uncheck it
	 * @return true means the checkbox was NOT already checked.  false means it was already in the
	 * 			requested state and we did nothing.
	 */
	private boolean changeCheckboxSetting(String checkboxText, boolean check) {
		WebElement setting =
				driver.findElement(By.xpath("//android.widget.TextView[@text='" + checkboxText + "']/../.."));
		WebElement checkbox =
				setting.findElement(By.className("android.widget.LinearLayout")).findElement(By.className("android.widget.CheckBox"));

		if( check == checkbox.getAttribute("checked").equals("true") ) {
			System.out.println("Checkbox " + checkboxText + " is already in the requested state");
			return false;
		}
		
		setting.click();
		
		// Make sure the checkbox is now in the requested state
		assert( check == checkbox.getAttribute("checked").equals("true"));
		return true;
	}
}
