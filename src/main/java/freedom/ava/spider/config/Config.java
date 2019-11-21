package freedom.ava.spider.config;


import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import freedom.ava.spider.entity.VocabularyMessage;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import javax.sql.DataSource;
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
    PhantomJSDriver phantomJSDriver() {
        DesiredCapabilities dcaps = new DesiredCapabilities();
        dcaps.setCapability("acceptSslCerts", true);
        dcaps.setCapability("takesScreenshot", true);
        dcaps.setCapability("cssSelectorsEnabled", true);
        dcaps.setJavascriptEnabled(true);
        dcaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, properties.getPhantomjsPath());

        PhantomJSDriver driver = new PhantomJSDriver(dcaps);
        //设置隐性等待（作用于全局）
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);

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
