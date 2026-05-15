package com.freshworks.core.processor;

import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.reflections.scanners.Scanners.SubTypes;

@Component
public class AssetBeanDependencyService {

    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    HashMap<String, ImmutableListMultimap<String, String>> assetBeanDependencyMapByBeanAssetLocation = new HashMap<>();
    AnalyticsFactory analyticsFactory;

    public AssetBeanDependencyService(AnalyticsFactory analyticsFactory) {
        this.analyticsFactory = analyticsFactory;
    }

    public ImmutableListMultimap<String, String> scanner(String namespace, ProcessorConfigService processorConfigService) throws IOException {

        try{
            lock.writeLock().lock();
            String assetBeanLocation = processorConfigService.getBeanLocation() + "_" + processorConfigService.getAssetLocation();
            if(assetBeanDependencyMapByBeanAssetLocation.containsKey(assetBeanLocation)){
                return assetBeanDependencyMapByBeanAssetLocation.get(assetBeanLocation);
            }

            Multimap<String, String> connectorConfigItemTable = ArrayListMultimap.create();
            String assetPath = processorConfigService.getAssetLocation();
            String beanPath = processorConfigService.getBeanLocation();

//            Creation of the config item table for the given config item and connector.
//            As of now we have just Software class

            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage(assetPath)));


            Set<Class<?>> assets = reflections.get(SubTypes.of(AbstractAsset.class)
                    .asClass());

            for (Class<?> asset : assets) {
                List<String> dependentClassList = findDependencyOfAsset(ProcessorUtility.getAllSetters(asset), beanPath);
                for (String dependent :
                        dependentClassList) {
                    connectorConfigItemTable.put(asset.getName(), dependent);
                }
            }
            ImmutableListMultimap<String, String> s = ImmutableListMultimap.copyOf(connectorConfigItemTable);
            assetBeanDependencyMapByBeanAssetLocation.put(assetBeanLocation, s);
            return s;
        }

        finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> findDependencyOfAsset(List<Method> setterMethodList, String beanPath){

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
            if(x.contains(beanPath)){
                dependents.add(x);
            }
        }
        return dependents;
    }
}
