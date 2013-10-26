package com.belo82.facetsearch;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class AddressSearchShouldTest {
    private Indexer indexer;

    @Before
    public void init() throws IOException, ParseException {
        indexer = new Indexer();
        indexer.createIndex(Main.readData());
    }

    @Test
    public void matchTheDocumentWhenSearchingWholeAddressField() throws IOException, ParseException {
        Assert.assertEquals(1, indexer.doFacetLabelSearch("57-59, Čušiľovaná Green Lane, Fulham, London, SW6 4JA").size());
    }

    @Test
    public void notMatchAnyDocumentWhenNotSearchingForWholeAddressField() throws IOException, ParseException {
        Assert.assertEquals(0, indexer.doFacetLabelSearch("57-59").size());
    }

    @Test
    public void notMatchTheDocumentIfUpperOrLowerCaseNotMatch() throws IOException, ParseException {
        Assert.assertEquals(0 , indexer.doFacetLabelSearch("57-59, Čušiľovaná green Lane, Fulham, London, SW6 4JA").size());
    }
}
