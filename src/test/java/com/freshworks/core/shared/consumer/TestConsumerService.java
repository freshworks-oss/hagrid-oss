package com.freshworks.core.shared.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.freshworks.core.data.five_zero_zero.unit.fb.assets.FbComment;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.FreshIndexBeanSerializeModifier;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.infra.MockFacadeInfraConfigService;
import com.freshworks.core.shared.infra.nitrite.MockFacadeNitriteDbService;
import com.freshworks.core.shared.infra.nitrite.MockFacadeNitritedbList;
import com.freshworks.core.shared.infra.nitrite.NitriteDbList;
import com.freshworks.core.shared.sync.MockFacadeSyncStatusService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.freshindex.NamespaceService;
import com.freshworks.freshindex.index.JsonIndexService;
import com.freshworks.freshindex.index.query.Expression;
import com.freshworks.freshindex.index.query.JsonQueryService;
import com.zaxxer.hikari.HikariConfig;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.rocksdb.RocksDBModule;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doCallRealMethod;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.[a-z]*")
public class TestConsumerService {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MockFacadeConsumerService mockFacadeConsumerService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeInfraConfigService mockFacadeInfraConfigService;

    @Autowired
    MockFacadeNitriteDbService mockFacadeNitriteDbService;

    @Autowired
    MockFacadeSyncStatusService mockFacadeSyncStatusService;

    @Autowired
    MockFacadeNitritedbList mockFacadeNitritedbList;

    ObjectMapper objectMapper = new ObjectMapper();
    String releaseVersion;

    static Nitrite nitriteDb;

    @BeforeAll
    public static void beforeAll(){

       nitriteDb = Nitrite.builder()
            .openOrCreate();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        releaseVersion = System.getProperty("spring.profiles.active").split("\\.")[0];
        mockFacadeConsumerService.configure().build();
        mockFacadeNitriteDbService.configure().build();
        mockFacadeNitritedbList.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
        mockFacadeInfraConfigService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
        mockFacadeSyncStatusService.configure().build();
    }


    @Test
    public void testWhenAssetsOfTypeArePresentInInfraThenGetAssetTypeReturnsAllAssetAtOnce() throws Exception{

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .build();
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("consumer_" + UUID.randomUUID().toString());
        syncServiceContainer.add(namespace);

        SyncStatusService syncStatusService = mockFacadeSyncStatusService
                .build();
        syncServiceContainer.add(syncStatusService);



        InfraConfigService infraConfigService = mockFacadeInfraConfigService
                .getInfraType("nitrite")
                .getNitriteDatabaseType("memory")
                .build();
        syncServiceContainer.add(infraConfigService);



        NitriteDbList publisherList = mockFacadeNitritedbList
                .addNitriteDataSource(nitriteDb)
                .listName("publisher_list")
                .namespace(namespace.getNamespace())
                .build();

        System.out.print("Inserting document in list " + publisherList.getListName() );

        doCallRealMethod().when(publisherList).configure(any());
        doCallRealMethod().when(publisherList).add(anyString());
        doCallRealMethod().when(publisherList).addAndGetIndex(anyString());
        doCallRealMethod().when(publisherList).get(anyList());
        doCallRealMethod().when(publisherList).get(anyList());
        doCallRealMethod().when(publisherList).add(anyList());
        publisherList.configure(syncServiceContainer);


        JsonIndexService jsonIndexService = applicationContext.getBean(JsonIndexService.class);
        jsonIndexService.configure(namespace.getNamespace());

        JsonQueryService jsonQueryService = applicationContext.getBean(JsonQueryService.class);
        jsonQueryService.configure(namespace.getNamespace());

        NamespaceService namespaceService  = applicationContext.getBean(NamespaceService.class);

        InfraService nitriteDbService = mockFacadeNitriteDbService
                .getPublisherList(publisherList)
                .getJsonIndexService(jsonIndexService)
                .getJsonQueryService(jsonQueryService)
                .getNamespaceService(namespaceService)
                .build();

        syncServiceContainer.add(nitriteDbService, InfraService.class);


        // Here insert data into the infra layer so that it can be consumed by consumer
        FbComment fbComment1 = new FbComment();
        fbComment1.setComment_id("1");
        fbComment1.setComment_title("This is comment title one");

        FbComment fbComment2 = new FbComment();
        fbComment2.setComment_id("2");
        fbComment2.setComment_title("This is comment title two");

        FbComment fbComment3 = new FbComment();
        fbComment3.setComment_id("3");
        fbComment3.setComment_title("This is comment title three");

        Long index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment1));
        String s = getFreshIndexObjectMapper().writeValueAsString(fbComment1);
        JsonNode j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());


        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment2));
        s = getFreshIndexObjectMapper().writeValueAsString(fbComment2);
        j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());

        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment3));
        s = getFreshIndexObjectMapper().writeValueAsString(fbComment3);
        j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());


        ConsumerService consumerService = mockFacadeConsumerService
                .build();
        doCallRealMethod().when(consumerService).configure(any());
        doCallRealMethod().when(consumerService).getAssetByAssetType(any());

        // Here consume the assets
        consumerService.configure(syncServiceContainer);
        List<FbComment> fbCommentList = consumerService.getAssetByAssetType(FbComment.class);

        assertThat(fbCommentList.size(), Matchers.is(3));
    }


    @Test
    public void testWhenAssetsOfTypeArePresentInInfraThenGetAssetTypeWithFilterConditionReturnsOnlyFilteredAssetAtOnce() throws Exception{

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .build();
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("consumer_" + UUID.randomUUID().toString());
        syncServiceContainer.add(namespace);

        SyncStatusService syncStatusService = mockFacadeSyncStatusService
                .build();
        syncServiceContainer.add(syncStatusService);



        InfraConfigService infraConfigService = mockFacadeInfraConfigService
                .getInfraType("nitrite")
                .getNitriteDatabaseType("memory")
                .build();
        syncServiceContainer.add(infraConfigService);



        NitriteDbList publisherList = mockFacadeNitritedbList
                .addNitriteDataSource(nitriteDb)
                .listName("publisher_list")
                .namespace(namespace.getNamespace())
                .build();

        System.out.println("Inserting document in list " + publisherList.getListName() );

        doCallRealMethod().when(publisherList).configure(any());
        doCallRealMethod().when(publisherList).add(anyString());
        doCallRealMethod().when(publisherList).addAndGetIndex(anyString());
        doCallRealMethod().when(publisherList).get(anyList());
        doCallRealMethod().when(publisherList).get(anyList());
        doCallRealMethod().when(publisherList).add(anyList());
        publisherList.configure(syncServiceContainer);


        JsonIndexService jsonIndexService = applicationContext.getBean(JsonIndexService.class);
        jsonIndexService.configure(namespace.getNamespace());

        JsonQueryService jsonQueryService = applicationContext.getBean(JsonQueryService.class);
        jsonQueryService.configure(namespace.getNamespace());

        NamespaceService namespaceService  = applicationContext.getBean(NamespaceService.class);

        InfraService h2DbService = mockFacadeNitriteDbService
                .getPublisherList(publisherList)
                .getJsonIndexService(jsonIndexService)
                .getJsonQueryService(jsonQueryService)
                .getNamespaceService(namespaceService)
                .build();

        syncServiceContainer.add(h2DbService, InfraService.class);


        // Here insert data into the infra layer so that it can be consumed by consumer
        FbComment fbComment1 = new FbComment();
        fbComment1.setComment_id("1");
        fbComment1.setComment_title("This is comment title one");

        FbComment fbComment2 = new FbComment();
        fbComment2.setComment_id("2");
        fbComment2.setComment_title("This is comment title two");

        FbComment fbComment3 = new FbComment();
        fbComment3.setComment_id("3");
        fbComment3.setComment_title("This is comment title three");

        Long index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment1));
        String s = getFreshIndexObjectMapper().writeValueAsString(fbComment1);
        JsonNode j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());


        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment2));
        s = getFreshIndexObjectMapper().writeValueAsString(fbComment2);
        j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());

        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment3));
        s = getFreshIndexObjectMapper().writeValueAsString(fbComment3);
        j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());


        ConsumerService consumerService = mockFacadeConsumerService
                .build();
        doCallRealMethod().when(consumerService).configure(any());
        doCallRealMethod().when(consumerService).getAssetByAssetTypeAndFilter(any(), any());

        Expression expression = Expression.expressionBuilder()
                        .whenAssetFieldName("$.FbComment.comment_id")
                                .is()
                                        .whenAssetFieldValue("1")
                                                .build();

        // Here consume the assets
        consumerService.configure(syncServiceContainer);
        List<FbComment> fbCommentList = consumerService.getAssetByAssetTypeAndFilter(FbComment.class, expression);

        assertThat(fbCommentList.size(), Matchers.is(1));
    }


    @Test
    public void testWhenAssetsOfMeetingSameFreshIndexKeyArePresentInInfraThenGetAssetTypeWithFilterConditionReturnsOnlyFilteredAssetAtOnce() throws Exception{

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .build();
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("consumer_" + UUID.randomUUID().toString());
        syncServiceContainer.add(namespace);

        SyncStatusService syncStatusService = mockFacadeSyncStatusService
                .build();
        syncServiceContainer.add(syncStatusService);



        InfraConfigService infraConfigService = mockFacadeInfraConfigService
                .getInfraType("nitrite")
                .getNitriteDatabaseType("memory")
                .build();
        syncServiceContainer.add(infraConfigService);



        NitriteDbList publisherList = mockFacadeNitritedbList
                .addNitriteDataSource(nitriteDb)
                .listName("publisher_list")
                .namespace(namespace.getNamespace())
                .build();

        System.out.println("publisher list name is " + publisherList.getListName() );
        System.out.println("documents in publisher list are" + publisherList.getNitriteCollection().find().size());
        System.out.println("publisher list nitrite collection name is " + publisherList.getNitriteCollection().getName());

        doCallRealMethod().when(publisherList).configure(any());
        doCallRealMethod().when(publisherList).add(anyString());
        doCallRealMethod().when(publisherList).addAndGetIndex(anyString());
        doCallRealMethod().when(publisherList).get(anyList());
        doCallRealMethod().when(publisherList).get(anyList());
        doCallRealMethod().when(publisherList).add(anyList());
        publisherList.configure(syncServiceContainer);


        JsonIndexService jsonIndexService = applicationContext.getBean(JsonIndexService.class);
        jsonIndexService.configure(namespace.getNamespace());

        JsonQueryService jsonQueryService = applicationContext.getBean(JsonQueryService.class);
        jsonQueryService.configure(namespace.getNamespace());

        NamespaceService namespaceService  = applicationContext.getBean(NamespaceService.class);

        InfraService nitriteDbService = mockFacadeNitriteDbService
                .getPublisherList(publisherList)
                .getJsonIndexService(jsonIndexService)
                .getJsonQueryService(jsonQueryService)
                .getNamespaceService(namespaceService)
                .build();

        syncServiceContainer.add(nitriteDbService, InfraService.class);


        // Here insert data into the infra layer so that it can be consumed by consumer
        FbComment fbComment1 = new FbComment();
        fbComment1.setComment_id("1");
        fbComment1.setComment_title("This is comment title");

        FbComment fbComment2 = new FbComment();
        fbComment2.setComment_id("2");
        fbComment2.setComment_title("This is comment title");

        FbComment fbComment3 = new FbComment();
        fbComment3.setComment_id("3");
        fbComment3.setComment_title("This is comment title three");

        Long index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment1));
        String s = getFreshIndexObjectMapper().writeValueAsString(fbComment1);
        JsonNode j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());


        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment2));
        s = getFreshIndexObjectMapper().writeValueAsString(fbComment2);
        j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());

        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment3));
        s = getFreshIndexObjectMapper().writeValueAsString(fbComment3);
        j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());


        ConsumerService consumerService = mockFacadeConsumerService
                .build();
        doCallRealMethod().when(consumerService).configure(any());
        doCallRealMethod().when(consumerService).getAssetByAssetTypeAndFilter(any(), any());

        Expression expression = Expression.expressionBuilder()
                .whenAssetFieldName("$.FbComment.comment_title")
                .is()
                .whenAssetFieldValue("This is comment title")
                .build();

        // Here consume the assets
        consumerService.configure(syncServiceContainer);
        List<FbComment> fbCommentList = consumerService.getAssetByAssetTypeAndFilter(FbComment.class, expression);

        assertThat(fbCommentList.size(), Matchers.is(2));
    }

    @Test
    public void testWhenAssetsOfTypeArePresentInInfraAndSyncIsCompletedThenStreamAssetReturnsOnlyLimitedAssets() throws Exception{

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .build();
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("consumer_" + UUID.randomUUID().toString());
        syncServiceContainer.add(namespace);

        SyncStatusService syncStatusService = mockFacadeSyncStatusService
                .getSyncStatus(1)
                .build();
        syncServiceContainer.add(syncStatusService);



        InfraConfigService infraConfigService = mockFacadeInfraConfigService
                .getInfraType("nitrite")
                .getNitriteDatabaseType("memory")
                .build();
        syncServiceContainer.add(infraConfigService);



        NitriteDbList publisherList = mockFacadeNitritedbList
                .addNitriteDataSource(nitriteDb)
                .listName("publisher_list")
                .namespace(namespace.getNamespace())
                .build();

        System.out.println("publisher list name is " + publisherList.getListName() );
        System.out.println("documents in publisher list are" + publisherList.getNitriteCollection().find().size());
        System.out.println("publisher list nitrite collection name is " + publisherList.getNitriteCollection().getName());
        

        doCallRealMethod().when(publisherList).configure(any());
        doCallRealMethod().when(publisherList).add(anyString());
        doCallRealMethod().when(publisherList).addAndGetIndex(anyString());
        doCallRealMethod().when(publisherList).get(anyList());
        doCallRealMethod().when(publisherList).get(anyList());
        doCallRealMethod().when(publisherList).add(anyList());
        publisherList.configure(syncServiceContainer);


        JsonIndexService jsonIndexService = applicationContext.getBean(JsonIndexService.class);
        jsonIndexService.configure(namespace.getNamespace());

        JsonQueryService jsonQueryService = applicationContext.getBean(JsonQueryService.class);
        jsonQueryService.configure(namespace.getNamespace());

        NamespaceService namespaceService  = applicationContext.getBean(NamespaceService.class);

        InfraService nitriteDbService = mockFacadeNitriteDbService
                .getPublisherList(publisherList)
                .getJsonIndexService(jsonIndexService)
                .getJsonQueryService(jsonQueryService)
                .getNamespaceService(namespaceService)
                .build();

        syncServiceContainer.add(nitriteDbService, InfraService.class);


        // Here insert data into the infra layer so that it can be consumed by consumer
        FbComment fbComment1 = new FbComment();
        fbComment1.setComment_id("1");
        fbComment1.setComment_title("This is comment title one");

        FbComment fbComment2 = new FbComment();
        fbComment2.setComment_id("2");
        fbComment2.setComment_title("This is comment title two");

        FbComment fbComment3 = new FbComment();
        fbComment3.setComment_id("3");
        fbComment3.setComment_title("This is comment title three");

        Long index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment1));
        String s = getFreshIndexObjectMapper().writeValueAsString(fbComment1);
        JsonNode j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());


        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment2));
        s = getFreshIndexObjectMapper().writeValueAsString(fbComment2);
        j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());

        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment3));
        s = getFreshIndexObjectMapper().writeValueAsString(fbComment3);
        j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());


        ConsumerService consumerService = mockFacadeConsumerService
                .build();
        doCallRealMethod().when(consumerService).configure(any());
        doCallRealMethod().when(consumerService).streamAssetByAssetType(any(), any());

        // Here consume the assets
        consumerService.configure(syncServiceContainer);
        AssetStreamResponse.Token token = new AssetStreamResponse.Token();
        token.setStart(0);
        token.setCount(1);
        AssetStreamResponse<FbComment> streamResponse = consumerService.streamAssetByAssetType(FbComment.class, token);
        List<FbComment> fbCommentList = streamResponse.getAbstractAssetList();
        token = streamResponse.getNextToken();
        assertThat(fbCommentList.size(), Matchers.is(1));
        assertThat(fbCommentList.get(0).getComment_id(), Matchers.is("1"));
        assertThat(token.getCount(), Matchers.is(1));
        assertThat(token.getStart(), Matchers.is(1));

        streamResponse = consumerService.streamAssetByAssetType(FbComment.class, token);
        fbCommentList = streamResponse.getAbstractAssetList();
        token = streamResponse.getNextToken();
        assertThat(fbCommentList.size(), Matchers.is(1));
        assertThat(fbCommentList.get(0).getComment_id(), Matchers.is("2"));
        assertThat(token.getCount(), Matchers.is(1));
        assertThat(token.getStart(), Matchers.is(2));

        streamResponse = consumerService.streamAssetByAssetType(FbComment.class, token);
        fbCommentList = streamResponse.getAbstractAssetList();
        token = streamResponse.getNextToken();
        assertThat(fbCommentList.size(), Matchers.is(1));
        assertThat(fbCommentList.get(0).getComment_id(), Matchers.is("3"));
        assertThat(token.getCount(), Matchers.is(1));
        assertThat(token.getStart(), Matchers.is(3));

        streamResponse = consumerService.streamAssetByAssetType(FbComment.class, token);
        fbCommentList = streamResponse.getAbstractAssetList();
        token = streamResponse.getNextToken();
        assertThat(fbCommentList.size(), Matchers.is(0));
        assertThat(token, Matchers.is(nullValue()));
    }

    @Test
    public void testWhenAssetsOfTypeArePresentInInfraAndSyncIsFailedThenStreamAssetReturnsOnlyLimitedAssets() throws Exception{

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .build();
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("consumer_" + UUID.randomUUID().toString());
        syncServiceContainer.add(namespace);

        SyncStatusService syncStatusService = mockFacadeSyncStatusService
                .getSyncStatus(-1)
                .build();
        syncServiceContainer.add(syncStatusService);



        InfraConfigService infraConfigService = mockFacadeInfraConfigService
                .getInfraType("nitrite")
                .getNitriteDatabaseType("memory")
                .build();
        syncServiceContainer.add(infraConfigService);



        NitriteDbList publisherList = mockFacadeNitritedbList
                .addNitriteDataSource(nitriteDb)
                .listName("publisher_list")
                .namespace(namespace.getNamespace())
                .build();

        System.out.println("Inserting document in list " + publisherList.getListName() );

        doCallRealMethod().when(publisherList).configure(any());
        doCallRealMethod().when(publisherList).add(anyString());
        doCallRealMethod().when(publisherList).addAndGetIndex(anyString());
        doCallRealMethod().when(publisherList).get(anyList());
        doCallRealMethod().when(publisherList).get(anyList());
        doCallRealMethod().when(publisherList).add(anyList());
        publisherList.configure(syncServiceContainer);


        JsonIndexService jsonIndexService = applicationContext.getBean(JsonIndexService.class);
        jsonIndexService.configure(namespace.getNamespace());

        JsonQueryService jsonQueryService = applicationContext.getBean(JsonQueryService.class);
        jsonQueryService.configure(namespace.getNamespace());

        NamespaceService namespaceService  = applicationContext.getBean(NamespaceService.class);

        InfraService nitriteDbService = mockFacadeNitriteDbService
                .getPublisherList(publisherList)
                .getJsonIndexService(jsonIndexService)
                .getJsonQueryService(jsonQueryService)
                .getNamespaceService(namespaceService)
                .build();

        syncServiceContainer.add(nitriteDbService, InfraService.class);


        // Here insert data into the infra layer so that it can be consumed by consumer
        FbComment fbComment1 = new FbComment();
        fbComment1.setComment_id("1");
        fbComment1.setComment_title("This is comment title one");

        FbComment fbComment2 = new FbComment();
        fbComment2.setComment_id("2");
        fbComment2.setComment_title("This is comment title two");

        FbComment fbComment3 = new FbComment();
        fbComment3.setComment_id("3");
        fbComment3.setComment_title("This is comment title three");

        Long index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment1));
        String s = getFreshIndexObjectMapper().writeValueAsString(fbComment1);
        JsonNode j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());


        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment2));
        s = getFreshIndexObjectMapper().writeValueAsString(fbComment2);
        j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());

        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment3));
        s = getFreshIndexObjectMapper().writeValueAsString(fbComment3);
        j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());


        ConsumerService consumerService = mockFacadeConsumerService
                .build();
        doCallRealMethod().when(consumerService).configure(any());
        doCallRealMethod().when(consumerService).streamAssetByAssetType(any(), any());

        // Here consume the assets
        consumerService.configure(syncServiceContainer);
        AssetStreamResponse.Token token = new AssetStreamResponse.Token();
        token.setStart(0);
        token.setCount(1);
        AssetStreamResponse<FbComment> streamResponse = consumerService.streamAssetByAssetType(FbComment.class, token);
        List<FbComment> fbCommentList = streamResponse.getAbstractAssetList();
        token = streamResponse.getNextToken();
        assertThat(fbCommentList.size(), Matchers.is(1));
        assertThat(fbCommentList.get(0).getComment_id(), Matchers.is("1"));
        assertThat(token.getCount(), Matchers.is(1));
        assertThat(token.getStart(), Matchers.is(1));

        streamResponse = consumerService.streamAssetByAssetType(FbComment.class, token);
        fbCommentList = streamResponse.getAbstractAssetList();
        token = streamResponse.getNextToken();
        assertThat(fbCommentList.size(), Matchers.is(1));
        assertThat(fbCommentList.get(0).getComment_id(), Matchers.is("2"));
        assertThat(token.getCount(), Matchers.is(1));
        assertThat(token.getStart(), Matchers.is(2));

        streamResponse = consumerService.streamAssetByAssetType(FbComment.class, token);
        fbCommentList = streamResponse.getAbstractAssetList();
        token = streamResponse.getNextToken();
        assertThat(fbCommentList.size(), Matchers.is(1));
        assertThat(fbCommentList.get(0).getComment_id(), Matchers.is("3"));
        assertThat(token.getCount(), Matchers.is(1));
        assertThat(token.getStart(), Matchers.is(3));

        streamResponse = consumerService.streamAssetByAssetType(FbComment.class, token);
        fbCommentList = streamResponse.getAbstractAssetList();
        token = streamResponse.getNextToken();
        assertThat(fbCommentList.size(), Matchers.is(0));
        assertThat(token, Matchers.is(nullValue()));
    }

    @Test
    public void testWhenAssetsOfTypeArePresentInInfraAndSyncIsInProgressThenStreamAssetReturnsOnlyLimitedAssets() throws Exception{

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .build();
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("consumer_" + UUID.randomUUID().toString());
        syncServiceContainer.add(namespace);

        SyncStatusService syncStatusService = mockFacadeSyncStatusService
                .getSyncStatus(0)
                .build();
        syncServiceContainer.add(syncStatusService);



        InfraConfigService infraConfigService = mockFacadeInfraConfigService
                .getInfraType("nitrite")
                .getNitriteDatabaseType("memory")
                .build();
        syncServiceContainer.add(infraConfigService);



        NitriteDbList publisherList = mockFacadeNitritedbList
                .addNitriteDataSource(nitriteDb)
                .listName("publisher_list")
                .namespace(namespace.getNamespace())
                .build();

        System.out.println("Inserting document in list " + publisherList.getListName() );

        doCallRealMethod().when(publisherList).configure(any());
        doCallRealMethod().when(publisherList).add(anyString());
        doCallRealMethod().when(publisherList).addAndGetIndex(anyString());
        doCallRealMethod().when(publisherList).get(anyList());
        doCallRealMethod().when(publisherList).get(anyList());
        doCallRealMethod().when(publisherList).add(anyList());
        publisherList.configure(syncServiceContainer);


        JsonIndexService jsonIndexService = applicationContext.getBean(JsonIndexService.class);
        jsonIndexService.configure(namespace.getNamespace());

        JsonQueryService jsonQueryService = applicationContext.getBean(JsonQueryService.class);
        jsonQueryService.configure(namespace.getNamespace());

        NamespaceService namespaceService  = applicationContext.getBean(NamespaceService.class);

        InfraService h2DbService = mockFacadeNitriteDbService
                .getPublisherList(publisherList)
                .getJsonIndexService(jsonIndexService)
                .getJsonQueryService(jsonQueryService)
                .getNamespaceService(namespaceService)
                .build();

        syncServiceContainer.add(h2DbService, InfraService.class);


        // Here insert data into the infra layer so that it can be consumed by consumer
        FbComment fbComment1 = new FbComment();
        fbComment1.setComment_id("1");
        fbComment1.setComment_title("This is comment title one");

        FbComment fbComment2 = new FbComment();
        fbComment2.setComment_id("2");
        fbComment2.setComment_title("This is comment title two");

        FbComment fbComment3 = new FbComment();
        fbComment3.setComment_id("3");
        fbComment3.setComment_title("This is comment title three");

        Long index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment1));
        String s = getFreshIndexObjectMapper().writeValueAsString(fbComment1);
        JsonNode j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());


        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment2));
        s = getFreshIndexObjectMapper().writeValueAsString(fbComment2);
        j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());

        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment3));
        s = getFreshIndexObjectMapper().writeValueAsString(fbComment3);
        j = getFreshIndexObjectMapper().readTree(s);
        jsonIndexService.indexJsonString(j, index.toString());


        ConsumerService consumerService = mockFacadeConsumerService
                .build();
        doCallRealMethod().when(consumerService).configure(any());
        doCallRealMethod().when(consumerService).streamAssetByAssetType(any(), any());

        // Here consume the assets
        consumerService.configure(syncServiceContainer);
        AssetStreamResponse.Token token = new AssetStreamResponse.Token();
        token.setStart(0);
        token.setCount(1);
        AssetStreamResponse<FbComment> streamResponse = consumerService.streamAssetByAssetType(FbComment.class, token);
        List<FbComment> fbCommentList = streamResponse.getAbstractAssetList();
        token = streamResponse.getNextToken();
        assertThat(fbCommentList.size(), Matchers.is(1));
        assertThat(fbCommentList.get(0).getComment_id(), Matchers.is("1"));
        assertThat(token.getCount(), Matchers.is(1));
        assertThat(token.getStart(), Matchers.is(1));

        streamResponse = consumerService.streamAssetByAssetType(FbComment.class, token);
        fbCommentList = streamResponse.getAbstractAssetList();
        token = streamResponse.getNextToken();
        assertThat(fbCommentList.size(), Matchers.is(1));
        assertThat(fbCommentList.get(0).getComment_id(), Matchers.is("2"));
        assertThat(token.getCount(), Matchers.is(1));
        assertThat(token.getStart(), Matchers.is(2));

        streamResponse = consumerService.streamAssetByAssetType(FbComment.class, token);
        fbCommentList = streamResponse.getAbstractAssetList();
        token = streamResponse.getNextToken();
        assertThat(fbCommentList.size(), Matchers.is(1));
        assertThat(fbCommentList.get(0).getComment_id(), Matchers.is("3"));
        assertThat(token.getCount(), Matchers.is(1));
        assertThat(token.getStart(), Matchers.is(3));

        streamResponse = consumerService.streamAssetByAssetType(FbComment.class, token);
        fbCommentList = streamResponse.getAbstractAssetList();
        token = streamResponse.getNextToken();
        assertThat(fbCommentList.size(), Matchers.is(0));
        assertThat(token.getCount(), Matchers.is(1));
        assertThat(token.getStart(), Matchers.is(3));
    }

    @Test
    public void testWhenAssetIsDeSerializedThenItDoesNotAddSyncServiceContainerService() throws JsonProcessingException {

        String s = "{\"clazz\":\"com.freshworks.core.data." + releaseVersion + ".unit.dag.assets.Usage\",\"uniqueIdentifier\":null,\"usage\":\"some usage\"}";
        AbstractAsset usageAsset = objectMapper.readValue(s, AbstractAsset.class);
        assertThat(usageAsset.getSyncServiceContainer(), nullValue());
    }


    private ObjectMapper getFreshIndexObjectMapper(){

        ObjectMapper freshIndexObjectMapper = new ObjectMapper();
        freshIndexObjectMapper.registerModule(new SimpleModule(){

            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addBeanSerializerModifier(new FreshIndexBeanSerializeModifier());
            }
        });

        freshIndexObjectMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);

        return freshIndexObjectMapper;
    }
}
