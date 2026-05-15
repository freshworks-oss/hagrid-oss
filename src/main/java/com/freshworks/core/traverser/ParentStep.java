package com.freshworks.core.traverser;

import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.RequestResponse;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

public class ParentStep extends AbstractStep {

    @Override
    public void closeSync() {

    }
}
