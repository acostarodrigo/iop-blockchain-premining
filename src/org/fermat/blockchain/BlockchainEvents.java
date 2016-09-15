package org.fermat.blockchain;

import org.fermatj.core.*;

import java.util.List;
import java.util.Set;

/**
 * Created by rodrigo on 7/22/16.
 */
public class BlockchainEvents implements PeerEventListener {
    @Override
    public void onPeersDiscovered(Set<PeerAddress> set) {

    }

    @Override
    public void onBlocksDownloaded(Peer peer, Block block, FilteredBlock filteredBlock, int i) {

    }

    @Override
    public void onChainDownloadStarted(Peer peer, int i) {
        System.out.println("download started from peer " + peer.toString());
    }

    @Override
    public void onPeerConnected(Peer peer, int i) {
        System.out.println("Connected to peer " + peer.toString());
    }

    @Override
    public void onPeerDisconnected(Peer peer, int i) {

    }

    @Override
    public Message onPreMessageReceived(Peer peer, Message message) {
        return null;
    }

    @Override
    public void onTransaction(Peer peer, Transaction transaction) {

    }

    @Override
    public List<Message> getData(Peer peer, GetDataMessage getDataMessage) {
        return null;
    }

}
