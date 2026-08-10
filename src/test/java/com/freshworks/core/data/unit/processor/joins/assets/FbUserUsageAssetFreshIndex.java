package com.freshworks.core.data.unit.processor.joins.assets;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.freshworks.core.data.unit.processor.joins.beans.FbUsageBean;
import com.freshworks.core.data.unit.processor.joins.beans.FbUserBean;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshIndex;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.processor.Annotations.FreshJoin.JOIN_TYPE;

import lombok.Getter;
import lombok.Setter;




@Getter
@Setter
@Component
@Profile("unit")
public class FbUserUsageAssetFreshIndex extends AbstractAsset{
    
    @FreshIndex
    String userId;

    String firstName;
    String lastName;
    String createdAt;

    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }
}
