/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk;

import co.rsk.blocks.FileBlockPlayer;
import co.rsk.blocks.FileBlockRecorder;
import co.rsk.config.RskSystemProperties;
import co.rsk.core.Rsk;
import co.rsk.core.RskAddress;
import co.rsk.core.RskImpl;
import co.rsk.crypto.Keccak256;
import co.rsk.mine.MinerClient;
import co.rsk.mine.MinerServer;
import co.rsk.mine.TxBuilder;
import co.rsk.mine.TxBuilderEx;
import co.rsk.net.BlockProcessResult;
import co.rsk.net.BlockProcessor;
import co.rsk.net.MessageHandler;
import co.rsk.net.Metrics;
import co.rsk.net.discovery.UDPServer;
import co.rsk.net.handler.TxHandler;
import co.rsk.rpc.netty.Web3HttpServer;
import co.rsk.trie.Trie;
import co.rsk.trie.TrieImpl;
import co.rsk.trie.TrieStore;
import co.rsk.trie.TrieStoreImpl;
import org.ethereum.cli.CLIInterface;
import org.ethereum.config.DefaultConfig;
import org.ethereum.core.*;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.server.ChannelManager;
import org.ethereum.rpc.Web3;
import org.ethereum.sync.SyncPool;
import org.ethereum.util.BuildInfo;
import org.ethereum.vm.PrecompiledContracts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

import static org.ethereum.datasource.DataSourcePool.levelDbByName;
import static org.ethereum.util.ByteUtil.toHexString;

@Component
public class DoPrune {
    private static Logger logger = LoggerFactory.getLogger("start");

    private final Rsk rsk;
    private final RskSystemProperties rskSystemProperties;
    private final Blockchain blockchain;

    private final PendingState pendingState;

    public static void main(String[] args) throws Exception {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(DefaultConfig.class);
        DoPrune runner = ctx.getBean(DoPrune.class);
        runner.doPrune(args);
        runner.stop();
    }

    @Autowired
    public DoPrune(Rsk rsk,
                   RskSystemProperties rskSystemProperties,
                   Blockchain blockchain,
                   PendingState pendingState) {
        this.rsk = rsk;
        this.rskSystemProperties = rskSystemProperties;
        this.blockchain = blockchain;
        this.pendingState = pendingState;
    }

    public void doPrune(String[] args) throws Exception {
        logger.info("Pruning Database");

        CLIInterface.call(rskSystemProperties, args);
        logger.info("Running {},  core version: {}-{}", rskSystemProperties.genesisInfo(), rskSystemProperties.projectVersion(), rskSystemProperties.projectVersionModifier());
        BuildInfo.printInfo();

        long height = this.blockchain.getBestBlock().getNumber();

        String dataSourceName = getDataSourceName();
        logger.info("Datasource Name {}", dataSourceName);
        logger.info("Blockchain height {}", height);

        TrieImpl source = new TrieImpl(new TrieStoreImpl(levelDbByName(this.rskSystemProperties, dataSourceName)), true);
        TrieStore target = new TrieStoreImpl(levelDbByName(this.rskSystemProperties, dataSourceName + "B"));
        this.processBlocks(height - 100, source, PrecompiledContracts.REMASC_ADDR, target);
    }

    public void processBlocks(long from, TrieImpl source, RskAddress contractAddress, TrieStore target) {
        long n = from;

        while (true) {
            List<Block> blocks = this.blockchain.getBlocksByNumber(n);

            if (blocks.isEmpty())
                break;

            for (Block b: blocks) {
                byte[] stateRoot = b.getStateRoot();

                logger.info("Block height {} State root {}", b.getNumber(), Hex.toHexString(stateRoot));
                Repository repo = this.blockchain.getRepository().getSnapshotTo(stateRoot);

                AccountState accountState = repo.getAccountState(contractAddress);
                Trie contractStorage = source.getSnapshotTo(new Keccak256(accountState.getStateRoot()));
                contractStorage.copyTo(target);
            }

            n++;
        }
    }

    public void stop() {
        logger.info("Shutting down RSK node");
        rsk.close();
    }

    private static String getDataSourceName() {
        return "details-storage/" + PrecompiledContracts.REMASC_ADDR;
    }
}
