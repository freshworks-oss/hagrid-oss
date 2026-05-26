package com.freshworks.core.traverser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.infra.InfraDbKeyValue;
import com.freshworks.core.shared.infra.InfraDbList;
import com.freshworks.core.shared.infra.persistent.MockFacadeMongodbKeyValue;
import com.freshworks.core.shared.infra.persistent.MockFacadeMongodbList;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

@Component
public class MockFacadeDagNode implements MockFacadeInterface {

    ObjectMapper objectMapper = new ObjectMapper();

    ReturnableMockTypeList<Boolean> isCloned = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<String> name = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<String> shortName = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Integer> nodeTraverserStatusCode = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<JsonNode> data = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<LinkedHashMap<DagNode, Relationship>> children = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<LinkedHashMap<DagNode, Relationship>> parentList = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<InfraDbList> infraDbList = new ReturnableMockTypeList<>();
    @Autowired
    MockFacadeMongodbList mockFacadeMongodbList;

    ReturnableMockTypeList<InfraDbKeyValue> infraDbKeyValue = new ReturnableMockTypeList<>();
    @Autowired
    MockFacadeMongodbKeyValue mockFacadeMongodbKeyValue;

    ReturnableMockTypeList<Boolean> hasMoreData;

    ReturnableMockTypeList<List<String>> getSyncResult;



    public MockFacadeDagNode configure() throws Exception {

        reset();

        isCloned.add(false);
        name.add(MockHttpAbstractStep.class.getName());
        shortName.add(MockHttpAbstractStep.class.getSimpleName());
        nodeTraverserStatusCode.add(-100);

        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("dummy-key", "dummy-value");
        data.add(objectNode);

        LinkedHashMap<DagNode, Relationship> c = new LinkedHashMap<>();
        children.add(c);
        DagNode parentNodeValue = null;

        parentList.add(c);
        infraDbList.add(mockFacadeMongodbList.configure().build());
        infraDbKeyValue.add(mockFacadeMongodbKeyValue.configure().build());
        hasMoreData.add(false);
        List<String> s = new ArrayList<>();
        s.add("{\"name\":\"amit\"}");
        s.add("{\"name\":\"rahul\"}");
        getSyncResult.add(s);

        return this;
    }


    public MockFacadeDagNode isCloned(Boolean... isCloned) {
        this.isCloned.clear();
        this.isCloned.add(isCloned);
        return this;
    }

    public MockFacadeDagNode name(Class<? extends AbstractStep>... stepClassName) {
        this.name.clear();
        for (Class<? extends AbstractStep> stepClass : stepClassName) {
            this.name.add(stepClass.getName());
        }
        return this;
    }

    public MockFacadeDagNode shortName(String shortNameData) {
        this.shortName.clear();
        this.shortName.add(shortNameData);
        return this;
    }

    public MockFacadeDagNode nodeTraverserStatuCode(Integer... nodeTraverserStatuCode) {
        this.nodeTraverserStatusCode.clear();
        this.nodeTraverserStatusCode.add(nodeTraverserStatuCode);
        return this;
    }

    public MockFacadeDagNode data(JsonNode data) {
        this.data.clear();
        this.data.add(data);
        return this;
    }

    public MockFacadeDagNode children(LinkedHashMap<DagNode, Relationship>... children) {
        this.children.clear();
        this.children.add(children);
        return this;
    }

    public MockFacadeDagNode parentList(LinkedHashMap<DagNode, Relationship>... parentList) {
        this.parentList.clear();
        this.parentList.add(parentList);
        return this;
    }

    public MockFacadeDagNode infraDbList(InfraDbList... infraDbList) {
        this.infraDbList.clear();
        this.infraDbList.add(infraDbList);
        return this;
    }

    public MockFacadeDagNode infraDbKeyValue(InfraDbKeyValue... infraDbKeyValue) {
        this.infraDbKeyValue.clear();
        this.infraDbKeyValue.add(infraDbKeyValue);
        return this;
    }

    public MockFacadeDagNode hasMoreData(Boolean... hasMoreData) {
        this.hasMoreData.clear();
        this.hasMoreData.add(hasMoreData);
        return this;
    }


    public MockFacadeDagNode getSyncResult(List<String>... syncResult) {
        this.getSyncResult.clear();
        this.getSyncResult.add(syncResult);
        return this;
    }

    public DagNode build() throws Exception {

        DagNode dagNode = new DagNode(name.next());
        dagNode = Mockito.spy(dagNode);
        dagNode.setIsCloned(isCloned.next());
        dagNode.setName(name.next());
        dagNode.setShortName(shortName.next());
        dagNode.setData(data.next());
        dagNode.setChildrenRelationshipMap(children.next());
        dagNode.setParentRelationshipMap(parentList.next());
        dagNode.setInfraDbList(infraDbList.next());
        dagNode.setInfraDbKeyValue(infraDbKeyValue.next());

        doAnswer(hasMoreData.answer()).when(dagNode).waitUntilHasMoreData(anyInt(), any());
        return dagNode;
    }
}
