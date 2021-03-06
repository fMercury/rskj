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

package org.ethereum.core;

import co.rsk.core.RskAddress;
import co.rsk.crypto.Keccak256;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ajlopez on 28/02/2018.
 */
public class TransactionSet {
    private final Map<Keccak256, Transaction> transactionsByHash = new HashMap<>();
    private final Map<RskAddress, List<Transaction>> transactionsByAddress = new HashMap<>();

    public void addTransaction(Transaction transaction) {
        Keccak256 txhash = transaction.getHash();

        if (this.transactionsByHash.containsKey(txhash)) {
            return;
        }

        this.transactionsByHash.put(txhash, transaction);

        RskAddress senderAddress = transaction.getSender();

        List<Transaction> txs = this.transactionsByAddress.get(senderAddress);

        if (txs == null) {
            txs = new ArrayList<>();
            this.transactionsByAddress.put(senderAddress, txs);
        }

        txs.add(transaction);
    }

    public boolean hasTransaction(Transaction transaction) {
        return this.transactionsByHash.containsKey(transaction.getHash());
    }

    public void removeTransactionByHash(Keccak256 hash) {
        Transaction transaction = this.transactionsByHash.get(hash);

        if (transaction == null) {
            return;
        }

        this.transactionsByHash.remove(hash);

        RskAddress senderAddress = transaction.getSender();
        List<Transaction> txs = this.transactionsByAddress.get(senderAddress);

        if (txs != null) {
            txs.remove(transaction);

            if (txs.isEmpty()) {
                this.transactionsByAddress.remove(senderAddress);
            }
        }
    }

    public List<Transaction> getTransactions() {
        List<Transaction> ret = new ArrayList<>();
        ret.addAll(this.transactionsByHash.values());
        return ret;
    }

    public List<Transaction> getTransactionsWithSender(RskAddress senderAddress) {
        List<Transaction> list = this.transactionsByAddress.get(senderAddress);

        if (list == null) {
            return new ArrayList<>();
        }

        return list;
    }
}
