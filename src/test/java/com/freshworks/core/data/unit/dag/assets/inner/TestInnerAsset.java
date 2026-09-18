package com.freshworks.core.data.unit.dag.assets.inner;

import com.freshworks.core.data.unit.dag.beans.Application;
import com.freshworks.core.processor.AbstractAsset;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("unit")
@Component("unit_dag_asset_TestInnerAsset")
public class TestInnerAsset extends AbstractAsset {


    public void setFromBean(Application application){

    }


    @Override
    public void transform() {

    }

}
