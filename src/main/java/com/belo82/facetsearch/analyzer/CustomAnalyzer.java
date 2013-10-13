package com.belo82.facetsearch.analyzer;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.Reader;

/**
 * Custom analyzer based on <code>{@link org.apache.lucene.analysis.cz.CzechAnalyzer} but <strong>excluding stemming process</strong></code>.<br/>
 *
 * <br/>
 * <strong>JavaDoc copied from CzechAnalyzer:</strong><br/>
 * {@link org.apache.lucene.analysis.Analyzer} for Czech language.
 * <p>
 * Supports an external list of stopwords (words that will not be indexed at
 * all). A default set of stopwords is used unless an alternative list is
 * specified.
 * </p>
 * <p/>
 * <a name="version"/>
 * <p/>
 * You must specify the required {@link org.apache.lucene.util.Version} compatibility when creating
 * CzechAnalyzer:
 * <ul>
 * <li>Edit: In <code>CustomAnalyzer</code> words are not stemmed at all. <strike>As of 3.1, words are stemmed with {@link org.apache.lucene.analysis.cz.CzechStemFilter}</strike>
 * <li>As of 2.9, StopFilter preserves position increments
 * <li>As of 2.4, Tokens incorrectly identified as acronyms are corrected (see
 * <a href="https://issues.apache.org/jira/browse/LUCENE-1068">LUCENE-1068</a>)
 * </ul>
 */
public final class CustomAnalyzer extends StopwordAnalyzerBase {
    /**
     * File containing default Slovak stopwords.
     */
    public final static String DEFAULT_STOPWORD_FILE = "stop-words-slovak.txt";

    /**
     * Returns a set of default Slovak-stopwords
     *
     * @return a set of default Slovak-stopwords
     */
    public static final CharArraySet getDefaultStopSet() {
        return DefaultSetHolder.DEFAULT_SET;
    }

    private static class DefaultSetHolder {
        private static final CharArraySet DEFAULT_SET;

        static {
            try {
                DEFAULT_SET = WordlistLoader.getWordSet(IOUtils.getDecodingReader(CustomAnalyzer.class,
                        DEFAULT_STOPWORD_FILE, IOUtils.CHARSET_UTF_8), "#", Version.LUCENE_42);
            } catch (IOException ex) {
                // default set should always be present as it is part of the
                // distribution (JAR)
                throw new RuntimeException("Unable to load default stopword set");
            }
        }
    }


    private final CharArraySet stemExclusionTable;

    /**
     * Builds an analyzer with the default stop words ({@link #getDefaultStopSet()}).
     *
     * @param matchVersion Lucene version to match See
     *                     {@link <a href="#version">above</a>}
     */
    public CustomAnalyzer(Version matchVersion) {
        this(matchVersion, DefaultSetHolder.DEFAULT_SET);
    }

    /**
     * Builds an analyzer with the given stop words.
     *
     * @param matchVersion Lucene version to match See
     *                     {@link <a href="#version">above</a>}
     * @param stopwords    a stopword set
     */
    public CustomAnalyzer(Version matchVersion, CharArraySet stopwords) {
        this(matchVersion, stopwords, CharArraySet.EMPTY_SET);
    }

    /**
     * Builds an analyzer with the given stop words and a set of work to be
     * excluded from the {@link org.apache.lucene.analysis.cz.CzechStemFilter}.
     *
     * @param matchVersion       Lucene version to match See
     *                           {@link <a href="#version">above</a>}
     * @param stopwords          a stopword set
     * @param stemExclusionTable a stemming exclusion set
     */
    public CustomAnalyzer(Version matchVersion, CharArraySet stopwords, CharArraySet stemExclusionTable) {
        super(matchVersion, stopwords);
        this.stemExclusionTable = CharArraySet.unmodifiableSet(CharArraySet.copy(matchVersion, stemExclusionTable));
    }

    /**
     * Creates
     * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
     * used to tokenize all the text in the provided {@link java.io.Reader}.
     *
     * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
     *         built from a {@link org.apache.lucene.analysis.standard.StandardTokenizer} filtered with
     *         {@link org.apache.lucene.analysis.standard.StandardFilter}, {@link org.apache.lucene.analysis.core.LowerCaseFilter}, {@link org.apache.lucene.analysis.core.StopFilter}
     *         , and {@link org.apache.lucene.analysis.cz.CzechStemFilter} (only if version is >= LUCENE_31). If
     *         a version is >= LUCENE_31 and a stem exclusion set is provided via
     *         {@link #CustomAnalyzer(Version, CharArraySet, CharArraySet)} a
     *         {@link org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilter} is added before
     *         {@link org.apache.lucene.analysis.cz.CzechStemFilter}.
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName,
                                                     Reader reader) {
        final Tokenizer source = new StandardTokenizer(matchVersion, reader);
        TokenStream result = new StandardFilter(matchVersion, source);
        result = new LowerCaseFilter(matchVersion, result);
        result = new StopFilter(matchVersion, result, stopwords);
        if (matchVersion.onOrAfter(Version.LUCENE_31)) {
            if (!this.stemExclusionTable.isEmpty())
                result = new KeywordMarkerFilter(result, stemExclusionTable);

            // result = new CzechStemFilter(result);
        }
        return new TokenStreamComponents(source, result);
    }
}
