package com.voxinsight;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.voxinsight.mapper")
public class VoxinsightApplication {

  public static void main(String[] args) {
    SpringApplication.run(VoxinsightApplication.class, args);
  }
}
