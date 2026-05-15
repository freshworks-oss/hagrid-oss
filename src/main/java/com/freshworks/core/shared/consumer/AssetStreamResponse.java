package com.freshworks.core.shared.consumer;

import com.freshworks.core.processor.AbstractAsset;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssetStreamResponse<T extends AbstractAsset> {

    private List<T> abstractAssetList;
    private Token nextToken;



    @Getter
    @Setter
    public static class Token{

        int start;
        int count;
    }
}
