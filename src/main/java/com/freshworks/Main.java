package com.freshworks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ApplicationContext;

/**
 * Hello world!
 *
 */
@SpringBootApplication(scanBasePackages = {"com.freshworks"}, exclude = {DataSourceAutoConfiguration.class})
public class Main
{
    public static void main( String[] args )
    {
        ApplicationContext applicationContext = SpringApplication.run(Main.class, args);
        Initialization initialization = applicationContext.getBean(Initialization.class);
//        initialization.run();
    }
}