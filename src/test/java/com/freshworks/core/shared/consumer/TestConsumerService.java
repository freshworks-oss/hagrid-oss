package com.freshworks.core.shared.consumer;

import static org.dizitart.no2.filters.FluentFilter.where;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dizitart.no2.Nitrite;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.data.unit.fb.assets.FbComment;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraDbCursor;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.infra.MockFacadeInfraConfigService;
import com.freshworks.core.shared.infra.nitrite.MockFacadeNitriteDbService;
import com.freshworks.core.shared.infra.nitrite.MockFacadeNitritedbList;
import com.freshworks.core.shared.infra.nitrite.NitriteDbList;
import com.freshworks.core.shared.sync.MockFacadeSyncStatusService;
import com.freshworks.core.shared.sync.SyncStatusService;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
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

        ObjectMapper objectMapper = new ObjectMapper();
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
        doCallRealMethod().when(publisherList).filter(any(), any());
        publisherList.configure(syncServiceContainer);

        InfraService nitriteDbService = mockFacadeNitriteDbService
                .getPublisherList(publisherList)
                .getNamespace("abs")
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

        publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment1));
        publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment2));
        publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment3));



        ConsumerService consumerService = mockFacadeConsumerService
                .build();
        doCallRealMethod().when(consumerService).configure(any());
        doCallRealMethod().when(consumerService).getAssetCursor(any());
        doCallRealMethod().when(consumerService).getAssetCursor(any(), any());

        // Here consume the assets
        consumerService.configure(syncServiceContainer);
        InfraDbCursor<FbComment> dbCursor = consumerService.getAssetCursor(FbComment.class);

        assertThat(dbCursor.docSize(), Matchers.is(3L));

        List<FbComment> fbCommentList = new ArrayList<>();

        while(dbCursor.hasNext()){

                FbComment fbComment = dbCursor.getNext();
                fbCommentList.add(fbComment);
        }

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
        doCallRealMethod().when(publisherList).filter(any(), any());
        publisherList.configure(syncServiceContainer);


        NamespaceService namespaceService  = applicationContext.getBean(NamespaceService.class);

        InfraService h2DbService = mockFacadeNitriteDbService
                .getPublisherList(publisherList)
                .getNamespace("abc")
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

        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment2));

        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment3));


        ConsumerService consumerService = mockFacadeConsumerService
                .build();
        doCallRealMethod().when(consumerService).configure(any());
        doCallRealMethod().when(consumerService).getAssetCursor(any());
        doCallRealMethod().when(consumerService).getAssetCursor(any(), any());


        // Here consume the assets
        consumerService.configure(syncServiceContainer);
        InfraDbCursor<FbComment> dbCursor = consumerService.getAssetCursor(FbComment.class, where("value.comment_id").eq("2"));
        assertThat(dbCursor.docSize(), Matchers.is(1L));

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
        doCallRealMethod().when(publisherList).filter(any(), any());
        publisherList.configure(syncServiceContainer);

        NamespaceService namespaceService  = applicationContext.getBean(NamespaceService.class);

        InfraService nitriteDbService = mockFacadeNitriteDbService
                .getPublisherList(publisherList)
                .getNamespace("df")
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

        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment2));

        index = publisherList.addAndGetIndex(objectMapper.writeValueAsString(fbComment3));


        ConsumerService consumerService = mockFacadeConsumerService
                .build();
        doCallRealMethod().when(consumerService).configure(any());
        doCallRealMethod().when(consumerService).getAssetCursor(any());
        doCallRealMethod().when(consumerService).getAssetCursor(any(), any());

        // Here consume the assets
        consumerService.configure(syncServiceContainer);
        
        // Here consume the assets
        consumerService.configure(syncServiceContainer);
        InfraDbCursor<FbComment> dbCursor = consumerService.getAssetCursor(FbComment.class, where("value.comment_title").eq("This is comment title"));
        assertThat(dbCursor.docSize(), Matchers.is(2L));
    }

    /**
     * This method, can only be tested when Hagrid is running. I will test it in integration test
     * @throws Exception
     */
    @Test
    public void testConsumerStream() throws Exception{

       
    }
}
