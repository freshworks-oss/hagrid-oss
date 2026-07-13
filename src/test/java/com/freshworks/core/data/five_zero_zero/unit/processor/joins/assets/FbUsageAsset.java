package com.freshworks.core.data.five_zero_zero.unit.processor.joins.assets;

import org.springframework.stereotype.Component;

import com.freshworks.core.data.five_zero_zero.unit.processor.joins.beans.FbUsageBean;
import com.freshworks.core.data.five_zero_zero.unit.processor.joins.beans.FbUserBean;
import com.freshworks.core.processor.AbstractAsset;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class FbUsageAsset extends AbstractAsset{

    String userId;
    String createdAt;

    public void setFromAsset(FbUsageBean fbUsageBean){
        this.userId = fbUsageBean.getUserId();
        this.createdAt = fbUsageBean.getCreatedAt();
    }

    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }
    
}
