/**
 * Copyright  2017
 * 
 * All  right  reserved.
 *
 * Created  on  2017年6月1日 下午4:12:19
 *
 * @Package com.happygo.dlc.ignite.task  
 * @Title: DlcIgniteTask.java
 * @Description: DlcIgniteTask.java
 * @author sxp (1378127237@qq.com) 
 * @version 1.0.0 
 */
package com.happygo.dlc.ignite.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ignite.IgniteException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;

import com.happgo.dlc.base.DlcConstants;
import com.happgo.dlc.base.DlcLog;
import com.happygo.dlc.log.LuceneAppender;
import com.happygo.dlc.lucene.LuceneIndexSearcher;
import com.happygo.dlc.lucene.LuceneIndexWriter;

/**
 * ClassName:DlcIgniteTask
 * 
 * @Description: DlcIgniteTask.java
 * @author sxp (1378127237@qq.com)
 * @date:2017年6月1日 下午4:12:19
 */
public class DlcIgniteTask extends ComputeTaskAdapter<String, List<DlcLog>>{

	/** 
	* The field serialVersionUID
	*/
	private static final long serialVersionUID = 2812385577387815111L;
	
	/** 
	* The field LOGEER
	*/
	private static final Logger LOGEER = LogManager.getLogger(DlcIgniteTask.class);

	/**
	* map
	* @param subgrid
	* @param arg
	* @throws IgniteException 
	* @see org.apache.ignite.compute.ComputeTask#map(java.util.List, java.lang.Object) 
	*/
	public Map<? extends ComputeJob, ClusterNode> map(
			List<ClusterNode> subgrid, final String keyWord)
			throws IgniteException {
		Map<ComputeJob, ClusterNode> map = new HashMap<>();
		Iterator<ClusterNode> it = subgrid.iterator();
		Map<String, LuceneIndexWriter> writeMap = LuceneAppender.writerMap;
		for (Map.Entry<String, LuceneIndexWriter> entry : writeMap.entrySet()) {
			if (!it.hasNext()) {
				it = subgrid.iterator();
			}
			final String targetPath = entry.getKey();
			ClusterNode node = it.next();
			map.put(new ComputeJobAdapter() {
				/**
				 * The field serialVersionUID
				 */
				private static final long serialVersionUID = 8846404467071310921L;

				@Override
				public Object execute() {
					LOGEER.info(">>> Search keyWord '" + keyWord
							+ "' on this node from target path '" + targetPath
							+ "'");
					KeywordAnalyzer analyzer = new KeywordAnalyzer();
					LuceneIndexSearcher indexSearcher = LuceneIndexSearcher
							.indexSearcher(targetPath, analyzer);
					ScoreDoc[] scoreDocs = indexSearcher.fuzzySearch(DlcConstants.DLC_CONTENT,
							keyWord, DlcConstants.DLC_HIGHLIGHT_PRE_TAG,
							DlcConstants.DLC_HIGHLIGHT_POST_TAG,
							DlcConstants.DLC_FRAGMENT_SIZE);
					return buildDlcLogs(scoreDocs, indexSearcher, analyzer);
				}
			}, node);
		}
		return map;
	}

	/**
	* reduce
	* @param results
	* @throws IgniteException 
	* @see org.apache.ignite.compute.ComputeTask#reduce(java.util.List) 
	*/
	public List<DlcLog> reduce(List<ComputeJobResult> results)
			throws IgniteException {
		List<DlcLog> dlcLogs = new ArrayList<DlcLog>();
		for (final ComputeJobResult res : results) {
			dlcLogs.addAll(res.<List<DlcLog>> getData());
		}
		return dlcLogs;
	}

	/**
	 * @MethodName: buildDlcLogs
	 * @Description: the buildDlcLogs
	 * @param scoreDocs
	 * @param iSearcher
	 * @param analyzer
	 * @return List<DlcLog>
	 */
	public static List<DlcLog> buildDlcLogs(ScoreDoc[] scoreDocs,
			LuceneIndexSearcher iSearcher, Analyzer analyzer) {
		List<DlcLog> dlcLogs = new ArrayList<>();
		DlcLog dlcLog = null;
		Document doc = null;
		for (final ScoreDoc scoreDoc : scoreDocs) {
			doc = iSearcher.hitDocument(scoreDoc);
			String content = iSearcher.luceneHighlighter.getBestFragment(
					analyzer, doc, DlcConstants.DLC_CONTENT);
			String level = doc.get(DlcConstants.DLC_LEVEL);
			long time = (Long) doc.getField(DlcConstants.DLC_TIME)
					.numericValue();
			String hostIp = doc.get(DlcConstants.DLC_HOST_IP);
			dlcLog = new DlcLog(content, level, time, hostIp);
			dlcLogs.add(dlcLog);
		}
		return dlcLogs;
	}
}