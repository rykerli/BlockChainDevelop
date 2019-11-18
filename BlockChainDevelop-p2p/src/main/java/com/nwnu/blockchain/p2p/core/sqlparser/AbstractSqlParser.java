package com.nwnu.blockchain.p2p.core.sqlparser;


import com.nwnu.blockchain.p2p.core.model.base.BaseEntity;

/**
 * <pre>
 *  Version         Date            Author          Description
 * ------------------------------------------------------------
 *  1.0.0           2019/11/18     red        -
 * </pre>
 *
 * @author red
 * @version 1.0.0 2019/11/18 2:39 PM
 * @since 1.0.0
 */
public abstract class AbstractSqlParser<T extends BaseEntity> {
	/**
	 * 解析sql的方法
	 *
	 * @param operation 是什么操作
	 * @param id        主键
	 * @param entity    对象entity
	 */
	abstract void parse(byte operation, String id, T entity);

	/**
	 * 对象的类
	 *
	 * @return Class
	 */
	abstract Class getEntityClass();
}
