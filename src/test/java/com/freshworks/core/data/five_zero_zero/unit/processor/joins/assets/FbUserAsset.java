package com.freshworks.core.data.five_zero_zero.unit.processor.joins.assets;

import org.springframework.stereotype.Component;

import com.freshworks.core.data.five_zero_zero.unit.processor.joins.beans.FbUserBean;
import com.freshworks.core.processor.AbstractAsset;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Component
public class FbUserAsset extends AbstractAsset{

    String userId;
    String userName;

    public void setFromFbUserBean(FbUserBean fbUserBean){
        this.userId = fbUserBean.getId();
        this.userName = fbUserBean.getFirstName() + " " + fbUserBean.getLastName();
    }


    @Override
    public void transform() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transform'");
    }
    
}
