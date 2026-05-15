package com.freshworks.core.processor;

import com.freshworks.core.data.four_zero_zero.unit.dag.assets.inner.TestInnerAsset;
import com.freshworks.core.data.four_zero_zero.unit.dag.assets.inner.innermost.TestInnerMostAsset;
import com.freshworks.core.data.four_zero_zero.unit.dag.assets.inner.innermost.TestInnerMostJoinedAsset;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;
import com.google.common.collect.ImmutableListMultimap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doCallRealMethod;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestAssetDependencyService {

    @Autowired
    MockFacadeAssetBeanDependencyService mockFacadeAssetBeanDependencyService;

    @Autowired
    MockFacadeProcessorConfigService mockFacadeProcessorConfigService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    String releaseVersion;

    @BeforeEach
    public void beforeEach() throws Exception {
        releaseVersion = System.getProperty("spring.profiles.active").split("\\.")[0];
        mockFacadeAssetBeanDependencyService.configure().build();
        mockFacadeProcessorConfigService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
    }

    @Test
    public void testWhenAssetPathIsOuterThenAllInnerPackageAssetsAreAlsoScanned() throws Exception {

        ProcessorConfigService processorConfigService = mockFacadeProcessorConfigService
                .getAssetLocation("com.freshworks.core.data." + releaseVersion + ".unit.dag.assets")
                .getBeanLocation("com.freshworks.core.data."+ releaseVersion + ".unit.dag.beans")
                .build();

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .build();

        AssetBeanDependencyService assetBeanDependencyService = mockFacadeAssetBeanDependencyService
                .build();

        doCallRealMethod().when(assetBeanDependencyService).scanner(anyString(), any());
        doCallRealMethod().when(assetBeanDependencyService).findDependencyOfAsset(anyList(), any());

        ImmutableListMultimap<String, String> x = assetBeanDependencyService.scanner("some-random-namespace", processorConfigService);

        assertThat(x.containsKey(TestInnerMostAsset.class.getName()), Matchers.is(true));
        assertThat(x.containsKey(TestInnerMostJoinedAsset.class.getName()), Matchers.is(true));
        assertThat(x.containsKey(TestInnerAsset.class.getName()), Matchers.is(true));
    }
}
