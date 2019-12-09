package freedom.ava.spider.config;


import freedom.ava.spider.entity.VocabularyMessage;
import org.mybatis.spring.annotation.MapperScan;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

@Configuration
@MapperScan(basePackages = {"freedom.ava.spider.service"})
public class Config {

    @Autowired
    private Properties properties;

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        return loggingFilter;
    }

    @Bean
    WebDriver phantomJSDriver() throws Exception {
        DesiredCapabilities dcaps = new DesiredCapabilities();
        dcaps.setCapability("acceptSslCerts", true);
        dcaps.setCapability("takesScreenshot", true);
        dcaps.setCapability("cssSelectorsEnabled", true);
        dcaps.setJavascriptEnabled(true);
        dcaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, properties.getPhantomjsPath());

//        PhantomJSDriver driver = new PhantomJSDriver(dcaps);
        WebDriver driver = new RemoteWebDriver(
                new URL(properties.getPhantomjsPath()),
                DesiredCapabilities.phantomjs());
        //设置隐性等待（作用于全局）
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

        return driver;
    }


    @Bean(name = "instant_q")
    public LinkedList<VocabularyMessage> instantQueue(){
        return new LinkedList<>();
    }

    @Bean(name = "schedule_q")
    public LinkedList<VocabularyMessage> scheduleQueue(){
        return new LinkedList<>();
    }

    @Bean(name = "varBag")
    public HashMap<String,Object> varBag(){
        return new HashMap<>();
    }
}
