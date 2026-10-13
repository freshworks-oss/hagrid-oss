package com.freshworks.hagrid.data.unit.dag.assets;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.processor.AbstractAsset;

@Profile("unit")
@Component("unit_dag_asset_Application")
public class Application extends AbstractAsset {

    String id;

    public void setFromBean(com.freshworks.hagrid.data.unit.dag.beans.Application application){
        this.id = application.getClazz();
    }

    @Override
    public void transform() {

    }

}
