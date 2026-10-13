package com.freshworks.hagrid.main.dsl.init;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.freshworks.hagrid.main.dsl.config.model.ModelSpec;
import com.freshworks.hagrid.main.dsl.config.model.ModelSpecHandler;

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

@Configuration
public class ModelSpecBean {

    private static Logger LOGGER = LoggerFactory.getLogger(ModelSpecBean.class);

    @Bean
    public ModelSpec modelSpec(InitConfig initConfig) throws Exception {

        ModelSpec modelSpec = new ModelSpec();
        
        if(initConfig.getModelDslPath() == null){

            return parseResourceDsl(modelSpec);
        }

        else{

            Path modelsDirectory = Paths.get(initConfig.getModelDslPath());
            List<File> modelSpecFileList = new ArrayList<>();

             // Ensure the path actually exists and is a directory before traversing
            if (!Files.isDirectory(modelsDirectory)) {
                throw new IllegalArgumentException("Provided path is not a valid directory: " + modelsDirectory);
            }

            // Try-with-resources is critical here to ensure the underlying OS file handles are closed
            try (Stream<Path> stream = Files.walk(modelsDirectory)) {
            
                stream
                    .filter(Files::isRegularFile)          
                    .map(Path::toFile)                     
                    .forEach( file -> modelSpecFileList.add(file));   
                    
                return parseDsl(modelSpec, modelSpecFileList);

            } catch (IOException e) {
                // Handle file system access issues (e.g., permission denied)
                System.err.println("Error traversing directory: " + e.getMessage());
            }
        }
        return modelSpec;
    }


    public ModelSpec parseDsl(ModelSpec modelSpec, List<File> fileList) throws Exception{

        
        ModelSpecHandler modelSpecHandler = new ModelSpecHandler(modelSpec);

        Binding binding = new Binding();
        GroovyShell groovyShell = new GroovyShell(binding);

        Closure<Void> modelsClosure = new Closure<Void>(null) {
            public void doCall(Closure closure) {
                modelSpecHandler.models(closure);
            }
        };
        binding.setVariable("models", modelsClosure);
        for(File file : fileList){

            groovyShell.evaluate(file);
        }

        return modelSpec;
    }

    public ModelSpec parseResourceDsl(ModelSpec modelSpec) throws Exception{

        String MODELS_DSL_RESOURCE = "models.groovy";
        ModelSpecHandler modelSpecHandler = new ModelSpecHandler(modelSpec);

        Binding binding = new Binding();
        GroovyShell groovyShell = new GroovyShell(binding);

        Closure<Void> modelsClosure = new Closure<Void>(null) {
            public void doCall(Closure closure) {
                modelSpecHandler.models(closure);
            }
        };
        binding.setVariable("models", modelsClosure);

        
        ClassPathResource resource = new ClassPathResource(MODELS_DSL_RESOURCE);

        if(resource.exists()){
            try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                groovyShell.evaluate(reader);
            }
        }
    
        return modelSpec;
    }
}
