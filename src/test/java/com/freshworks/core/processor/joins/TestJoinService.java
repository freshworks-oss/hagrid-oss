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
import com.freshworks.core.data.four_zero_zero.unit.processor.joins.assets.FbUserUsageAsset;
import com.freshworks.core.data.four_zero_zero.unit.processor.joins.beans.FbApplicationBean;
import com.freshworks.core.data.four_zero_zero.unit.processor.joins.beans.FbServicePrincipleBean;
import com.freshworks.core.data.four_zero_zero.unit.processor.joins.beans.FbUsageBean;
import com.freshworks.core.data.four_zero_zero.unit.processor.joins.beans.FbUserBean;
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

    @BeforeEach
    public void beforeEach() throws Exception {

        mockFacadeInfraConfigService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
    }

    @Test
    public void testGetLookupFieldValueOfLeftClass() throws Exception{

        FbUserBean fbUserBean = new FbUserBean();
        fbUserBean.setLastName("aggarwal");
        fbUserBean.setFirstName("amit");
        fbUserBean.setId("user_id_123456");
        
        FreshJoin freshJoin = FbUserUsageAsset.class.getAnnotation(FreshJoin.class);
        String lookupFieldValueOfLeftClass = leftJoinService.getLookupFieldValueOfLeftClass(fbUserBean, freshJoin);
        
        assertThat(lookupFieldValueOfLeftClass, Matchers.is("user_id_123456"));
                
    }

    @Test
    public void testGetLookupFieldValueOfRightClass() throws Exception{

        FbUsageBean fbUsageBean = new FbUsageBean();
        fbUsageBean.setCreatedAt("02-06-1989");
        fbUsageBean.setUserId("user_id_654321");
        
        FreshJoin freshJoin = FbUserUsageAsset.class.getAnnotation(FreshJoin.class);
        String lookupFieldValueOfRightClass = leftJoinService.getLookupFieldValueOfRightClass(fbUsageBean, freshJoin);
        
        assertThat(lookupFieldValueOfRightClass, Matchers.is("user_id_654321"));

    }

    @Test
    public void testCompareParentWhenThereIsCommonParent() throws Exception{
        
        ObjectMapper objectMapper = new ObjectMapper();

        FbApplicationBean fbApplicationBean = new FbApplicationBean();
        fbApplicationBean.setApplicationId("app_id_994433");
        fbApplicationBean.setApplicationName("slack");
        fbApplicationBean.setClazz(fbApplicationBean.getClass().getName());

        FbServicePrincipleBean fbServicePrincipleBean = new FbServicePrincipleBean();
        fbServicePrincipleBean.setServicePrincipleId("sp_id_5575775");
        fbServicePrincipleBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));

        fbServicePrincipleBean.setClazz(fbServicePrincipleBean.getClass().getName());

        FbUserBean fbUserBean = new FbUserBean();
        fbUserBean.setLastName("aggarwal");
        fbUserBean.setFirstName("amit");
        fbUserBean.setId("user_id_123456");
        fbUserBean.setParentBean(objectMapper.convertValue(fbServicePrincipleBean, JsonNode.class));
        fbUserBean.setClazz(fbUserBean.getClass().getName());

        FbUsageBean fbUsageBean = new FbUsageBean();
        fbUsageBean.setCreatedAt("02-06-1989");
        fbUsageBean.setUserId("user_id_654321");
        fbUsageBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));
        fbUsageBean.setClazz(fbUsageBean.getClass().getName());

        ArrayList<AbstractBean> list = new ArrayList<>();
        list.add(0, fbUserBean);
        HashMap<String, AbstractBean> unwrappedLeftClassMap = leftJoinService.unwrappedBeanToClassMap(list);
        
        list.add(0, fbUsageBean);
        HashMap<String, AbstractBean> unwrappedRightClassMap = leftJoinService.unwrappedBeanToClassMap(list);

        Boolean isEqual = leftJoinService.compareParent(unwrappedLeftClassMap, unwrappedRightClassMap);
        assertThat(isEqual, Matchers.is(true));
    }

    @Test
    public void testCompareAttributesWhenThereIsCommonParentAndValuesAreMatching() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper();

        FreshJoin freshJoin = FbUserUsageAsset.class.getAnnotation(FreshJoin.class);

        FbApplicationBean fbApplicationBean = new FbApplicationBean();
        fbApplicationBean.setApplicationId("app_id_994433");
        fbApplicationBean.setApplicationName("slack");
        fbApplicationBean.setClazz(fbApplicationBean.getClass().getName());

        FbServicePrincipleBean fbServicePrincipleBean = new FbServicePrincipleBean();
        fbServicePrincipleBean.setServicePrincipleId("sp_id_5575775");
        fbServicePrincipleBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));

        fbServicePrincipleBean.setClazz(fbServicePrincipleBean.getClass().getName());

        FbUserBean fbUserBean = new FbUserBean();
        fbUserBean.setLastName("aggarwal");
        fbUserBean.setFirstName("amit");
        fbUserBean.setId("user_id_123456");
        fbUserBean.setParentBean(objectMapper.convertValue(fbServicePrincipleBean, JsonNode.class));
        fbUserBean.setClazz(fbUserBean.getClass().getName());

        FbUsageBean fbUsageBean = new FbUsageBean();
        fbUsageBean.setCreatedAt("02-06-1989");
        fbUsageBean.setUserId("user_id_123456");
        fbUsageBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));
        fbUsageBean.setClazz(fbUsageBean.getClass().getName());

        ArrayList<AbstractBean> list = new ArrayList<>();
        list.add(0, fbUserBean);
        HashMap<String, AbstractBean> unwrappedLeftClassMap = leftJoinService.unwrappedBeanToClassMap(list);

        Boolean isEqual = leftJoinService.compareAttributes(unwrappedLeftClassMap, fbUsageBean, freshJoin);
        assertThat(isEqual, Matchers.is(true));

    }

    @Test
    public void testCompareAttributesWhenThereIsCommonParentAndValuesAreNotMatching() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper();

        FreshJoin freshJoin = FbUserUsageAsset.class.getAnnotation(FreshJoin.class);

        FbApplicationBean fbApplicationBean = new FbApplicationBean();
        fbApplicationBean.setApplicationId("app_id_994433");
        fbApplicationBean.setApplicationName("slack");
        fbApplicationBean.setClazz(fbApplicationBean.getClass().getName());

        FbServicePrincipleBean fbServicePrincipleBean = new FbServicePrincipleBean();
        fbServicePrincipleBean.setServicePrincipleId("sp_id_5575775");
        fbServicePrincipleBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));

        fbServicePrincipleBean.setClazz(fbServicePrincipleBean.getClass().getName());

        FbUserBean fbUserBean = new FbUserBean();
        fbUserBean.setLastName("aggarwal");
        fbUserBean.setFirstName("amit");
        fbUserBean.setId("user_id_123456");
        fbUserBean.setParentBean(objectMapper.convertValue(fbServicePrincipleBean, JsonNode.class));
        fbUserBean.setClazz(fbUserBean.getClass().getName());

        FbUsageBean fbUsageBean = new FbUsageBean();
        fbUsageBean.setCreatedAt("02-06-1989");
        fbUsageBean.setUserId("user_id_654321");
        fbUsageBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));
        fbUsageBean.setClazz(fbUsageBean.getClass().getName());

        ArrayList<AbstractBean> list = new ArrayList<>();
        list.add(0, fbUserBean);
        HashMap<String, AbstractBean> unwrappedLeftClassMap = leftJoinService.unwrappedBeanToClassMap(list);

        Boolean isEqual = leftJoinService.compareAttributes(unwrappedLeftClassMap, fbUsageBean, freshJoin);
        assertThat(isEqual, Matchers.is(false));

    }

    @Test
    public void testUnwrappedBeanToClassMap() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper();

        FbApplicationBean fbApplicationBean = new FbApplicationBean();
        fbApplicationBean.setApplicationId("app_id_994433");
        fbApplicationBean.setApplicationName("slack");
        fbApplicationBean.setClazz(fbApplicationBean.getClass().getName());

        FbServicePrincipleBean fbServicePrincipleBean = new FbServicePrincipleBean();
        fbServicePrincipleBean.setServicePrincipleId("sp_id_5575775");
        fbServicePrincipleBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));

        fbServicePrincipleBean.setClazz(fbServicePrincipleBean.getClass().getName());

        FbUserBean fbUserBean = new FbUserBean();
        fbUserBean.setLastName("aggarwal");
        fbUserBean.setFirstName("amit");
        fbUserBean.setId("user_id_123456");
        fbUserBean.setParentBean(objectMapper.convertValue(fbServicePrincipleBean, JsonNode.class));
        fbUserBean.setClazz(fbUserBean.getClass().getName());

        ArrayList<AbstractBean> list = new ArrayList<>();
        list.add(0, fbUserBean);

        HashMap<String, AbstractBean> unwrappedBeanToClassMap = leftJoinService.unwrappedBeanToClassMap(list);

        assertThat(unwrappedBeanToClassMap.size(), Matchers.is(3));
        Set<String> keySet = unwrappedBeanToClassMap.keySet();

        for(String key : keySet){
            System.out.println(key);
        }
        
        assertThat(unwrappedBeanToClassMap.containsKey("com.freshworks.core.data.four_zero_zero.unit.processor.joins.beans.FbApplicationBean"), Matchers.is(true));

        assertThat(unwrappedBeanToClassMap.containsKey("com.freshworks.core.data.four_zero_zero.unit.processor.joins.beans.FbServicePrincipleBean"), Matchers.is(true));

        assertThat(unwrappedBeanToClassMap.containsKey("com.freshworks.core.data.four_zero_zero.unit.processor.joins.beans.FbUserBean"), Matchers.is(true));

    }

    @Test
    public void testLookupStagingAreaWithSingleLeftBeanAndRightBean() throws Exception{

        // Setting up data 
        ObjectMapper objectMapper = new ObjectMapper();

        FreshJoin freshJoin = FbUserUsageAsset.class.getAnnotation(FreshJoin.class);

        FbApplicationBean fbApplicationBean = new FbApplicationBean();
        fbApplicationBean.setApplicationId("app_id_994433");
        fbApplicationBean.setApplicationName("slack");
        fbApplicationBean.setClazz(fbApplicationBean.getClass().getName());

        FbServicePrincipleBean fbServicePrincipleBean = new FbServicePrincipleBean();
        fbServicePrincipleBean.setServicePrincipleId("sp_id_5575775");
        fbServicePrincipleBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));

        fbServicePrincipleBean.setClazz(fbServicePrincipleBean.getClass().getName());

        FbUserBean fbUserBean = new FbUserBean();
        fbUserBean.setLastName("aggarwal");
        fbUserBean.setFirstName("amit");
        fbUserBean.setId("user_id_123456");
        fbUserBean.setParentBean(objectMapper.convertValue(fbServicePrincipleBean, JsonNode.class));
        fbUserBean.setClazz(fbUserBean.getClass().getName());

        FbUsageBean fbUsageBean = new FbUsageBean();
        fbUsageBean.setCreatedAt("02-06-1989");
        fbUsageBean.setUserId("user_id_123456");
        fbUsageBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));
        fbUsageBean.setClazz(fbUsageBean.getClass().getName());


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
        // infraDbKeyValue.put("com.freshworks.core.data.four_zero_zero.unit.processor.joins.beans.FbUserBean_left", "user_id_123456");

        List<HashMap<String, AbstractBean>> joinBeanData = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUserBean, freshJoin);

        // String s = objectMapper.writeValueAsString(joinBeanData);
        // System.out.print(s);

        assertThat(joinBeanData.size(), Matchers.is(1));
        HashMap<String, AbstractBean> beanMap = joinBeanData.get(0);
        assertThat(beanMap.size(), Matchers.is(3));

        List<HashMap<String, AbstractBean>> joinBeanData1 = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUsageBean, freshJoin);

        String s = objectMapper.writeValueAsString(joinBeanData1);
        System.out.print(s);

        assertThat(joinBeanData1.size(), Matchers.is(1));
        HashMap<String, AbstractBean> beanMap1 = joinBeanData1.get(0);
        assertThat(beanMap1.size(), Matchers.is(4));
    }

    @Test
    public void testLookupStagingAreaWithSingleLeftBeanAndRightBeanWithNullKeyInLeftBean() throws Exception{

        // Setting up data 
        ObjectMapper objectMapper = new ObjectMapper();

        FreshJoin freshJoin = FbUserUsageAsset.class.getAnnotation(FreshJoin.class);

        FbApplicationBean fbApplicationBean = new FbApplicationBean();
        fbApplicationBean.setApplicationId("app_id_994433");
        fbApplicationBean.setApplicationName("slack");
        fbApplicationBean.setClazz(fbApplicationBean.getClass().getName());

        FbServicePrincipleBean fbServicePrincipleBean = new FbServicePrincipleBean();
        fbServicePrincipleBean.setServicePrincipleId("sp_id_5575775");
        fbServicePrincipleBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));

        fbServicePrincipleBean.setClazz(fbServicePrincipleBean.getClass().getName());

        FbUserBean fbUserBean = new FbUserBean();
        fbUserBean.setLastName("aggarwal");
        fbUserBean.setFirstName("amit");
        fbUserBean.setParentBean(objectMapper.convertValue(fbServicePrincipleBean, JsonNode.class));
        fbUserBean.setClazz(fbUserBean.getClass().getName());

        FbUsageBean fbUsageBean = new FbUsageBean();
        fbUsageBean.setCreatedAt("02-06-1989");
        fbUsageBean.setUserId("user_id_123456");
        fbUsageBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));
        fbUsageBean.setClazz(fbUsageBean.getClass().getName());


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
        // infraDbKeyValue.put("com.freshworks.core.data.four_zero_zero.unit.processor.joins.beans.FbUserBean_left", "user_id_123456");

        List<HashMap<String, AbstractBean>> joinBeanData = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUserBean, freshJoin);

        // String s = objectMapper.writeValueAsString(joinBeanData);
        // System.out.print(s);

        assertThat(joinBeanData.size(), Matchers.is(1));
        HashMap<String, AbstractBean> beanMap = joinBeanData.get(0);
        assertThat(beanMap.size(), Matchers.is(3));

        List<HashMap<String, AbstractBean>> joinBeanData1 = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUsageBean, freshJoin);

        assertThat(joinBeanData1.size(), Matchers.is(0));
    }

    @Test
    public void testLookupStagingAreaWithMultipleLeftBeanAndSingleRightBean() throws Exception{

        // Setting up data 
        ObjectMapper objectMapper = new ObjectMapper();

        FreshJoin freshJoin = FbUserUsageAsset.class.getAnnotation(FreshJoin.class);

        FbApplicationBean fbApplicationBean = new FbApplicationBean();
        fbApplicationBean.setApplicationId("app_id_994433");
        fbApplicationBean.setApplicationName("slack");
        fbApplicationBean.setClazz(fbApplicationBean.getClass().getName());

        FbServicePrincipleBean fbServicePrincipleBean = new FbServicePrincipleBean();
        fbServicePrincipleBean.setServicePrincipleId("sp_id_5575775");
        fbServicePrincipleBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));

        fbServicePrincipleBean.setClazz(fbServicePrincipleBean.getClass().getName());

        FbUserBean fbUserBean = new FbUserBean();
        fbUserBean.setLastName("aggarwal");
        fbUserBean.setFirstName("amit");
        fbUserBean.setId("user_id_123456");
        fbUserBean.setParentBean(objectMapper.convertValue(fbServicePrincipleBean, JsonNode.class));
        fbUserBean.setClazz(fbUserBean.getClass().getName());


        FbUserBean fbUserBean1 = new FbUserBean();
        fbUserBean1.setLastName("menon");
        fbUserBean1.setFirstName("praveen");
        fbUserBean1.setId("user_id_123456");
        fbUserBean1.setParentBean(objectMapper.convertValue(fbServicePrincipleBean, JsonNode.class));
        fbUserBean1.setClazz(fbUserBean1.getClass().getName());



        FbUsageBean fbUsageBean = new FbUsageBean();
        fbUsageBean.setCreatedAt("02-06-1989");
        fbUsageBean.setUserId("user_id_123456");
        fbUsageBean.setParentBean(objectMapper.convertValue(fbApplicationBean, JsonNode.class));
        fbUsageBean.setClazz(fbUsageBean.getClass().getName());


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
        // infraDbKeyValue.put("com.freshworks.core.data.four_zero_zero.unit.processor.joins.beans.FbUserBean_left", "user_id_123456");

        List<HashMap<String, AbstractBean>> joinBeanData = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUserBean, freshJoin);

        // String s = objectMapper.writeValueAsString(joinBeanData);
        // System.out.print(s);

        assertThat(joinBeanData.size(), Matchers.is(1));
        HashMap<String, AbstractBean> beanMap = joinBeanData.get(0);
        assertThat(beanMap.size(), Matchers.is(3));


        List<HashMap<String, AbstractBean>> joinBeanDataWithAnotherLeftBean = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUserBean1, freshJoin);

        // String s = objectMapper.writeValueAsString(joinBeanDataWithAnotherLeftBean);
        // System.out.print(s);

        assertThat(joinBeanDataWithAnotherLeftBean.size(), Matchers.is(1));
        HashMap<String, AbstractBean> beanMapWithSecondLeftBean = joinBeanDataWithAnotherLeftBean.get(0);
        assertThat(beanMapWithSecondLeftBean.size(), Matchers.is(3));


        List<HashMap<String, AbstractBean>> joinBeanDataWithSingleRightBeanMatchingWithTwoLeftBean = leftJoinService.lookupStagingArea(infraDbKeyValue, fbUsageBean, freshJoin);

        String s = objectMapper.writeValueAsString(joinBeanDataWithSingleRightBeanMatchingWithTwoLeftBean);
        System.out.print(s);

        assertThat(joinBeanDataWithSingleRightBeanMatchingWithTwoLeftBean.size(), Matchers.is(2));
        HashMap<String, AbstractBean> beanMapWithFirstLeftBeanAndSingleRightBean = joinBeanDataWithSingleRightBeanMatchingWithTwoLeftBean.get(0);
        assertThat(beanMapWithFirstLeftBeanAndSingleRightBean.size(), Matchers.is(4));

        HashMap<String, AbstractBean> beanMapWithSecondLeftBeanAndSingleRightBean = joinBeanDataWithSingleRightBeanMatchingWithTwoLeftBean.get(0);
        assertThat(beanMapWithSecondLeftBeanAndSingleRightBean.size(), Matchers.is(4));
    }
}
