package com.freshworks.core.server;


import io.micrometer.observation.annotation.Observed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AzureAdController {


//    @Observed(name = "GetName",
//            contextualName = "get-name",
//            lowCardinalityKeyValues = {"sync-type", "orchestration"})
    @GetMapping("/get-name")
    public String getStringName(){

        return getString();
    }


    public String getString(){

        return "amit";
    }
}
