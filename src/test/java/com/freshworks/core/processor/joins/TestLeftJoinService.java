package com.freshworks.core.processor.joins;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;

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

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestLeftJoinService {
    
    @Autowired
    LeftJoinService leftJoinService;
    String releaseVersion;

    @BeforeEach
    public void beforeEach() throws Exception {
        releaseVersion = System.getProperty("spring.profiles.active").split("\\.")[0];
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
}
