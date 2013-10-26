package com.belo82.facetsearch;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class PrefixSearchShouldTest {
    private Indexer indexer;

    @Before
    public void init() throws IOException, ParseException {
        indexer = new Indexer();
        indexer.createIndex(Main.readData());
    }

    @Test
    public void return2documentsWhenSearchForCode_2c() throws IOException, ParseException {
        Assert.assertEquals(1, indexer.doPrefixSearch("6C").size());
    }

    @Test
    public void beCaseInsensitive() throws IOException, ParseException {
        Assert.assertEquals(1, indexer.doPrefixSearch("6C").size());
        Assert.assertEquals(1, indexer.doPrefixSearch("6c").size());
    }

    @Test
    public void findAlsoNonAlphanumericChars() throws IOException, ParseException {
        Assert.assertEquals(1, indexer.doPrefixSearch("006/54").size());
    }

    @Test
    public void matchAlsoWholeContentOfTheField() throws IOException, ParseException {
        Assert.assertEquals(2, indexer.doPrefixSearch("6Wab5/8c/2013").size());
    }
}
