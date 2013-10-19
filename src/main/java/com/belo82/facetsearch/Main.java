package com.belo82.facetsearch;

import org.apache.lucene.queryparser.classic.ParseException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Hello world!
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);


    public static void main(String[] args) throws IOException, ParseException {
        logger.debug("Hello world!");

        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("com/belo82/facetsearch/data.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode data = mapper.readTree(inputStream);

        logger.debug("data: {}", data);

        Indexer indexer = new Indexer();
        indexer.createIndex(data);
    }
}
