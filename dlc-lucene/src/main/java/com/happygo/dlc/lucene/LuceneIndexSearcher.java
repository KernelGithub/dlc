/**
 * Copyright  2017
 * 
 * All  right  reserved.
 *
 * Created  on  2017年5月29日 下午6:33:43
 *
 * @Package com.happygo.dlc.lucene  
 * @Title: LuceneIndexSearcher.java
 * @Description: LuceneIndexSearcher.java
 * @author sxp (1378127237@qq.com) 
 * @version 1.0.0 
 */
package com.happygo.dlc.lucene;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.happgo.dlc.base.Assert;
import com.happgo.dlc.base.DLCException;

/**
 * ClassName:LuceneIndexSearcher
 * 
 * @Description: LuceneIndexSearcher.java
 * @author sxp (1378127237@qq.com)
 * @date:2017年5月29日 下午6:33:43
 */
public final class LuceneIndexSearcher {

	/**
	 * LuceneHighlighter the luceneHighlighter
	 */
	public LuceneHighlighter luceneHighlighter;

	/**
	 * Analyzer the analyzer
	 */
	public Analyzer analyzer;

	/**
	 * IndexSearcher the indexSearcher
	 */
	private IndexSearcher indexSearcher;

	/**
	 * Directory the directory
	 */
	private Directory directory;

	/**
	 * Constructor com.happygo.dlc.lucene.LuceneIndexSearcher
	 * 
	 * @param dirPath
	 * @param analyzer
	 */
	private LuceneIndexSearcher(String dirPath, Analyzer analyzer) {
		Assert.isNull(dirPath);

		try {
			directory = FSDirectory.open(Paths.get(dirPath));
			DirectoryReader iDirectoryReader = DirectoryReader.open(directory);
			indexSearcher = new IndexSearcher(iDirectoryReader);
		} catch (IOException e) {
			throw new DLCException(e.getMessage(), e);
		}
		this.analyzer = analyzer;
	}

	/**
	 * @MethodName: indexSearcher
	 * @Description: the indexSearcher
	 * @param dirPath
	 * @param analyzer
	 * @return LuceneIndexSearcher
	 */
	public static LuceneIndexSearcher indexSearcher(String dirPath,
			Analyzer analyzer) {
		return new LuceneIndexSearcher(dirPath, analyzer);
	}

	/**
	 * @MethodName: fuzzySearch
	 * @Description: the fuzzySearch
	 * @param fieldName
	 * @param text
	 * @param preTag
	 * @param postTag
	 * @param fragmentSize
	 * @return ScoreDoc[]
	 */
	public ScoreDoc[] fuzzySearch(String fieldName, String text, String preTag,
			String postTag, int fragmentSize) {
		Term term = new Term(fieldName, text);
		Query query = new FuzzyQuery(term);
		luceneHighlighter = LuceneHighlighter.highlight(preTag, postTag, query,
				fragmentSize);
		ScoreDoc[] scoreDocs;
		try {
			scoreDocs = indexSearcher.search(query, 10000).scoreDocs;
		} catch (IOException e) {
			throw new DLCException(e.getMessage(), e);
		}
		return scoreDocs;
	}

	/**
	 * @MethodName: multiFieldSearch
	 * @Description: the multiFieldSearch
	 * @param queryStrs
	 * @param fields
	 * @param occurs
	 * @param preTag
	 * @param postTag
	 * @param fragmentSize
	 * @return ScoreDoc[]
	 */
	public ScoreDoc[] multiFieldSearch(String[] queryStrs, String[] fields,
			Occur[] occurs, String preTag, String postTag, int fragmentSize) {
		ScoreDoc[] scoreDocs;
		try {
			Query query = MultiFieldQueryParser.parse(queryStrs, fields,
					occurs, analyzer);
			luceneHighlighter = LuceneHighlighter.highlight(preTag, postTag,
					query, fragmentSize);
			scoreDocs = indexSearcher.search(query, 10000).scoreDocs;
		} catch (Exception e) {
			throw new DLCException(e.getMessage(), e);
		}
		return scoreDocs;
	}
	
	/**
	 * @MethodName: hitDocument
	 * @Description: the hitDocument
	 * @param scoreDoc
	 * @return Document
	 */
	public Document hitDocument(ScoreDoc scoreDoc) {
		try {
			return indexSearcher.doc(scoreDoc.doc);
		} catch (IOException e) {
			throw new DLCException(e.getMessage(), e);
		}
	}
}