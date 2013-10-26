package com.belo82.facetsearch;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class RangeSearchShouldTest {
    private Indexer indexer;

    @Before
    public void init() throws IOException, ParseException {
        indexer = new Indexer();
        indexer.createIndex(Main.readData());
    }

    @Test
    public void return11documentsWhenSearch_From_01_01_1900() throws IOException, ParseException {
        Assert.assertEquals(11, indexer.doRangeSearch("01-01-1900", null).size());
    }

    @Test
    public void matchDocumentWhenFromAndToAreSameDates() throws IOException, ParseException {
        Assert.assertEquals(1, indexer.doRangeSearch("09-07-2005", "09-07-2005").size());
    }

    @Test
    public void matchAllDocumentsWhenFromIsNotSpecifiedAndToIsFarInTheFuture() throws IOException, ParseException {
        Assert.assertEquals(11, indexer.doRangeSearch(null, "09-07-2105").size());
    }
}
