package cn.coderead.testspring;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@MapperScan
public class TestSpringApplication {


    public static void main(String[] args) {
        SpringApplication.run(TestSpringApplication.class, args);
    }

}
