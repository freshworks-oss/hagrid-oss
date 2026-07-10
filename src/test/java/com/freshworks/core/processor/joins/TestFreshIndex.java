package com.freshworks.core.processor.joins;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.freshworks.core.TestUtility;
import com.freshworks.core.data.five_zero_zero.unit.processor.joins.assets.FbUserUsageAssetFreshIndex;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.FreshIndexBeanSerializeModifier;


@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestFreshIndex {

    ObjectMapper freshIndexObjectMapper;

    String releaseVersion;
    Class<? extends AbstractAsset> fbUserUsageAssetFreshIndex;
    Class<? extends AbstractAsset> fbUserBean;


    @BeforeEach
    public void beforeEach() throws Exception {

        releaseVersion = System.getProperty("spring.profiles.active").split("\\.")[0];

        freshIndexObjectMapper = new ObjectMapper();
        freshIndexObjectMapper.registerModule(new SimpleModule(){

            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addBeanSerializerModifier(new FreshIndexBeanSerializeModifier());
            }
        });

        freshIndexObjectMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);

        fbUserUsageAssetFreshIndex = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.processor.joins.assets.FbUserUsageAssetFreshIndex");
        fbUserBean = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.processor.joins.beans.FbUserBean");
    }

    @Test
    public void testFreshIndexSerializerWhenIndexKeyIsPresent() throws Exception{

        AbstractAsset fbUserUsageAssetFreshIndex =  this.fbUserUsageAssetFreshIndex.getDeclaredConstructor().newInstance();
        
        TestUtility.callMethod(fbUserUsageAssetFreshIndex, "setUserId", "user_id_12345");
        TestUtility.callMethod(fbUserUsageAssetFreshIndex, "setFirstName", "amit");
        TestUtility.callMethod(fbUserUsageAssetFreshIndex, "setLastName", "aggarwal");
        TestUtility.callMethod(fbUserUsageAssetFreshIndex, "setCreatedAt", "20-02-2026");



        String s = freshIndexObjectMapper.writeValueAsString(fbUserUsageAssetFreshIndex);
        assertThat(s.length(), Matchers.is(Matchers.greaterThan(0)));
        assertThat(s.contains("user_id_12345"), Matchers.is(true));
        assertThat(s.contains("null"), Matchers.is(false));
        assertThat(s.contains("amit"), Matchers.is(false));
        assertThat(s.contains("aggarwal"), Matchers.is(false));
        assertThat(s.contains("20-02-2026"), Matchers.is(false));
    }

        @Test
    public void testFreshIndexSerializerWhenIndexKeyIsAbsent() throws Exception{

        AbstractAsset fbUserUsageAssetFreshIndex =  this.fbUserUsageAssetFreshIndex.getDeclaredConstructor().newInstance();

        TestUtility.callMethod(fbUserUsageAssetFreshIndex, "setFirstName", "amit");
        TestUtility.callMethod(fbUserUsageAssetFreshIndex, "setLastName", "aggarwal");
        TestUtility.callMethod(fbUserUsageAssetFreshIndex, "setCreatedAt", "20-02-2026");

        String s = freshIndexObjectMapper.writeValueAsString(fbUserUsageAssetFreshIndex);

        // Ideally any property that is marked with @FreshIndex that should NOT be null. 
        // If it is null then it will be tranlated to null and may give erraneous results. 
        // I can not ignore such assets from freshindex because they will anyways be inserted in mongo. 
        // Hence it is responsibility of developer to make sure any property marked as @freshindex, should not be null
        assertThat(s.contains("null"), Matchers.is(true));
        assertThat(s.contains("amit"), Matchers.is(false));
        assertThat(s.contains("aggarwal"), Matchers.is(false));
        assertThat(s.contains("20-02-2026"), Matchers.is(false));
    }
    
}
