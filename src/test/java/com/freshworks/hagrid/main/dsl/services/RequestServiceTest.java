package com.freshworks.hagrid.main.dsl.services;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;

class RequestServiceTest {

    @Test
    void withoutExecutorUsesRequestExecuteRequest() throws Exception {
        ActionRequest request = mock(ActionRequest.class);
        ActionResponse response = new ActionResponse();
        when(request.executeRequest()).thenReturn(response);

        assertSame(response, new RequestService().execute(request, Map.of()));
    }

    @Test
    void withExecutorDelegatesAndPassesInput() throws Exception {
        ActionRequest request = mock(ActionRequest.class);
        RequestExecutor executor = mock(RequestExecutor.class);
        ActionResponse response = new ActionResponse();
        Map<String, Object> input = Map.of("k", "v");
        when(executor.execute(request, input)).thenReturn(response);

        RequestService service = new RequestService();
        service.requestExecutor = executor;

        assertSame(response, service.execute(request, input));
        verify(executor).execute(request, input);
    }
}
