/**
* Copyright  2017
* 
* All  right  reserved.
*
* Created  on  2017年6月4日 上午9:25:42
*
* @Package com.happygo.dlc.biz  
* @Title: DlcLogQueryServiceImpl.java
* @Description: DlcLogQueryServiceImpl.java
* @author sxp (1378127237@qq.com) 
* @version 1.0.0 
*/
package com.happygo.dlc.biz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.ignite.Ignite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.happgo.dlc.base.DlcConstants;
import com.happgo.dlc.base.DlcLogIgniteCache;
import com.happgo.dlc.base.bean.DlcLog;
import com.happgo.dlc.base.bean.PageParam;
import com.happgo.dlc.base.util.CollectionUtils;
import com.happygo.dlc.biz.config.SystemConfig;
import com.happygo.dlc.biz.service.DlcLogQueryService;
import com.happygo.dlc.dal.access.DlcLogQueryCallback;

/**
 * ClassName:DlcLogQueryServiceImpl
 * @Description: DlcLogQueryServiceImpl.java
 * @author sxp (1378127237@qq.com) 
 * @date:2017年6月4日 上午9:25:42
 */
@Service
public class DlcLogQueryServiceImpl implements DlcLogQueryService {
	
	/**
	 * Logger the LOGGER 
	 */
	private static final Logger LOGGER = LogManager.getLogger(DlcLogQueryServiceImpl.class);
	
	/**
	 * Ignite the ignite 
	 */
	@Autowired
	private Ignite ignite;
	
	/**
	 * SystemConfig the systemConfig 
	 */
	@Autowired
	private SystemConfig systemConfig;
	
	/**
	 * DlcLogIgniteCache<String,List<DlcLog>> the igniteCache 
	 */
	private DlcLogIgniteCache<String, List<List<DlcLog>>> igniteCache;

	@PostConstruct
	public void initIgniteCache() {
		int duration = systemConfig.getCacheDuration();
		igniteCache = new DlcLogIgniteCache<>(ignite, "dlcLogCache", duration);
	}
	
	/* (non-Javadoc)
	 * @see com.happygo.dlc.biz.service.DlcLogQueryService
	 * @see #logQuery(java.lang.String, com.happgo.dlc.base.bean.PageParam)
	 */
	public List<List<DlcLog>> logQuery(String keyWord, PageParam pageParam) {
		//1.根据key在IgniteCache查询是否有缓存，如果有直接返回，否则继续第二步
		List<List<DlcLog>> splitLogQueryDlcLogs = igniteCache.get(keyWord);
		if (splitLogQueryDlcLogs != null && !splitLogQueryDlcLogs.isEmpty()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("get keyword[" + keyWord + "] dlc log from ignite cache");
			}
			return splitLogQueryDlcLogs;
		}
		
		int partitionSize = pageParam.getNumPerPage();
		
		//2.根据keyword关键字匹配，如果没有匹配到，继续第三步
		List<DlcLog> logQueryDlcLogs = broadcastLogQuery(keyWord, null);
		if (!logQueryDlcLogs.isEmpty()) {
			splitLogQueryDlcLogs = CollectionUtils.split(logQueryDlcLogs, partitionSize);
			igniteCache.put(keyWord, splitLogQueryDlcLogs);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("keyword[" + keyWord + "] dlc log put ignite cache");
			}
			return splitLogQueryDlcLogs;
		}
		
		//3.根据keyword进行相似度查询
		logQueryDlcLogs = broadcastLogQuery(keyWord, DlcConstants.DLC_MORE_LIKE_THIS_QUERY_MODE);
		if (!logQueryDlcLogs.isEmpty()) {
			splitLogQueryDlcLogs = CollectionUtils.split(logQueryDlcLogs, partitionSize);
			igniteCache.put(keyWord, splitLogQueryDlcLogs);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("keyword[" + keyWord + "] dlc log put ignite cache");
			}
		}
		return splitLogQueryDlcLogs;
	}
	
	/**
	* @MethodName: broadcastLogQuery
	* @Description: the broadcastLogQuery
	* @param keyWord
	* @param queryMode
	* @return List<DlcLog>
	*/
	private List<DlcLog> broadcastLogQuery(String keyWord, String queryMode) {
		Collection<List<DlcLog>> logQueryResults = ignite.compute().broadcast(
				new DlcLogQueryCallback(keyWord, queryMode));
		if (logQueryResults == null) {
			return null;
		}
		List<DlcLog> logQueryDlcLogs = new ArrayList<DlcLog>();
		for (Iterator<List<DlcLog>> it = logQueryResults.iterator(); it
				.hasNext();) {
			logQueryDlcLogs.addAll(it.next());
		}
		return logQueryDlcLogs;
	}
}
