package com.freshworks.core.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.freshworks.core.TestUtility;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;
import com.google.common.collect.ImmutableListMultimap;

import java.util.List;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestAssetAssetDependencyService {

    @Autowired
    MockFacadeAssetAssetDependencyService mockFacadeAssetAssetDependencyService;

    @Autowired
    MockFacadeProcessorConfigService mockFacadeProcessorConfigService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    String releaseVersion;

    Class<? extends AbstractAsset> innerAsset;
    Class<? extends AbstractAsset> innerMostAsset;
    Class<? extends AbstractAsset> outer;

    @BeforeEach
    public void beforeEach() throws Exception {

        releaseVersion = TestUtility.getReleaseVerion();
        mockFacadeAssetAssetDependencyService.configure().build();
        mockFacadeProcessorConfigService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();

        outer = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data.unit.dag.assets.complex_asset.Outer");
        innerAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data.unit.dag.assets.complex_asset.inner.Inner");
        innerMostAsset = (Class<? extends AbstractAsset>) Class.forName("com.freshworks.core.data.unit.dag.assets.complex_asset.inner.inner_most.InnerMost");
    }

    @Test
    public void testWhenAssetPathIsOuterThenAllInnerPackageAssetsAreAlsoScanned() throws Exception {

        ProcessorConfigService processorConfigService = mockFacadeProcessorConfigService
                .build();

        AssetAssetDependencyService assetAssetDependencyService = mockFacadeAssetAssetDependencyService
                .build();

        doCallRealMethod().when(assetAssetDependencyService).scanner(anyString(), any());
        doCallRealMethod().when(assetAssetDependencyService).findDependencyOfAsset(anyList());

        ImmutableListMultimap<String, String> x = assetAssetDependencyService.scanner("some-random-namespace", processorConfigService);

        assertThat(x.containsKey(innerMostAsset.getName()), Matchers.is(true));
        assertThat(x.containsKey(outer.getName()), Matchers.is(true));
        assertThat(x.containsKey(innerAsset.getName()), Matchers.is(true));
    }

    @Test
    public void testAssetDependencyCorrectlyIdentifyForDeepNonPrimitiveAssets() throws Exception{

        ProcessorConfigService processorConfigService = mockFacadeProcessorConfigService
                .build();

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .build();

        AssetAssetDependencyService assetAssetDependencyService = mockFacadeAssetAssetDependencyService
                .build();

        doCallRealMethod().when(assetAssetDependencyService).scanner(anyString(), any());
        doCallRealMethod().when(assetAssetDependencyService).findDependencyOfAsset(anyList());

        ImmutableListMultimap<String, String> x = assetAssetDependencyService.scanner("some-random-namespace", processorConfigService);

        ImmutableList<String> dependencyList = x.get(outer.getName());
        assertThat(dependencyList, Matchers.hasItem(Matchers.containsString("unit.dag.assets.Application")));
        assertThat(dependencyList, Matchers.hasItem(Matchers.containsString("unit.dag.assets.Usage")));

    }
}
