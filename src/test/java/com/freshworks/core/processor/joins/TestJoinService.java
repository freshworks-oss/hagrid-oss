package com.freshworks.core.processor.joins;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
import com.freshworks.core.data.four_five_zero.unit.processor.joins.assets.non_primitive_assets.FbUserUsageAsset;
import com.freshworks.core.data.four_five_zero.unit.processor.joins.beans.FbUsageBean;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestJoinService {
    
    @Autowired
    LeftJoinService leftJoinService;
    
    @Autowired
    MockFacadeInfraConfigService mockFacadeInfraConfigService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    AnalyticsFactory analyticsFactory;

    Class<? extends AbstractAsset> fbUserUsageAsset;
    Class<? extends AbstractAsset> fbUserAsset;
    Class<? extends AbstractAsset> fbUserAsset1;
    Class<? extends AbstractAsset> fbUsageAsset;
    Class<? extends AbstractAsset> fbUsageAsset1;


    @BeforeEach
    public void beforeEach() throws Exception {

        mockFacadeInfraConfigService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
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


        AbstractAsset fbUserAsset = this.fbUserAsset.getDeclaredConstructor().newInstance();
        TestUtility.callMethod(fbUserAsset, "setUserId", "aggarwal");
        TestUtility.callMethod(fbUserAsset, "setUserName", "amit");

        AbstractAsset fbUsageAsset = this.fbUsageAsset.getDeclaredConstructor().newInstance();
        TestUtility.callMethod(fbUsageAsset, "setUserId", "aggarwal");
        TestUtility.callMethod(fbUsageAsset, "setCreatedAt", "2026-01-01");


        // Setting up services 
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16),1000000);

        leftJoinService.configure(bloomFilter);
    
        InmemoryService inMemoryService = new InmemoryService();   
        
        Namespace namespace = new Namespace();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, Namespace.class)
        .add(mockFacadeInfraConfigService, InfraConfigService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();


        inMemoryService.configure(syncServiceContainer, mockFacadeInfraConfigService.build());
        InfraDbKeyValue infraDbKeyValue = inMemoryService.getKeyValue();

        // Simulate that left bean has arrived already
        // infraDbKeyValue.put("com.freshworks.core.data.four_five_zero.unit.processor.joins.beans.FbUserBean_left", "user_id_123456");

        List<HashMap<String, AbstractAsset>> joinAssetData = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUserAsset, freshJoin);

        // String s = objectMapper.writeValueAsString(joinAssetData);
        // System.out.print(s);

        assertThat(joinAssetData.size(), Matchers.is(1));
        HashMap<String, AbstractAsset> assetMap = joinAssetData.get(0);
        assertThat(assetMap.size(), Matchers.is(1));

        List<HashMap<String, AbstractAsset>> joinAssetData1 = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUsageAsset, freshJoin);

        String s = objectMapper.writeValueAsString(joinAssetData1);
        System.out.print(s);

        assertThat(joinAssetData1.size(), Matchers.is(1));
        HashMap<String, AbstractAsset> assetMap1 = joinAssetData1.get(0);
        assertThat(assetMap1.size(), Matchers.is(2));
    }

    @Test
    public void testLookupStagingAreaWithSingleLeftBeanAndRightBeanWithNullKeyInLeftBean() throws Exception{

        fbUserUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.non_primitive_assets.FbUserUsageAsset");
        fbUserAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUserAsset");
        fbUsageAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + TestUtility.getReleaseVerion() + ".unit.processor.joins.assets.FbUsageAsset");

        // Setting up data 
        ObjectMapper objectMapper = new ObjectMapper();

        FreshJoin freshJoin = fbUserUsageAsset.getAnnotation(FreshJoin.class);


        AbstractAsset fbUserAsset = this.fbUserAsset.getDeclaredConstructor().newInstance();
        TestUtility.callMethod(fbUserAsset, "setUserName", "amit");

        AbstractAsset fbUsageAsset = this.fbUsageAsset.getDeclaredConstructor().newInstance();
        TestUtility.callMethod(fbUsageAsset, "setUserId", "aggarwal");
        TestUtility.callMethod(fbUsageAsset, "setCreatedAt", "2026-01-01");


        // Setting up services 
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16),1000000);

        leftJoinService.configure(bloomFilter);
    
        InmemoryService inMemoryService = new InmemoryService();   
        
        Namespace namespace = new Namespace();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, Namespace.class)
        .add(mockFacadeInfraConfigService, InfraConfigService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();


        inMemoryService.configure(syncServiceContainer, mockFacadeInfraConfigService.build());
        InfraDbKeyValue infraDbKeyValue = inMemoryService.getKeyValue();

        // Simulate that left bean has arrived already
        // infraDbKeyValue.put("com.freshworks.core.data.four_five_zero.unit.processor.joins.beans.FbUserBean_left", "user_id_123456");

        List<HashMap<String, AbstractAsset>> joinAssetData = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUserAsset, freshJoin);

        assertThat(joinAssetData.size(), Matchers.is(1));
        HashMap<String, AbstractAsset> assetMap = joinAssetData.get(0);
        assertThat(assetMap.size(), Matchers.is(1));

        List<HashMap<String, AbstractAsset>> joinAssetData1 = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUsageAsset, freshJoin);

        String s = objectMapper.writeValueAsString(joinAssetData1);
        System.out.print(s);

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


        AbstractAsset fbUserAsset = this.fbUserAsset.getDeclaredConstructor().newInstance();
        TestUtility.callMethod(fbUserAsset, "setUserId", "user_id_1234");
        TestUtility.callMethod(fbUserAsset, "setUserName", "amit");


        AbstractAsset fbUserAsset1 = this.fbUserAsset.getDeclaredConstructor().newInstance();
        TestUtility.callMethod(fbUserAsset1, "setUserId", "user_id_1234");
        TestUtility.callMethod(fbUserAsset1, "setUserName", "praveen");


        AbstractAsset fbUsageAsset = this.fbUsageAsset.getDeclaredConstructor().newInstance();
        TestUtility.callMethod(fbUsageAsset, "setUserId", "user_id_1234");
        TestUtility.callMethod(fbUsageAsset, "setCreatedAt", "2026-01-01");


        // Setting up services 
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16),1000000);

        leftJoinService.configure(bloomFilter);
    
        InmemoryService inMemoryService = new InmemoryService();   
        
        Namespace namespace = new Namespace();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, Namespace.class)
        .add(mockFacadeInfraConfigService, InfraConfigService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();


        inMemoryService.configure(syncServiceContainer, mockFacadeInfraConfigService.build());
        InfraDbKeyValue infraDbKeyValue = inMemoryService.getKeyValue();

        // Simulate that left bean has arrived already
        // infraDbKeyValue.put("com.freshworks.core.data.four_five_zero.unit.processor.joins.beans.FbUserBean_left", "user_id_123456");

        List<HashMap<String, AbstractAsset>> joinAssetData = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUserAsset, freshJoin);

        // String s = objectMapper.writeValueAsString(joinBeanData);
        // System.out.print(s);

        assertThat(joinAssetData.size(), Matchers.is(1));
        HashMap<String, AbstractAsset> beanMap = joinAssetData.get(0);
        assertThat(beanMap.size(), Matchers.is(1));


        List<HashMap<String, AbstractAsset>> joinAssetDataWithAnotherLeftAsset = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUserAsset1, freshJoin);

        // String s = objectMapper.writeValueAsString(joinBeanDataWithAnotherLeftBean);
        // System.out.print(s);

        assertThat(joinAssetDataWithAnotherLeftAsset.size(), Matchers.is(1));
        HashMap<String, AbstractAsset> assetMapWithSecondLeftBean = joinAssetDataWithAnotherLeftAsset.get(0);
        assertThat(assetMapWithSecondLeftBean.size(), Matchers.is(1));


        List<HashMap<String, AbstractAsset>> joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUsageAsset, freshJoin);

        assertThat(joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset.size(), Matchers.is(2));
        HashMap<String, AbstractAsset> beanMapWithFirstLeftBeanAndSingleRightBean = joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset.get(0);
        assertThat(beanMapWithFirstLeftBeanAndSingleRightBean.size(), Matchers.is(2));

        HashMap<String, AbstractAsset> assetMapWithSecondLeftAssetAndSingleRightAsset = joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset.get(1);
        assertThat(assetMapWithSecondLeftAssetAndSingleRightAsset.size(), Matchers.is(2));

        String s = objectMapper.writeValueAsString(joinBeanDataWithSingleRightAssetMatchingWithTwoLeftAsset);
        System.out.print(s);
    }
}
