package com.freshworks.hagrid.main.dsl.services;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;

class RequestServiceTest {

    @Test
    void withoutExecutorUsesRequestExecuteRequest() throws Exception {
        ActionRequest request = mock(ActionRequest.class);
        ActionResponse response = new ActionResponse();
        when(request.executeRequest()).thenReturn(response);

        assertSame(response, new RequestService().execute(request));
    }

    @Test
    void withExecutorDelegates() throws Exception {
        ActionRequest request = mock(ActionRequest.class);
        RequestExecutor executor = mock(RequestExecutor.class);
        ActionResponse response = new ActionResponse();
        when(executor.execute(request)).thenReturn(response);

        RequestService service = new RequestService();
        service.requestExecutor = executor;

        assertSame(response, service.execute(request));
        verify(executor).execute(request);
    }
}
