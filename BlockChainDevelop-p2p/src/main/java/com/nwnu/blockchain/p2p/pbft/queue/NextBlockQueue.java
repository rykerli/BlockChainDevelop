package com.nwnu.blockchain.p2p.pbft.queue;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.nwnu.blockchain.core.body.BlockHash;
import com.nwnu.blockchain.core.body.RpcSimpleBlockBody;
import com.nwnu.blockchain.core.packet.BlockPacket;
import com.nwnu.blockchain.core.packet.PacketBuilder;
import com.nwnu.blockchain.core.packet.PacketType;
import com.nwnu.blockchain.p2p.pbft.client.ClientStarter;
import com.nwnu.blockchain.packet.PacketSender;
import com.nwnu.blockchain.repository.manager.DbBlockManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * NextBlockQueue
 * 处理请求next block时的返回
 * <pre>
 *  Version         Date            Author          Description
 * ------------------------------------------------------------
 *  1.0.0           2019/11/18     red        -
 * </pre>
 *
 * @author red
 * @version 1.0.0 2019/11/18 1:49 PM
 * @since 1.0.0
 */
@Component
@Slf4j
public class NextBlockQueue {
	@Resource
	private DbBlockManager dbBlockManager;
	@Resource
	private ClientStarter clientStarter;
	@Resource
	private PacketSender packetSender;

	/**
	 * prevHash->hash，记录上一区块hash和hash的映射
	 */
	private ConcurrentHashMap<String, List<BlockHash>> requestMap = new ConcurrentHashMap<>();

	/**
	 * 保存已经通过的区块hash,用于后面校验落地区块
	 */
	private List<String> wantHashs = Lists.newCopyOnWriteArrayList();

	public String pop(String hash) {
		if (wantHashs.remove(hash)) {
			return hash;
		}
		return null;
	}

	public List<BlockHash> get(String key) {
		return requestMap.get(key);
	}

	public void put(String key, List<BlockHash> responses) {
		requestMap.put(key, responses);
	}

	private void add(String key, BlockHash blockHash) {
		List<BlockHash> baseResponses = get(key);

		if (baseResponses == null) {
			baseResponses = new ArrayList<>();
		}
		//避免同一个机构多次投票
		for (BlockHash oldResponse : baseResponses) {
			if (StrUtil.equals(oldResponse.getAppId(), blockHash.getAppId())) {
				return;
			}
		}
		baseResponses.add(blockHash);
		put(key, baseResponses);
	}

	/**
	 * 查询key对应的BlockHash集合中，hash相同的数量
	 *
	 * @param key key
	 * @return hash最多的集合
	 */
	public List<BlockHash> findMaxHash(String key) {
		List<BlockHash> blockHashes = get(key);
		//寻找hash相同的总数量
		Map<String, Integer> map = new HashMap<>();
		for (BlockHash blockHash : blockHashes) {
			String hash = blockHash.getHash();
			map.merge(hash, 1, Integer::sum);
//			map.merge(hash, 1, (a, b) -> a + b);
		}
		//找到value最大的那个key，即被同意最多的hash
		String hash = getMaxKey(map);
		return blockHashes.stream().filter(blockHash -> hash.equals(blockHash.getHash())).collect(Collectors.toList());
	}

	private String getMaxKey(Map<String, Integer> hashMap) {
		int value, flagValue = 0;
		String key, flagKey = null;
		Set<Map.Entry<String, Integer>> entrySet = hashMap.entrySet();
		for (Map.Entry<String, Integer> entry : entrySet) {
			key = entry.getKey();
			value = entry.getValue();

			if (flagValue < value) {
				//flagKey flagValue 当判断出最大值是将最大值赋予该变量
				flagKey = key;
				flagValue = value;
			}
		}
		return flagKey;
	}


	public void remove(String key) {
		requestMap.remove(key);
	}

	/**
	 * 群发请求nextBlock的请求，收到新的回复，在此做处理。
	 *
	 * @param blockHash blockHash
	 */
	public void push(BlockHash blockHash) {
		String wantHash = blockHash.getHash();
		String prevHash = blockHash.getPrevHash();
		//创世块
		if (prevHash == null) {
			prevHash = "first_block_hash";
		}
		//针对该hash已经处理过了
		if (dbBlockManager.getBlockByHash(wantHash) != null) {
			remove(prevHash);
			return;
		}
		add(prevHash, blockHash);

		int agreeCount = clientStarter.pbftAgreeCount();

		//寻找集合中，哪个hash数最多
		int maxCount = findMaxHash(prevHash).size();

		//判断数量是否过线
		if (maxCount >= agreeCount - 1) {
			log.info("共有<{}>个节点返回next block hash为:{}", maxCount, wantHash);
			wantHashs.add(wantHash);
			//请求拉取该hash的Block
			BlockPacket blockPacket = new PacketBuilder<RpcSimpleBlockBody>().setType(PacketType
					.FETCH_BLOCK_INFO_REQUEST).setBody(new RpcSimpleBlockBody(wantHash)).build();
			packetSender.sendGroup(blockPacket);
			//remove后，这一次请求内的后续回复就凑不够agreeCount了，就不会再次触发全员请求block
			remove(prevHash);
		}
	}
}
