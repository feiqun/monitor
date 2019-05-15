package cn.com.feiqun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author feiqun
 * @description:
 * @date 2019/3/14
 */
@SpringBootApplication
@ComponentScan(basePackages = "cn.com.feiqun")
@EnableSwagger2
@EnableCaching
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
