package com.freshworks.core.traverser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.shared.Annotations.BetaRelease;
import com.freshworks.core.shared.Annotations.ReleaseCandidate;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;

@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractStep {

    private SyncServiceContainer syncServiceContainer;

    @ReleaseCandidate(sourceVersion = "3.0.0-beta", targetVersion = "3.1.0", useCase = "For flexibility where dev within steps want to say terminate whole sync, get the number of beans in this step using infra beans ")
    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer  = syncServiceContainer;
    }

    public SyncServiceContainer getSyncServiceContainer() {
        return syncServiceContainer;
    }


    public abstract void closeSync();

}
