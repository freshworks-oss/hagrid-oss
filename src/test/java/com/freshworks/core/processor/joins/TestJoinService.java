package com.freshworks.core.processor.joins;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.processor.MockFacadeAssetAssetDependencyService;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraDbKeyValue;
import com.freshworks.core.shared.infra.MockFacadeInfraConfigService;
import com.freshworks.core.shared.infra.inmemory.InmemoryService;
import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.freshworks.core.TestUtility;
import com.freshworks.core.data.four_five_zero.unit.processor.joins.assets.FbUsageAsset;
import com.freshworks.core.data.four_five_zero.unit.processor.joins.assets.FbUserAsset;
import com.freshworks.core.data.four_five_zero.unit.processor.joins.assets.non_primitive_assets.FbUserUsageAsset;
import com.freshworks.core.data.four_five_zero.unit.processor.joins.assets.non_primitive_assets.FbUserUsageAssetInnerJoin;
import com.freshworks.core.data.four_five_zero.unit.processor.joins.beans.FbUsageBean;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestJoinService {
    
    @Autowired
    LeftJoinService leftJoinService;

    @Autowired
    InnerJoinService innerJoinService;
    
    @Autowired
    MockFacadeInfraConfigService mockFacadeInfraConfigService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeAssetAssetDependencyService mockAssetAssetDependencyService;

    @Autowired
    AnalyticsFactory analyticsFactory;

    Class<? extends AbstractAsset> fbUserUsageAsset;
    Class<? extends AbstractAsset> fbUserUsageAssetInnerJoin;
    Class<? extends AbstractAsset> fbUserAsset;
    Class<? extends AbstractAsset> fbUserAsset1;
    Class<? extends AbstractAsset> fbUsageAsset;
    Class<? extends AbstractAsset> fbUsageAsset1;


    @BeforeEach
    public void beforeEach() throws Exception {

        mockFacadeInfraConfigService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
        mockAssetAssetDependencyService.configure().build();
    }

    @Test
    public void testGetLookupFieldValueOfLeftClass() throws Exception{

        fbUserUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.non_primitive_assets.FbUserUsageAsset");
        fbUserAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUserAsset");
        fbUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUsageAsset");

        AbstractAsset fbUserAsset = this.fbUserAsset.getDeclaredConstructor().newInstance();
        TestUtility.callMethod(fbUserAsset, "setUserId", "aggarwal");
        TestUtility.callMethod(fbUserAsset, "setUserName", "amit");
        
        FreshJoin freshJoin = fbUserUsageAsset.getAnnotation(FreshJoin.class);
        String lookupFieldValueOfLeftClass = JoinUtility.getLookupFieldValueOfLeftClass(fbUserAsset, freshJoin);
        
        assertThat(lookupFieldValueOfLeftClass, Matchers.is("aggarwal"));
                
    }

    @Test
    public void testGetLookupFieldValueOfRightClass() throws Exception{

        fbUserUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.non_primitive_assets.FbUserUsageAsset");
        fbUserAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUserAsset");
        fbUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUsageAsset");

        AbstractAsset fbUsageAsset = this.fbUsageAsset.getDeclaredConstructor().newInstance();
        TestUtility.callMethod(fbUsageAsset, "setUserId", "aggarwal");
        TestUtility.callMethod(fbUsageAsset, "setCreatedAt", "2026-01-01");
        
        FreshJoin freshJoin = fbUserUsageAsset.getAnnotation(FreshJoin.class);
        String lookupFieldValueOfRightClass = JoinUtility.getLookupFieldValueOfRightClass(fbUsageAsset, freshJoin);
        
        assertThat(lookupFieldValueOfRightClass, Matchers.is("aggarwal"));

    }

    @Test
    public void testLookupStagingAreaWithSingleLeftAssetAndRightAsset() throws Exception{

        fbUserUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.non_primitive_assets.FbUserUsageAsset");
        fbUserAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUserAsset");
        fbUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUsageAsset");

        // Setting up data 
        ObjectMapper objectMapper = new ObjectMapper();
        FreshJoin freshJoin = fbUserUsageAsset.getAnnotation(FreshJoin.class);

        FbUserUsageAsset fbUserUsageAsset = (FbUserUsageAsset) this.fbUserUsageAsset.getDeclaredConstructor().newInstance();
        FbUserAsset fbUserAsset = (FbUserAsset) this.fbUserAsset.getDeclaredConstructor().newInstance();
        fbUserAsset.setUserId("aggarwal");
        fbUserAsset.setUserName("amit");


        FbUsageAsset fbUsageAsset = (FbUsageAsset) this.fbUsageAsset.getDeclaredConstructor().newInstance();
        fbUsageAsset.setUserId("aggarwal");
        fbUsageAsset.setCreatedAt("2026-01-01");
    

        // Setting up services 
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16),1000000);

        InmemoryService inMemoryService = new InmemoryService();   
        
        Namespace namespace = new Namespace();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, Namespace.class)
        .add(mockFacadeInfraConfigService, InfraConfigService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        leftJoinService.configure(syncServiceContainer, bloomFilter);

        inMemoryService.configure(syncServiceContainer, mockFacadeInfraConfigService.build());
        InfraDbKeyValue infraDbKeyValue = inMemoryService.getKeyValue();

        // Simulate that left bean has arrived already
        // infraDbKeyValue.put("com.freshworks.core.data.four_five_zero.unit.processor.joins.beans.FbUserBean_left", "user_id_123456");

        List<AbstractAsset> joinAssetData = leftJoinService.getNonPrimitiveAsset(infraDbKeyValue, fbUserUsageAsset.getClass().getName(), fbUserAsset, freshJoin);

        assertThat(joinAssetData.size(), Matchers.is(1));
        FbUserUsageAsset partiallyFormedUserUsageAsset = (FbUserUsageAsset) joinAssetData.get(0);
        assertThat(partiallyFormedUserUsageAsset.getUserId(), Matchers.is(fbUserAsset.getUserId()));
        assertThat(partiallyFormedUserUsageAsset.getUserName(), Matchers.is(fbUserAsset.getUserName()));
        assertThat(partiallyFormedUserUsageAsset.getCreatedAt(), Matchers.nullValue());

        List<AbstractAsset> joinAssetData1 = leftJoinService.getNonPrimitiveAsset(infraDbKeyValue, fbUserUsageAsset.getClass().getName(), fbUsageAsset, freshJoin);

        assertThat(joinAssetData1.size(), Matchers.is(1));
        FbUserUsageAsset fullyFormedUserUsageAsset = (FbUserUsageAsset) joinAssetData1.get(0);
        assertThat(fullyFormedUserUsageAsset.getUserId(), Matchers.is(fbUserAsset.getUserId()));
        assertThat(fullyFormedUserUsageAsset.getUserName(), Matchers.is(fbUserAsset.getUserName()));
        assertThat(fullyFormedUserUsageAsset.getCreatedAt(), Matchers.is(fbUsageAsset.getCreatedAt()));
    }


    @Test
    public void testLookupStagingAreaWithSingleLeftAssetAndRightAssetForInnerJoinService() throws Exception{

        fbUserUsageAssetInnerJoin = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.non_primitive_assets.FbUserUsageAssetInnerJoin");
        fbUserAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUserAsset");
        fbUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUsageAsset");

        // Setting up data 
        ObjectMapper objectMapper = new ObjectMapper();
        FreshJoin freshJoin = fbUserUsageAssetInnerJoin.getAnnotation(FreshJoin.class);

        FbUserUsageAssetInnerJoin fbUserUsageAssetInnerJoin = (FbUserUsageAssetInnerJoin) this.fbUserUsageAssetInnerJoin.getDeclaredConstructor().newInstance();
        FbUserAsset fbUserAsset = (FbUserAsset) this.fbUserAsset.getDeclaredConstructor().newInstance();
        fbUserAsset.setUserId("aggarwal");
        fbUserAsset.setUserName("amit");


        FbUsageAsset fbUsageAsset = (FbUsageAsset) this.fbUsageAsset.getDeclaredConstructor().newInstance();
        fbUsageAsset.setUserId("aggarwal");
        fbUsageAsset.setCreatedAt("2026-01-01");
    

        // Setting up services 
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16),1000000);

        InmemoryService inMemoryService = new InmemoryService();   
        
        Namespace namespace = new Namespace();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, Namespace.class)
        .add(mockFacadeInfraConfigService, InfraConfigService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        innerJoinService.configure(syncServiceContainer, bloomFilter);

        inMemoryService.configure(syncServiceContainer, mockFacadeInfraConfigService.build());
        InfraDbKeyValue infraDbKeyValue = inMemoryService.getKeyValue();

        // Simulate that left bean has arrived already
        // infraDbKeyValue.put("com.freshworks.core.data.four_five_zero.unit.processor.joins.beans.FbUserBean_left", "user_id_123456");

        List<AbstractAsset> joinAssetData = innerJoinService.getNonPrimitiveAsset(infraDbKeyValue, fbUserUsageAssetInnerJoin.getClass().getName(), fbUserAsset, freshJoin);

        assertThat(joinAssetData.size(), Matchers.is(0));

        List<AbstractAsset> joinAssetData1 = innerJoinService.getNonPrimitiveAsset(infraDbKeyValue, fbUserUsageAssetInnerJoin.getClass().getName(), fbUsageAsset, freshJoin);

        assertThat(joinAssetData1.size(), Matchers.is(1));
        FbUserUsageAssetInnerJoin fullyFormedUserUsageAsset = (FbUserUsageAssetInnerJoin) joinAssetData1.get(0);
        assertThat(fullyFormedUserUsageAsset.getUserId(), Matchers.is(fbUserAsset.getUserId()));
        assertThat(fullyFormedUserUsageAsset.getUserName(), Matchers.is(fbUserAsset.getUserName()));
        assertThat(fullyFormedUserUsageAsset.getCreatedAt(), Matchers.is(fbUsageAsset.getCreatedAt()));
    }


    @Test
    public void testLookupStagingAreaWithSingleLeftBeanAndRightBeanWithNullKeyInLeftBean() throws Exception{

        fbUserUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.non_primitive_assets.FbUserUsageAsset");
        fbUserAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUserAsset");
        fbUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUsageAsset");

        // Setting up data 
        ObjectMapper objectMapper = new ObjectMapper();
        FreshJoin freshJoin = fbUserUsageAsset.getAnnotation(FreshJoin.class);
        FbUserUsageAsset fbUserUsageAsset = (FbUserUsageAsset) this.fbUserUsageAsset.getDeclaredConstructor().newInstance();

        FbUserAsset fbUserAsset = (FbUserAsset) this.fbUserAsset.getDeclaredConstructor().newInstance();
        fbUserAsset.setUserName("amit");

        FbUsageAsset fbUsageAsset = (FbUsageAsset) this.fbUsageAsset.getDeclaredConstructor().newInstance();
        fbUsageAsset.setUserId("aggarwal");
        fbUsageAsset.setCreatedAt("2026-01-01");
        
        // Setting up services 
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16),1000000);
    
        InmemoryService inMemoryService = new InmemoryService();   
        
        Namespace namespace = new Namespace();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, Namespace.class)
        .add(mockFacadeInfraConfigService, InfraConfigService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        leftJoinService.configure(syncServiceContainer, bloomFilter);


        inMemoryService.configure(syncServiceContainer, mockFacadeInfraConfigService.build());
        InfraDbKeyValue infraDbKeyValue = inMemoryService.getKeyValue();

        List<AbstractAsset> joinAssetData = leftJoinService.getNonPrimitiveAsset(infraDbKeyValue, fbUserUsageAsset.getClass().getName(), fbUserAsset, freshJoin);

        assertThat(joinAssetData.size(), Matchers.is(1));
        FbUserUsageAsset partiallyFormedUserUsageAsset = (FbUserUsageAsset) joinAssetData.get(0);
        assertThat(partiallyFormedUserUsageAsset.getUserId(), Matchers.nullValue());
        assertThat(partiallyFormedUserUsageAsset.getUserName(), Matchers.is(fbUserAsset.getUserName()));
        assertThat(partiallyFormedUserUsageAsset.getCreatedAt(), Matchers.nullValue());

        // If keys are not matched then for right bean no asset will be generated
        List<AbstractAsset> joinAssetData1 = leftJoinService.getNonPrimitiveAsset(infraDbKeyValue, fbUserUsageAsset.getClass().getName(), fbUsageAsset, freshJoin);
        assertThat(joinAssetData1.size(), Matchers.is(0));
    }

    @Test
    public void testLookupStagingAreaWithMultipleLeftBeanAndSingleRightBean() throws Exception{

        fbUserUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.non_primitive_assets.FbUserUsageAsset");
        fbUserAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUserAsset");
        fbUserAsset1 = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUserAsset");
        fbUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUsageAsset");

        // Setting up data 
        ObjectMapper objectMapper = new ObjectMapper();
        FreshJoin freshJoin = fbUserUsageAsset.getAnnotation(FreshJoin.class);

        FbUserUsageAsset fbUserUsageAsset = (FbUserUsageAsset) this.fbUserUsageAsset.getDeclaredConstructor().newInstance();

        FbUserAsset fbUserAsset = (FbUserAsset) this.fbUserAsset.getDeclaredConstructor().newInstance();
        fbUserAsset.setUserId("user_id_1234");
        fbUserAsset.setUserName("amit");

        FbUserAsset fbUserAsset1 = (FbUserAsset) this.fbUserAsset.getDeclaredConstructor().newInstance();
        fbUserAsset1.setUserId("user_id_1234");
        fbUserAsset1.setUserName("praveen");

        FbUsageAsset fbUsageAsset = (FbUsageAsset) this.fbUsageAsset.getDeclaredConstructor().newInstance();

        fbUsageAsset.setUserId("user_id_1234");
        fbUsageAsset.setCreatedAt("2026-01-01");

        // Setting up services 
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16),1000000);
    
        InmemoryService inMemoryService = new InmemoryService();   
        
        Namespace namespace = new Namespace();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, Namespace.class)
        .add(mockFacadeInfraConfigService, InfraConfigService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        leftJoinService.configure(syncServiceContainer, bloomFilter);

        inMemoryService.configure(syncServiceContainer, mockFacadeInfraConfigService.build());
        InfraDbKeyValue infraDbKeyValue = inMemoryService.getKeyValue();

        // Simulate that left bean has arrived already
        // infraDbKeyValue.put("com.freshworks.core.data.four_five_zero.unit.processor.joins.beans.FbUserBean_left", "user_id_123456");

        List<AbstractAsset> joinAssetData = leftJoinService.getNonPrimitiveAsset(infraDbKeyValue, fbUserUsageAsset.getClass().getName(), fbUserAsset, freshJoin);

        assertThat(joinAssetData.size(), Matchers.is(1));
        FbUserUsageAsset partiallyFormedUserUsageAsset = (FbUserUsageAsset) joinAssetData.get(0);
        
        assertThat(partiallyFormedUserUsageAsset.getUserId(), Matchers.is(fbUserAsset.getUserId()));
        assertThat(partiallyFormedUserUsageAsset.getUserName(), Matchers.is(fbUserAsset.getUserName()));
        assertThat(partiallyFormedUserUsageAsset.getCreatedAt(), Matchers.nullValue());


        List<AbstractAsset> joinAssetDataWithAnotherLeftAsset = leftJoinService.getNonPrimitiveAsset(infraDbKeyValue, fbUserUsageAsset.getClass().getName(), fbUserAsset1, freshJoin);

        assertThat(joinAssetDataWithAnotherLeftAsset.size(), Matchers.is(1));
        FbUserUsageAsset secondPartiallyFormedUserUsageAsset = (FbUserUsageAsset) joinAssetDataWithAnotherLeftAsset.get(0);
        assertThat(secondPartiallyFormedUserUsageAsset.getUserId(), Matchers.is(fbUserAsset1.getUserId()));
        assertThat(secondPartiallyFormedUserUsageAsset.getUserName(), Matchers.is(fbUserAsset1.getUserName()));
        assertThat(secondPartiallyFormedUserUsageAsset.getCreatedAt(), Matchers.nullValue());


        List<AbstractAsset> joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset = leftJoinService.getNonPrimitiveAsset(infraDbKeyValue, fbUserUsageAsset.getClass().getName(), fbUsageAsset, freshJoin);

        assertThat(joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset.size(), Matchers.is(2));


        FbUserUsageAsset assetWithFirstLeftBeanAndSingleRightBean = (FbUserUsageAsset) joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset.get(0);
        assertThat(assetWithFirstLeftBeanAndSingleRightBean.getUserId(), Matchers.is(fbUserAsset.getUserId()));
        assertThat(assetWithFirstLeftBeanAndSingleRightBean.getUserName(), Matchers.is(fbUserAsset.getUserName()));
        assertThat(assetWithFirstLeftBeanAndSingleRightBean.getCreatedAt(), Matchers.is(fbUsageAsset.getCreatedAt()));


        FbUserUsageAsset assetWithSecondLeftBeanAndSingleRightBean = (FbUserUsageAsset) joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset.get(1);
        assertThat(assetWithSecondLeftBeanAndSingleRightBean.getUserId(), Matchers.is(fbUserAsset1.getUserId()));
        assertThat(assetWithSecondLeftBeanAndSingleRightBean.getUserName(), Matchers.is(fbUserAsset1.getUserName()));
        assertThat(assetWithSecondLeftBeanAndSingleRightBean.getCreatedAt(), Matchers.is(fbUsageAsset.getCreatedAt()));

    }

    @Test
    public void testLookupStagingAreaWithMultipleLeftBeanAndSingleRightBeanInnerJoin() throws Exception{

        fbUserUsageAssetInnerJoin = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.non_primitive_assets.FbUserUsageAssetInnerJoin");
        fbUserAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUserAsset");
        fbUserAsset1 = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUserAsset");
        fbUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUsageAsset");

        // Setting up data 
        ObjectMapper objectMapper = new ObjectMapper();
        FreshJoin freshJoin = fbUserUsageAssetInnerJoin.getAnnotation(FreshJoin.class);

        FbUserUsageAssetInnerJoin fbUserUsageAssetInnerJoin = (FbUserUsageAssetInnerJoin) this.fbUserUsageAssetInnerJoin.getDeclaredConstructor().newInstance();

        FbUserAsset fbUserAsset = (FbUserAsset) this.fbUserAsset.getDeclaredConstructor().newInstance();
        fbUserAsset.setUserId("user_id_1234");
        fbUserAsset.setUserName("amit");

        FbUserAsset fbUserAsset1 = (FbUserAsset) this.fbUserAsset.getDeclaredConstructor().newInstance();
        fbUserAsset1.setUserId("user_id_1234");
        fbUserAsset1.setUserName("praveen");

        FbUsageAsset fbUsageAsset = (FbUsageAsset) this.fbUsageAsset.getDeclaredConstructor().newInstance();

        fbUsageAsset.setUserId("user_id_1234");
        fbUsageAsset.setCreatedAt("2026-01-01");

        // Setting up services 
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16),1000000);
    
        InmemoryService inMemoryService = new InmemoryService();   
        
        Namespace namespace = new Namespace();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, Namespace.class)
        .add(mockFacadeInfraConfigService, InfraConfigService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        innerJoinService.configure(syncServiceContainer, bloomFilter);

        inMemoryService.configure(syncServiceContainer, mockFacadeInfraConfigService.build());
        InfraDbKeyValue infraDbKeyValue = inMemoryService.getKeyValue();

        // Simulate that left bean has arrived already
        // infraDbKeyValue.put("com.freshworks.core.data.four_five_zero.unit.processor.joins.beans.FbUserBean_left", "user_id_123456");

        List<AbstractAsset> joinAssetData = innerJoinService.getNonPrimitiveAsset(infraDbKeyValue, fbUserUsageAssetInnerJoin.getClass().getName(), fbUserAsset, freshJoin);
        assertThat(joinAssetData.size(), Matchers.is(0));


        List<AbstractAsset> joinAssetDataWithAnotherLeftAsset = innerJoinService.getNonPrimitiveAsset(infraDbKeyValue, fbUserUsageAssetInnerJoin.getClass().getName(), fbUserAsset1, freshJoin);
        assertThat(joinAssetDataWithAnotherLeftAsset.size(), Matchers.is(0));



        List<AbstractAsset> joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset = innerJoinService.getNonPrimitiveAsset(infraDbKeyValue, fbUserUsageAssetInnerJoin.getClass().getName(), fbUsageAsset, freshJoin);
        assertThat(joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset.size(), Matchers.is(2));


        FbUserUsageAssetInnerJoin assetWithFirstLeftBeanAndSingleRightBean = (FbUserUsageAssetInnerJoin) joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset.get(0);
        assertThat(assetWithFirstLeftBeanAndSingleRightBean.getUserId(), Matchers.is(fbUserAsset.getUserId()));
        assertThat(assetWithFirstLeftBeanAndSingleRightBean.getUserName(), Matchers.is(fbUserAsset.getUserName()));
        assertThat(assetWithFirstLeftBeanAndSingleRightBean.getCreatedAt(), Matchers.is(fbUsageAsset.getCreatedAt()));


        FbUserUsageAssetInnerJoin assetWithSecondLeftBeanAndSingleRightBean = (FbUserUsageAssetInnerJoin) joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset.get(1);
        assertThat(assetWithSecondLeftBeanAndSingleRightBean.getUserId(), Matchers.is(fbUserAsset1.getUserId()));
        assertThat(assetWithSecondLeftBeanAndSingleRightBean.getUserName(), Matchers.is(fbUserAsset1.getUserName()));
        assertThat(assetWithSecondLeftBeanAndSingleRightBean.getCreatedAt(), Matchers.is(fbUsageAsset.getCreatedAt()));

    }
}
