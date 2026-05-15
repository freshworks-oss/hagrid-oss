package com.freshworks.core.shared.synchronizers;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Getter
public class GlobalNamespaceService {

    String globalNamespace;

    public GlobalNamespaceService(){

        LocalDateTime currentDateTime = LocalDateTime.now();

        // Define the desired format
        // Example: "yyyy-MM-dd HH:mm:ss" for "2025-08-05 13:22:00"
        // Example: "dd/MM/yyyy HH:mm:ss a" for "05/08/2025 01:22:00 PM"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Format the LocalDateTime object into a string
        globalNamespace =  "GLOBAL_NAMESPACE_AT_" + currentDateTime.format(formatter);
    }

}
