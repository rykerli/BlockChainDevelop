package com.nwnu.blockchain.p2p.socket.handler.server;

import com.nwnu.blockchain.ApplicationContextProvider;
import com.nwnu.blockchain.p2p.socket.base.AbstractBlockHandler;
import com.nwnu.blockchain.p2p.socket.body.VoteBody;
import com.nwnu.blockchain.p2p.socket.pbft.queue.MsgQueueManager;
import com.nwnu.blockchain.p2p.socket.vote.BlockPacket;
import com.nwnu.blockchain.p2p.socket.vote.VoteMsg;
import lombok.extern.slf4j.Slf4j;
import org.tio.core.ChannelContext;

/**
 * pbft投票处理
 * <pre>
 *  Version         Date            Author          Description
 * ------------------------------------------------------------
 *  1.0.0           2019/11/18     red        -
 * </pre>
 *
 * @author red
 * @version 1.0.0 2019/11/18 2:35 PM
 * @since 1.0.0
 */
@Slf4j
public class PbftVoteHandler extends AbstractBlockHandler<VoteBody> {
	@Override
	public Class<VoteBody> bodyClass() {
		return VoteBody.class;
	}

	@Override
	public Object handler(BlockPacket packet, VoteBody voteBody, ChannelContext channelContext) {
		VoteMsg voteMsg = voteBody.getVoteMsg();
		log.info("收到来自于<" + voteMsg.getAppId() + "><投票>消息，投票信息为[" + voteMsg + "]");

		ApplicationContextProvider.getBean(MsgQueueManager.class).pushMsg(voteMsg);
		return null;
	}
}
