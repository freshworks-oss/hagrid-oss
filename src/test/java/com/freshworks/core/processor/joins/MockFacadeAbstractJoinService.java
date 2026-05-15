package com.freshworks.core.processor.joins;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.AbstractBean;
import com.google.common.base.Optional;
import jakarta.persistence.Column;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Component
public class MockFacadeAbstractJoinService implements MockFacadeInterface {

    @SpyBean
    AbstractJoinService abstractJoinServiceSpy;

    ReturnableMockTypeList<String> getLookupFieldValueOfLeftClass;

    ReturnableMockTypeList<String> getLookupFieldValueOfRightClass;

    ReturnableMockTypeList<List<HashMap<String, AbstractBean>>> lookupStagingArea;

    ReturnableMockTypeList<Boolean> compare;

    ReturnableMockTypeList<Boolean> compareParent;

    ReturnableMockTypeList<Boolean> compareAttributes;

    ReturnableMockTypeList<List<Optional<AbstractAsset>>> getAssetWithFreshJoin;

    ReturnableMockTypeList<AbstractAsset> getAsset;

    ReturnableMockTypeList<Object> getLookupObject;

    ReturnableMockTypeList<Object> getLookupFieldValue;

    ReturnableMockTypeList<String> getLookupField;

    ReturnableMockTypeList<HashMap<String, AbstractBean>> unwrappedBeanToClassMap;


    @Override
    public MockFacadeAbstractJoinService configure(){
        reset();
        getLookupFieldValueOfLeftClass.add("");
        getLookupFieldValueOfRightClass.add("");
        lookupStagingArea.addNull();
        compare.add(false);
        compareParent.add(false);
        compareAttributes.add(false);
        getAssetWithFreshJoin.addNull();
        getAsset.addNull();
        getLookupObject.add("");
        getLookupFieldValue.add("");
        getLookupFieldValueOfLeftClass.add("");
        getLookupFieldValueOfRightClass.add("");
        getLookupField.add("");
        unwrappedBeanToClassMap.addNull();

        return this;
    }


    public MockFacadeAbstractJoinService getLookupFieldValueOfLeftClass(String... getLookupFieldValueOfLeftClass){
        this.getLookupFieldValueOfLeftClass.clear();;
        this.getLookupFieldValueOfLeftClass.add(getLookupFieldValueOfLeftClass);
        return this;
    }
    public MockFacadeAbstractJoinService getLookupFieldValueOfRightClass(String... getLookupFieldValueOfRightClass) {
        this.getLookupFieldValueOfRightClass.clear();
        this.getLookupFieldValueOfRightClass.add(getLookupFieldValueOfRightClass);
        return this;
    }


    public MockFacadeAbstractJoinService lookupStagingArea(List<HashMap<String, AbstractBean>> lookupStagingArea){
        this.lookupStagingArea.clear();
        this.lookupStagingArea.add(lookupStagingArea);
        return this;
    }

    public MockFacadeAbstractJoinService compare(Boolean... compare){
        this.compare.clear();
        this.compare.add(compare);
        this.compare.add(false);
        return this;
    }

    public MockFacadeAbstractJoinService compareParent(Boolean... compareParent){
        this.compareParent.clear();
        this.compareParent.add(compareParent);
        return this;
    }

    public MockFacadeAbstractJoinService compareAttributes(Boolean... compareAttributes){
        this.compareAttributes.clear();
        this.compareAttributes.add(compareAttributes);
        return this;
    }

    public MockFacadeAbstractJoinService getAssetWithFreshJoin(List<Optional<AbstractAsset>>... getAssetWithFreshJoin){
        this.getAssetWithFreshJoin.clear();
        this.getAssetWithFreshJoin.add(getAssetWithFreshJoin);
        return this;
    }

    public MockFacadeAbstractJoinService getAsset(AbstractAsset... asset){
        this.getAsset.clear();
        this.getAsset.add(asset);
        return this;
    }

    public MockFacadeAbstractJoinService getLookupObject(Object... getLookupObject) {
        this.getLookupObject.clear();
        this.getLookupObject.add(getLookupObject);
        return this;
    }


    public MockFacadeAbstractJoinService getLookupFieldValue(Object... getLookupFieldValue) {
        this.getLookupFieldValue.clear();
        this.getLookupFieldValue.add(getLookupFieldValue);
        return this;
    }

    public MockFacadeAbstractJoinService getLookupField(String... getLookupField) {
        this.getLookupField.clear();
        this.getLookupField.add(getLookupField);
        return this;
    }


    public MockFacadeAbstractJoinService unwrappedBeanToClassMap(HashMap<String, AbstractBean>... unwrappedBeanToClassMap) {
        this.unwrappedBeanToClassMap.clear();
        this.unwrappedBeanToClassMap.add(unwrappedBeanToClassMap);
        return this;
    }


    @Override
    public AbstractJoinService build() throws Exception {

        doNothing().when(abstractJoinServiceSpy).configure(any());
        doAnswer(getLookupFieldValueOfLeftClass.answer()).when(abstractJoinServiceSpy).getLookupFieldValueOfLeftClass(any(), any());
        doAnswer(getLookupFieldValueOfRightClass.answer()).when(abstractJoinServiceSpy).getLookupFieldValueOfRightClass(any(), any());
        doAnswer(lookupStagingArea.answer()).when(abstractJoinServiceSpy).lookupStagingArea(any(), any(), any());
        doAnswer(compare.answer()).when(abstractJoinServiceSpy).compare(any(), any(), any());
        doAnswer(compareParent.answer()).when(abstractJoinServiceSpy).compareParent(any(), any());
        doAnswer(compareAttributes.answer()).when(abstractJoinServiceSpy).compareAttributes(any(), any(), any());
        doAnswer(getAssetWithFreshJoin.answer()).when(abstractJoinServiceSpy).getAssetWithFreshJoin(any(), any(), any(), any(), any());
        doAnswer(getAsset.answer()).when(abstractJoinServiceSpy).getAsset(any(), any(), any());
        doAnswer(getLookupObject.answer()).when(abstractJoinServiceSpy).getLookupObject(any(), any());
        doAnswer(getLookupFieldValue.answer()).when(abstractJoinServiceSpy).getLookupField(any(), any());
        doAnswer(getLookupField.answer()).when(abstractJoinServiceSpy).getLookupField(any(), any());
        doAnswer(unwrappedBeanToClassMap.answer()).when(abstractJoinServiceSpy).unwrappedBeanToClassMap(any());
        doNothing().when(abstractJoinServiceSpy).invokeSetterOnAssetObject(any(), any(), any());

        return abstractJoinServiceSpy;
    }
}
