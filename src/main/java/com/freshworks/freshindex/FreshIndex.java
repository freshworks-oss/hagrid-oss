package com.freshworks.freshindex;

import com.freshworks.freshindex.index.JsonIndexService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SpringBootApplication
public class FreshIndex {

    public static void main(String[] args) throws Exception{


        SpringApplication.run(FreshIndex.class, args);


    }

}
