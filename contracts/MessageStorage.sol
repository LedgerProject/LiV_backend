//SPDX-License-Identifier: UNLICENSED

pragma solidity >=0.7.0;


contract MessageStorage {

    struct Transaction {
        string messageHash;
        string signature;
        string pubKey;
    }

    mapping (string => Transaction) transactions;
    mapping (string => uint) txIdexer;

    function addMessage(string memory _messageHash, string memory _signature, string memory _pubKey) public returns (uint) {
        transactions[_messageHash] = Transaction(_messageHash, _signature, _pubKey);
        return txIdexer[_messageHash]-1;
    }

    function getMessage(string memory _messageHash) public view returns(string memory, string memory, string memory) {
        Transaction memory transaction = transactions[_messageHash];
        return (transaction.messageHash, transaction.signature, transaction.pubKey);
    }
}