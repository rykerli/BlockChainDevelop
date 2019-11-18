package com.nwnu.blockchain.p2p.core.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.nwnu.blockchain.block.Block;
import com.nwnu.blockchain.block.BlockBody;
import com.nwnu.blockchain.block.BlockHeader;
import com.nwnu.blockchain.block.Instruction;
import com.nwnu.blockchain.common.exception.TrustSDKException;
import com.nwnu.blockchain.p2p.socket.body.RpcBlockBody;
import com.nwnu.blockchain.p2p.socket.client.PacketSender;
import com.nwnu.blockchain.p2p.core.manager.DbBlockManager;
import com.nwnu.blockchain.p2p.core.manager.PermissionManager;
import com.nwnu.blockchain.p2p.core.requestbody.BlockRequestBody;
import com.nwnu.blockchain.p2p.socket.vote.BlockPacket;
import com.nwnu.blockchain.p2p.socket.vote.PacketBuilder;
import com.nwnu.blockchain.p2p.socket.vote.PacketType;
import com.nwnu.blockchain.tree.MerkleTree;
import com.nwnu.blockchain.utils.CommonUtil;
import com.nwnu.blockchain.utils.CryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

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
@Service
public class BlockService {
	@Resource
	private InstructionService instructionService;
	@Value("${version}")
	private int version;
	@Resource
	private PacketSender packetSender;
	@Resource
	private DbBlockManager dbBlockManager;
	@Resource
	private PermissionManager permissionManager;

	/**
	 * 校验指令集是否合法
	 *
	 * @param blockRequestBody 指令集
	 * @return 是否合法，为null则校验通过，其他则失败并返回原因
	 */
	public String check(BlockRequestBody blockRequestBody) throws TrustSDKException {
		//TODO 此处可能需要校验publicKey的合法性
		if (blockRequestBody == null || blockRequestBody.getBlockBody() == null || StrUtil.isEmpty(blockRequestBody
				.getPublicKey())) {
			return "请求参数缺失";
		}
		List<Instruction> instructions = blockRequestBody.getBlockBody().getInstructions();
		if (CollectionUtil.isEmpty(instructions)) {
			return "指令信息不能为空";
		}

		for (Instruction instruction : instructions) {
			if (!StrUtil.equals(blockRequestBody.getPublicKey(), instruction.getPublicKey())) {
				return "指令内公钥和传来的公钥不匹配";
			}
			if (!instructionService.checkSign(instruction)) {
				return "签名校验不通过";
			}
			if (!instructionService.checkHash(instruction)) {
				return "Hash校验不通过";
			}
		}

		if (!permissionManager.checkPermission(instructions)) {
			return "权限校验不通过";
		}

		return null;
	}

	/**
	 * 添加新的区块
	 *
	 * @param blockRequestBody blockRequestBody
	 * @return Block
	 */
	public Block addBlock(BlockRequestBody blockRequestBody) {
		BlockBody blockBody = blockRequestBody.getBlockBody();
		List<Instruction> instructions = blockBody.getInstructions();
		List<String> hashList = instructions.stream().map(Instruction::getHash).collect(Collectors
				.toList());

		BlockHeader blockHeader = new BlockHeader();
		blockHeader.setHashList(hashList);

		//计算所有指令的hashRoot
		blockHeader.setHashMerkleRoot(new MerkleTree(hashList).getRoot().toString());
		blockHeader.setPublicKey(blockRequestBody.getPublicKey());
		blockHeader.setTimeStamp(CommonUtil.getNow());
		blockHeader.setVersion(version);
		blockHeader.setNumber(dbBlockManager.getLastBlockNumber() + 1);
		blockHeader.setHashPreviousBlock(dbBlockManager.getLastBlockHash());
		Block block = new Block();
		block.setBlockBody(blockBody);
		block.setBlockHeader(blockHeader);
		block.setBlockHash(CryptoUtil.getSHA256(blockHeader.toString() + blockBody.toString()));

		BlockPacket blockPacket = new PacketBuilder<>().setType(PacketType.GENERATE_BLOCK_REQUEST).setBody(new
				RpcBlockBody(block)).build();

		//广播给其他人做验证
		packetSender.sendGroup(blockPacket);

		return block;
	}

}
