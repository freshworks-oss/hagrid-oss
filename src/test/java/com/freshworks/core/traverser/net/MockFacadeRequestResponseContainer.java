package com.freshworks.core.traverser.net;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.traverser.RequestResponseContainer;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import static org.mockito.Mockito.doReturn;

@Component
public class MockFacadeRequestResponseContainer implements MockFacadeInterface {


    ReturnableMockTypeList<Object> request;

    ReturnableMockTypeList<Object> response;


    public MockFacadeRequestResponseContainer configure() throws Exception{

        reset();

        request.add("select * from students");
        response.add("[{\n" +
                "      \"name\" :\"amit\",\n" +
                "      \"company\"  :\"freshworks\"\n" +
                "}\n" +
                "\n" +
                "{\n" +
                "      \"name\" :\"praveen\",\n" +
                "      \"company\"  :\"abc\"\n" +
                "}]");

        return this;
    }


    public MockFacadeRequestResponseContainer request(Object... request){
        this.request.clear();
        this.request.add(request);
        return this;
    }

    public MockFacadeRequestResponseContainer response(Object... response){

        this.response.clear();
        this.response.add(response);
        return this;
    }


    @Override
    public RequestResponseContainer build() throws Exception {

        RequestResponseContainer requestResponseContainer = new RequestResponseContainer();
        requestResponseContainer = Mockito.spy(requestResponseContainer);

        doReturn(this.request.answer()).when(requestResponseContainer).getRequest();
        doReturn(this.response.answer()).when(requestResponseContainer).getResponse();
        return requestResponseContainer;
    }
}
