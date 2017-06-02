/**
 * Copyright  2017
 * 
 * All  right  reserved.
 *
 * Created  on  2017年6月1日 下午3:16:20
 *
 * @Package com.happygo.dlc.ignite  
 * @Title: DlcIgniteServiceImpl.java
 * @Description: DlcIgniteServiceImpl.java
 * @author sxp (1378127237@qq.com) 
 * @version 1.0.0 
 */
package com.happygo.dlc.ignite.service;

import java.util.List;

import org.apache.ignite.Ignite;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.happgo.dlc.base.DlcLog;
import com.happygo.dlc.ignite.task.DlcIgniteTask;

/**
 * ClassName:DlcIgniteServiceImpl
 * 
 * @Description: DlcIgniteServiceImpl.java
 * @author sxp (1378127237@qq.com)
 * @date:2017年6月1日 下午3:16:20
 */
public class DlcIgniteServiceImpl implements DlcIgniteService, Service {

	/** 
	* The field serialVersionUID
	*/
	private static final long serialVersionUID = -2857433909801286792L;
	
	/** 
	* The field LOGEER
	*/
	private static final Logger LOGEER = LogManager.getLogger(DlcIgniteServiceImpl.class);
	
	/** 
	* The field ignite
	*/
	@IgniteInstanceResource
	private Ignite ignite;

	/**
	 * logQuery
	 * @param keyWord
	 * @see com.happygo.dlc.ignite.service.DlcIgniteService#logQuery(java.lang.String) 
	 */
	public List<DlcLog> logQuery(String keyWord) {
		return ignite.compute().execute(DlcIgniteTask.class, keyWord);
	}

	/**
	* cancel
	* @param ctx 
	* @see org.apache.ignite.services.Service#cancel(org.apache.ignite.services.ServiceContext) 
	*/
	public void cancel(ServiceContext ctx) {
		LOGEER.info("Service was cancel: " + ctx.name());
	}

	/**
	* init
	* @param ctx
	* @throws Exception 
	* @see org.apache.ignite.services.Service#init(org.apache.ignite.services.ServiceContext) 
	*/
	public void init(ServiceContext ctx) throws Exception {
		LOGEER.info("Service was initialized: " + ctx.name());
	}

	/**
	* execute
	* @param ctx
	* @throws Exception 
	* @see org.apache.ignite.services.Service#execute(org.apache.ignite.services.ServiceContext) 
	*/
	public void execute(ServiceContext ctx) throws Exception {
		LOGEER.info("Executing distributed service: " + ctx.name());
	}
}