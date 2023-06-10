package com.jdaalba.words.config;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.Closeable;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SeleniumConfig implements Closeable {

    private final WebDriver driver;

    public WebDriver getDriver() {
        return driver;
    }

    public SeleniumConfig() {
        Capabilities capabilities = DesiredCapabilities.firefox();
        var opt = new FirefoxOptions();
        opt.addCapabilities(capabilities);
        opt.setLogLevel(Level.SEVERE);
        driver = new FirefoxDriver(opt);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    }

    static {
        System.setProperty("webdriver.gecko.driver", findFile("geckodriver_" + getDriverSuffix()));
    }

    private static String getDriverSuffix() {
        return System.getProperty("SUFFIX", "arm64_mac");
    }

    static private String findFile(String filename) {
        String[] paths = {"", "bin/", "target/classes"};
        for (String path : paths) {
            if (new File(path + filename).exists()) {
                return path + filename;
            }
        }
        return "";
    }

    @Override
    public void close() {
        driver.quit();
    }
}
