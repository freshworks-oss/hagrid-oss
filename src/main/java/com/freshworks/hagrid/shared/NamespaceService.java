package com.freshworks.hagrid.shared;

import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Data
public class NamespaceService {

    String namespace;
}
