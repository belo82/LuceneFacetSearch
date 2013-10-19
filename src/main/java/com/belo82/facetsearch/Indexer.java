package com.belo82.facetsearch;

import com.belo82.facetsearch.analyzer.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.index.FacetFields;
import org.apache.lucene.facet.params.FacetSearchParams;
import org.apache.lucene.facet.search.CountFacetRequest;
import org.apache.lucene.facet.search.FacetResult;
import org.apache.lucene.facet.search.FacetResultNode;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Peter Belko
 */
public class Indexer {
    private static final Logger logger = LoggerFactory.getLogger(Indexer.class);
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String OWNERS = "owners";
    public static final String FACET_SHOP_CATEGORY = "shop_category";
    public static final String SHOP_CATEGORIES = "shop_categories";
    public static final String AREA = "area";
    public static final String ADDRESS = "address";

    private IndexWriter iWriter;
    private Directory dir_taxo;

    public Indexer() {
        init();
    }

    private void init() {
        Directory index = new RAMDirectory();
        dir_taxo = new RAMDirectory();

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, new CustomAnalyzer(Version.LUCENE_42));
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        try {
            iWriter = new IndexWriter(index, config);
        } catch (IOException e) {
            throw new RuntimeException("cannot create an index writer.", e);
        }
    }

    /*
        {
            "id": 1,
            "name": "Budgens",
            "owners": ["Dylan James", "Kristin Sullivan"],
            "shop_category": "supermarket",
            "area": "Fulham",
            "address": "57-59, Parsons Green Lane, Fulham, London, SW6 4JA"
        }
     */
    public void createIndex(ArrayNode data) throws IOException, ParseException {
        TaxonomyWriter taxo = new DirectoryTaxonomyWriter(dir_taxo, IndexWriterConfig.OpenMode.CREATE);
        FacetFields facetFields = new FacetFields(taxo);

        List<CategoryPath> categories;

        for (Iterator<JsonNode> it = data.getElements(); it.hasNext();) {
            ObjectNode item = (ObjectNode) it.next();
            Document doc = new Document();

            doc.add(new IntField(ID, item.get(ID).getIntValue(), Field.Store.YES));
            doc.add(new TextField(NAME, item.get(NAME).getTextValue(), Field.Store.YES));
            doc.add(new TextField(SHOP_CATEGORIES, item.get(FACET_SHOP_CATEGORY).getTextValue(), Field.Store.YES));
            doc.add(new TextField(AREA, item.get(AREA).getTextValue(), Field.Store.YES));
            doc.add(new TextField(ADDRESS, item.get(ADDRESS).getTextValue(), Field.Store.YES));

            for (Iterator<JsonNode> it2 = item.get(OWNERS).getElements(); it2.hasNext();) {
                JsonNode ownerNode = it2.next();

                // logger.debug("owner: {}", ownerNode.getTextValue());
                doc.add(new TextField(OWNERS, ownerNode.getTextValue(), Field.Store.YES));
            }

            categories = new ArrayList<>();
            CategoryPath categoryPath = new CategoryPath(FACET_SHOP_CATEGORY, item.get(FACET_SHOP_CATEGORY).getTextValue());
            categories.add(categoryPath);
            taxo.addCategory(categoryPath);
            facetFields.addFields(doc, categories);

            iWriter.addDocument(doc);
        }

        taxo.commit();
    }

    public void doSearch(String query) throws IOException, ParseException {
        DirectoryReader iReader = DirectoryReader.open(iWriter, true);
        IndexSearcher iSearcher = new IndexSearcher(iReader);
        TaxonomyReader taxo = new DirectoryTaxonomyReader(dir_taxo);


        QueryParser queryParser = new QueryParser(Version.LUCENE_42, NAME, new StandardAnalyzer(Version.LUCENE_42));
        Query luceneQuery = queryParser.parse(query);

        // Collectors to get top results and facets
        TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create(10, true);

        FacetSearchParams facetSearchParams = new FacetSearchParams(new CountFacetRequest(new CategoryPath(FACET_SHOP_CATEGORY),10));

        FacetsCollector facetsCollector = FacetsCollector.create(facetSearchParams, iReader, taxo);
        iSearcher.search(luceneQuery, MultiCollector.wrap(topScoreDocCollector, facetsCollector));
        logger.debug("Found:");

        for(ScoreDoc scoreDoc: topScoreDocCollector.topDocs().scoreDocs) {
            Document document = iSearcher.doc(scoreDoc.doc);
            logger.debug("- shop: id: {}, name: {}, shop_category={}, area: {}, owners={}, score={}",
                    document.get(ID),
                    document.get(NAME),
                    document.get(SHOP_CATEGORIES),
                    document.get(AREA),
                    document.get(OWNERS),
                    scoreDoc.score);
        }

        logger.debug("Facets:");
        for(FacetResult facetResult : facetsCollector.getFacetResults()) {
            logger.debug("- " + facetResult.getFacetResultNode().label);
            for(FacetResultNode facetResultNode: facetResult.getFacetResultNode().subResults) {
                logger.debug("    - {} ({})", facetResultNode.label.toString(),
                        facetResultNode.value);
                for(FacetResultNode subFacetResultNode: facetResultNode.subResults) {
                    logger.debug("        - {} ({})", subFacetResultNode.label.toString(),
                            subFacetResultNode.value);
                }
            }
        }
    }
}
