package com.freshworks.core.processor;

import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.reflections.scanners.Scanners.SubTypes;

@Component
public class AssetAssetDependencyService {

    @Autowired
    List<AbstractAsset> assetList;

    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    ImmutableListMultimap<String, String> assetAssetDependencyMapping;
    SyncServiceContainer syncServiceContainer;
    AnalyticsFactory analyticsFactory;
    AnalyticsService analyticsService;

    public void configure(SyncServiceContainer syncServiceContainer) {
        this.syncServiceContainer = syncServiceContainer;
        this.analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        NamespaceService namespaceService = syncServiceContainer.getBean(NamespaceService.class);
        this.analyticsService = this.analyticsFactory.getAnalyticsService(namespaceService.getNamespace());
    }

    public ImmutableListMultimap<String, String> scanner(String namespace, ProcessorConfigService processorConfigService) throws Exception {

        try{
            lock.writeLock().lock();

            if(Boolean.FALSE.equals(assetAssetDependencyMapping.isEmpty())){
                return assetAssetDependencyMapping;
            }

            Multimap<String, String> connectorConfigItemTable = ArrayListMultimap.create();

            Set<Class<?>> assetSet = new HashSet<>();

            for(AbstractAsset asset : assetList){
                assetSet.add(asset.getClass());                
            }

            for (Class<?> assetClass : assetSet) {

                FreshJoin freshJoin = assetClass.getAnnotation(FreshJoin.class);

                // Only consider those assets where freshJoin is mentioned
                if(freshJoin == null){
                    continue;
                }

                List<String> dependentClassList = findDependencyOfAsset(ProcessorUtility.getAllSetters(assetClass));
                for (String dependent :
                        dependentClassList) {
                    connectorConfigItemTable.put(assetClass.getName(), dependent);
                }
            }
            assetAssetDependencyMapping = ImmutableListMultimap.copyOf(connectorConfigItemTable);
            return assetAssetDependencyMapping;
        }

        finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> findDependencyOfAsset(List<Method> setterMethodList){

        ArrayList<String> dependents = new ArrayList<>();

        ArrayList<Class<?>> arrayList = new ArrayList<>();

        for(Method setter: setterMethodList){
            ArrayList<Class<?>> y = new ArrayList(Arrays.asList(setter.getParameterTypes()));
            arrayList.addAll(y);
        }
        HashSet<Class<?>> u =  new HashSet<>(arrayList);

        Iterator<Class<?>> it  = u.iterator();

        while(it.hasNext()){
            Class<?> c = it.next();
            String x = c.getName();

            // Here do not add any dependency of the assets which is of primitive type like setName(String s), setNumber(ArrayList x)
            if(AbstractAsset.class.isAssignableFrom(c)){
                dependents.add(x);
            }
        }
        return dependents;
    }
}
