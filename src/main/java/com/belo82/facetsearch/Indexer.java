package com.belo82.facetsearch;

import com.belo82.facetsearch.analyzer.CustomAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;

/**
 * @author Peter Belko
 */
public class Indexer {
    private static final Logger logger = LoggerFactory.getLogger(Indexer.class);

    private IndexWriter iWriter;
    private Directory index;

    private DirectoryReader iReader;
    private IndexSearcher iSearcher;

    public Indexer() {
        init();
    }

    private void init() {
        index = new RAMDirectory();

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, new CustomAnalyzer(Version.LUCENE_42));
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        try {
            iWriter = new IndexWriter(index, config);
        } catch (IOException e) {
            throw new RuntimeException("cannot create an index writer.", e);
        }
    }

    public void createIndex(JsonNode data) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("contend", "lorem ipsum dolor sit amet", Field.Store.YES));
        iWriter.addDocument(doc);

        TaxonomyWriter taxo = new DirectoryTaxonomyWriter(index, IndexWriterConfig.OpenMode.CREATE);
        DocumentBuilder categoryDocBuilder = new CategoryDocumentBuilder(taxo);

        // TODO: store test data

        TopDocs topDocs = openSearcher().search(new MatchAllDocsQuery(), 2);
        logger.debug("no of all documents: {}", topDocs.totalHits);
    }

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
