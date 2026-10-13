package com.freshworks.hagrid.shared.infra.mongo;

import static org.mockito.Mockito.doAnswer;

import org.bson.Document;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.MockFacadeInterface;
import com.freshworks.hagrid.ReturnableMockTypeList;
import com.freshworks.hagrid.main.assets.GenericAsset;
import com.freshworks.hagrid.processor.AbstractAsset;
import com.mongodb.client.MongoCursor;

/**
 * Mongo twin of MockFacadeNitriteDbCursor.
 * Difference: Nitrite's facade returns a List<String> from getNextAsset(), which cannot satisfy the method's
 * return type (an AbstractAsset) and would fail with a ClassCastException if ever called. Here getNextAsset()
 * returns a GenericAsset.
 */
@Component
public class MockFacadeMongoDbCursor implements MockFacadeInterface {

    ReturnableMockTypeList<Boolean> hasMore = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Long> docSize = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<AbstractAsset> getNext = new ReturnableMockTypeList<>();

    MongoCursor<Document> mongoCursor;

    @Override
    @SuppressWarnings("unchecked")
    public MockFacadeMongoDbCursor configure() throws Exception{

        reset();

        hasMore.add(false);
        docSize.add(10L);
        getNext.add(new GenericAsset());
        mongoCursor = Mockito.mock(MongoCursor.class);
        return this;
    }

    public MockFacadeMongoDbCursor hasMore(Boolean... hasMore){
        this.hasMore.clear();
        this.hasMore.add(hasMore);
        return this;
    }

    public MockFacadeMongoDbCursor docSize(Long... docSize){
        this.docSize.clear();
        this.docSize.add(docSize);
        return this;
    }

    public MockFacadeMongoDbCursor getNext(AbstractAsset... assets){
        this.getNext.clear();
        this.getNext.add(assets);
        return this;
    }

    public MockFacadeMongoDbCursor getMongoCursor(MongoCursor<Document> mongoCursor){
        this.mongoCursor = mongoCursor;
        return this;
    }

    @Override
    public MongoDbCursor build() throws Exception {

        MongoDbCursor mongoDbCursor = new MongoDbCursor(mongoCursor, 0L);
        mongoDbCursor = Mockito.spy(mongoDbCursor);
        doAnswer(hasMore.answer()).when(mongoDbCursor).hasNext();
        doAnswer(getNext.answer()).when(mongoDbCursor).getNextAsset();
        doAnswer(docSize.answer()).when(mongoDbCursor).docSize();
        return mongoDbCursor;
    }
}
