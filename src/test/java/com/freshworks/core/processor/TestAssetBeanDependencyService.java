package com.freshworks.core.processor;

import com.freshworks.core.TestUtility;
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
import java.util.List;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestAssetBeanDependencyService {

    @Autowired
    MockFacadeAssetBeanDependencyService mockFacadeAssetBeanDependencyService;

    @Autowired
    MockFacadeProcessorConfigService mockFacadeProcessorConfigService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    String releaseVersion;

    Class<? extends AbstractAsset> innerAsset;
    Class<? extends AbstractAsset> innerMostAsset;
    Class<? extends AbstractAsset> innerMostJoinedAsset;

    @BeforeEach
    public void beforeEach() throws Exception {

        releaseVersion = TestUtility.getReleaseVerion();
        mockFacadeAssetBeanDependencyService.configure().build();
        mockFacadeProcessorConfigService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();

        innerAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.assets.inner.TestInnerAsset");
        innerMostAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.assets.inner.innermost.TestInnerMostAsset");
        innerMostJoinedAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.assets.inner.innermost.TestInnerMostJoinedAsset");
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

        assertThat(x.containsKey(innerMostAsset.getName()), Matchers.is(true));
        assertThat(x.containsKey(innerMostJoinedAsset.getName()), Matchers.is(true));
        assertThat(x.containsKey(innerAsset.getName()), Matchers.is(true));
    }

}
