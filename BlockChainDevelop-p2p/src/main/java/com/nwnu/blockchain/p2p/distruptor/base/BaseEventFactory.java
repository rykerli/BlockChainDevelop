package com.nwnu.blockchain.p2p.distruptor.base;

import com.alipay.disruptor.EventFactory;

/**
 * BaseEventFactory
 * <pre>
 *  Version         Date            Author          Description
 * ------------------------------------------------------------
 *  1.0.0           2019/11/18     red        -
 * </pre>
 *
 * @author red
 * @version 1.0.0 2019/11/18 11:06 AM
 * @since 1.0.0
 */
public class BaseEventFactory implements EventFactory<BaseEvent> {
	@Override
	public BaseEvent newInstance() {
		return new BaseEvent();
	}
}
