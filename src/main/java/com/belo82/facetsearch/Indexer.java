package com.belo82.facetsearch;

import com.belo82.facetsearch.analyzer.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Belko
 */
public class Indexer {
    private static final Logger logger = LoggerFactory.getLogger(Indexer.class);

    private IndexWriter iWriter;
    private Directory index;

    private DirectoryReader iReader;
    private IndexSearcher iSearcher;
    private Directory dir_taxo;

    public Indexer() {
        init();
    }

    private void init() {
        index = new RAMDirectory();
        dir_taxo = new RAMDirectory();

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, new CustomAnalyzer(Version.LUCENE_42));
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        try {
            iWriter = new IndexWriter(index, config);
        } catch (IOException e) {
            throw new RuntimeException("cannot create an index writer.", e);
        }
    }

    public void createIndex(JsonNode data) throws IOException, ParseException {
        Document doc = new Document();

        TaxonomyWriter taxo = new DirectoryTaxonomyWriter(dir_taxo, IndexWriterConfig.OpenMode.CREATE);
        FacetFields ff = new FacetFields(taxo);
//        This was for version 4.0
//        DocumentBuilder categoryDocBuilder = new CategoryDocumentBuilder(taxo);

        List<CategoryPath> categories = new ArrayList<>();





        // TODO: store test data
        doc.add(new TextField("content", "lorem ipsum dolor sit amet", Field.Store.YES));
        doc.add(new TextField("authors", "Frano Kral", Field.Store.YES));

        CategoryPath categoryPath = new CategoryPath("author", "Frano Kral");
        categories.add(categoryPath);
        taxo.addCategory(categoryPath);


        ff.addFields(doc, categories);
        iWriter.addDocument(doc);
        taxo.commit();
        doSearch();
    }

    private void doSearch() throws IOException, ParseException {
        TaxonomyReader taxo = new DirectoryTaxonomyReader(dir_taxo);





        QueryParser queryParser = new QueryParser(Version.LUCENE_42, "content", new StandardAnalyzer(Version.LUCENE_42));
        Query luceneQuery = queryParser.parse("lorem");

        // Collectors to get top results and facets
        TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create(10, true);

        FacetSearchParams facetSearchParams = new FacetSearchParams(new CountFacetRequest(new CategoryPath("author"),10));

        openSearcher();
        FacetsCollector facetsCollector = FacetsCollector.create(facetSearchParams, iReader, taxo);
        iSearcher.search(luceneQuery, MultiCollector.wrap(topScoreDocCollector, facetsCollector));
        logger.debug("Found:");

        for(ScoreDoc scoreDoc: topScoreDocCollector.topDocs().scoreDocs) {
            Document document = openSearcher().doc(scoreDoc.doc);
            logger.debug("- book: content={}, authors={}, score={}",
                    document.get("content"),
                    document.get("authors"),
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
        }    }

    public IndexSearcher openSearcher() {
        try {
            iReader = DirectoryReader.open(iWriter, true);
            iSearcher = new IndexSearcher(iReader);
            return iSearcher;
        } catch (IOException e) {
            throw new RuntimeException("cannot create an index reader.", e);
        }
    }

}
