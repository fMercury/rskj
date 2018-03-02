/*
 * This file is part of RskJ
 * Copyright (C) 2018 RSK Labs Ltd.
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
package co.rsk.rpc.modules.txpool;

import co.rsk.test.builders.AccountBuilder;
import co.rsk.test.builders.TransactionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.ethereum.core.Account;
import org.ethereum.core.PendingState;
import org.ethereum.core.Transaction;
import org.ethereum.rpc.Web3Mocks;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;

import static org.mockito.Mockito.when;

public class TxPoolModuleImplTest {

    private final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
    private TxPoolModule txPoolModule;
    private PendingState pendingState;

    @Before
    public void setup(){
        pendingState = Web3Mocks.getMockPendingState();
        txPoolModule = new TxPoolModuleImpl(pendingState);
    }

    private Transaction createSampleTransaction() {
        Account sender = new AccountBuilder().name("sender").build();
        Account receiver = new AccountBuilder().name("receiver").build();

        Transaction tx = new TransactionBuilder()
                .sender(sender)
                .receiver(receiver)
                .value(BigInteger.TEN)
                .build();

        return tx;
    }

    @Test
    public void txpool_content_basic() throws IOException {
        String result = txPoolModule.content();
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.reader().forType(JsonNode.class).readValue(result);
        Assert.assertTrue(node.has("pending"));
        Assert.assertTrue(node.has("queued"));
        Assert.assertTrue(node.get("pending").isObject());
        Assert.assertTrue(node.get("queued").isObject());
    }

    @Test
    public void txpool_inspect_basic() throws IOException {
        String result = txPoolModule.inspect();
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.reader().forType(JsonNode.class).readValue(result);
        Assert.assertTrue(node.has("pending"));
        Assert.assertTrue(node.has("queued"));
        Assert.assertTrue(node.get("pending").isObject());
        Assert.assertTrue(node.get("queued").isObject());
    }

    @Test
    public void txpool_status_basic() throws IOException {
        String result = txPoolModule.status();
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.reader().forType(JsonNode.class).readValue(result);
        Assert.assertTrue(node.has("pending"));
        Assert.assertTrue(node.has("queued"));
        Assert.assertTrue(node.get("pending").isInt());
        Assert.assertTrue(node.get("queued").isInt());
    }

    @Test
    public void txpool_content_oneTx() throws Exception {
        Transaction tx = createSampleTransaction();
        when(pendingState.getPendingTransactions()).thenReturn(Collections.singletonList(tx));
        String result = txPoolModule.content();

        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.reader().forType(JsonNode.class).readValue(result);

        checkFieldIsEmpty(node, "queued");
        JsonNode pendingNode = checkFieldIsObject(node, "pending");
        JsonNode senderNode = checkFieldIsObject(pendingNode, tx.getSender().toString());
        JsonNode nonceNode = checkFieldIsArray(senderNode, tx.getNonceAsInteger().toString());
        nonceNode.elements().forEachRemaining(item -> assertFullTransaction(tx, item));
    }

    @Test
    public void txpool_inspect_oneTx() throws Exception {
        Transaction tx = createSampleTransaction();
        when(pendingState.getPendingTransactions()).thenReturn(Collections.singletonList(tx));
        String result = txPoolModule.inspect();

        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.reader().forType(JsonNode.class).readValue(result);

        checkFieldIsEmpty(node, "queued");
        JsonNode pendingNode = checkFieldIsObject(node, "pending");
        JsonNode senderNode = checkFieldIsObject(pendingNode, tx.getSender().toString());
        JsonNode nonceNode = checkFieldIsArray(senderNode, tx.getNonceAsInteger().toString());
        nonceNode.elements().forEachRemaining(item -> assertSummaryTransaction(tx, item));
    }

    @Test
    public void txpool_status_oneTx() throws Exception {
        Transaction tx = createSampleTransaction();
        when(pendingState.getPendingTransactions()).thenReturn(Collections.singletonList(tx));
        String result = txPoolModule.status();

        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.reader().forType(JsonNode.class).readValue(result);

        JsonNode queuedNode = checkFieldIsNumber(node, "queued");
        JsonNode pendingNode = checkFieldIsNumber(node, "pending");
        Assert.assertEquals(0, queuedNode.asInt());
        Assert.assertEquals(1, pendingNode.asInt());
    }

    private JsonNode checkFieldIsArray(JsonNode node, String fieldName) {
        Assert.assertTrue(node.has(fieldName));
        JsonNode fieldNode = node.get(fieldName);
        Assert.assertTrue(fieldNode.isArray());
        return fieldNode;
    }

    private JsonNode checkFieldIsObject(JsonNode node, String fieldName) {
        Assert.assertTrue(node.has(fieldName));
        JsonNode fieldNode = node.get(fieldName);
        Assert.assertTrue(fieldNode.isObject());
        return fieldNode;
    }

    private JsonNode checkFieldIsNumber(JsonNode node, String fieldName) {
        Assert.assertTrue(node.has(fieldName));
        JsonNode fieldNode = node.get(fieldName);
        Assert.assertTrue(fieldNode.isNumber());
        return fieldNode;
    }

    private void checkFieldIsEmpty(JsonNode node, String fieldName) {
        Assert.assertTrue(node.has(fieldName));
        JsonNode fieldNode = node.get(fieldName);
        Assert.assertTrue(fieldNode.isObject());
        Assert.assertFalse(fieldNode.fields().hasNext());
    }

    private void assertFullTransaction(Transaction tx, JsonNode transactionNode) {
        Assert.assertTrue(transactionNode.has("blockhash"));
        Assert.assertEquals(transactionNode.get("blockhash").asText(), "0x0000000000000000000000000000000000000000000000000000000000000000");
        Assert.assertTrue(transactionNode.has("blocknumber"));
        Assert.assertEquals(transactionNode.get("blocknumber"), jsonNodeFactory.nullNode());
        Assert.assertTrue(transactionNode.has("from"));
        Assert.assertEquals(transactionNode.get("from").asText(), tx.getSender().toString());
        Assert.assertTrue(transactionNode.has("gas"));
        Assert.assertEquals(transactionNode.get("gas").asText(), tx.getGasLimitAsInteger().toString());
        Assert.assertTrue(transactionNode.has("gasPrice"));
        Assert.assertEquals(transactionNode.get("gasPrice").asText(), tx.getGasPrice().toString());
        Assert.assertTrue(transactionNode.has("hash"));
        Assert.assertEquals(transactionNode.get("hash").asText(), tx.getHash().toHexString());
        Assert.assertTrue(transactionNode.has("input"));
        Assert.assertEquals(transactionNode.get("input").asText(), Hex.toHexString(tx.getData()));
        Assert.assertTrue(transactionNode.has("nonce"));
        Assert.assertEquals(transactionNode.get("nonce").asText(), tx.getNonceAsInteger().toString());
        Assert.assertTrue(transactionNode.has("to"));
        Assert.assertEquals(transactionNode.get("to").asText(), tx.getReceiveAddress().toString());
        Assert.assertTrue(transactionNode.has("transactionIndex"));
        Assert.assertEquals(transactionNode.get("transactionIndex"), jsonNodeFactory.nullNode());
        Assert.assertTrue(transactionNode.has("value"));
        Assert.assertEquals(transactionNode.get("value").asText(), tx.getValue().toString());
    }

    private void assertSummaryTransaction(Transaction tx, JsonNode summaryNode) {
        String summary = "{}: {} wei + {} x {} gas";
        String summaryFormatted = String.format(summary,
                tx.getReceiveAddress().toString(),
                tx.getValue().toString(),
                tx.getGasLimitAsInteger().toString(),
                tx.getGasPrice().toString());

        Assert.assertEquals(summaryFormatted, summaryNode.asText());
    }
}