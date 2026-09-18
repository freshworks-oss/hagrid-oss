package com.freshworks.hagrid.traverser;

import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.hagrid.traverser.exception.StepFailedException;
import com.freshworks.hagrid.traverser.net.RequestResponse;
import com.freshworks.hagrid.traverser.net.http.HttpRequestResponse;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class ParentStep extends AbstractStep {

    @Override
    public void closeSync() {

    }
}
