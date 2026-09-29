package com.freshworks.hagrid.main.dsl.init;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.freshworks.hagrid.main.dsl.config.requests.RequestSpec;
import com.freshworks.hagrid.main.dsl.config.requests.RequestSpecHandler;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;


@Configuration
public class RequestTemplateBean {
    

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTemplateBean.class);

    @Bean
    public RequestSpec requestSpec(InitConfig initConfig) throws Exception {
        
        RequestSpec requestSpec = new RequestSpec();

        if(initConfig.getRequestDslPath() == null){

            return parseResourceDsl(requestSpec);
        }
        
        else{

            Path requestsDirectory = Paths.get(initConfig.getRequestDslPath());
            List<File> requestSpecFileList = new ArrayList<>();

             // Ensure the path actually exists and is a directory before traversing
            if (!Files.isDirectory(requestsDirectory)) {
                throw new IllegalArgumentException("Provided path is not a valid directory: " + requestsDirectory);
            }

            // Try-with-resources is critical here to ensure the underlying OS file handles are closed
            try (Stream<Path> stream = Files.walk(requestsDirectory)) {
            
                stream
                    .filter(Files::isRegularFile)          
                    .map(Path::toFile)                     
                    .forEach( file -> requestSpecFileList.add(file));   
                    
                return parseDsl(requestSpec, requestSpecFileList);

            } catch (IOException e) {
                // Handle file system access issues (e.g., permission denied)
                System.err.println("Error traversing directory: " + e.getMessage());
            }
        }

        return requestSpec;
    }


    public RequestSpec parseDsl(RequestSpec requestSpec, List<File> fileList) throws Exception{

        RequestSpecHandler requestSpecHandler =  new RequestSpecHandler(requestSpec);
        
        Binding binding = new Binding();
        GroovyShell groovyShell = new GroovyShell(binding);

        Closure<Void> requestsClosure = new Closure<Void>(null) {
            public void doCall(Closure closure) {
                requestSpecHandler.requests(closure);
            }
        };
        binding.setVariable("requests", requestsClosure);

        for(File file : fileList){
            groovyShell.evaluate(file);
        }

        return requestSpec;

    }


    public RequestSpec parseResourceDsl(RequestSpec requestSpec) throws Exception{

        String REQUESTS_DSL_RESOURCE = "requests.groovy";
        RequestSpecHandler requestSpecHandler =  new RequestSpecHandler(requestSpec);
        
        Binding binding = new Binding();
        GroovyShell groovyShell = new GroovyShell(binding);

        Closure<Void> requestsClosure = new Closure<Void>(null) {
            public void doCall(Closure closure) {
                requestSpecHandler.requests(closure);
            }
        };
        binding.setVariable("requests", requestsClosure);

        ClassPathResource resource = new ClassPathResource(REQUESTS_DSL_RESOURCE);
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            groovyShell.evaluate(reader);
        }

        return requestSpec;
    }
    
}
