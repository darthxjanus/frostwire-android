package com.frostwire.android.gui.transfers;

import com.frostwire.bittorrent.BTDownload;
import com.frostwire.transfers.BittorrentDownload;
import com.frostwire.transfers.TransferItem;
import com.frostwire.transfers.TransferState;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * This proxy class is necessary to handle android specific UI actions different to other
 * platforms.
 *
 * @author gubatron
 * @author aldenml
 */
public final class UIBittorrentDownload implements BittorrentDownload {

    private final TransferManager manager;
    private final BTDownload dl;

    private TransferState state;

    public UIBittorrentDownload(TransferManager manager, BTDownload dl) {
        this.manager = manager;
        this.dl = dl;

        this.state = dl.getState();

        // TODO:BITTORRENT
        // review the logic of under what conditions we can actually start with resume
        dl.resume();
    }

    @Override
    public String getInfoHash() {
        return dl.getInfoHash();
    }

    @Override
    public int getConnectedPeers() {
        return dl.getConnectedPeers();
    }

    @Override
    public int getTotalPeers() {
        return dl.getTotalPeers();
    }

    @Override
    public int getConnectedSeeds() {
        return dl.getConnectedSeeds();
    }

    @Override
    public int getTotalSeeds() {
        return dl.getTotalSeeds();
    }

    @Override
    public boolean isPaused() {
        return dl.isPaused();
    }

    @Override
    public boolean isSeeding() {
        return dl.isSeeding();
    }

    @Override
    public boolean isFinished() {
        return dl.isFinished();
    }

    @Override
    public void pause() {
        dl.pause();
    }

    @Override
    public void resume() {
        dl.resume();
    }

    @Override
    public boolean isDownloading() {
        return dl.isDownloading();
    }

    @Override
    public void remove(boolean deleteData) {
        dl.remove(deleteData);
    }

    @Override
    public boolean isUploading() {
        return dl.isUploading();
    }

    @Override
    public String getName() {
        return dl.getName();
    }

    @Override
    public String getDisplayName() {
        return dl.getDisplayName();
    }

    @Override
    public File getSavePath() {
        return dl.getSavePath();
    }

    @Override
    public long getSize() {
        return dl.getSize();
    }

    @Override
    public Date getCreated() {
        return dl.getCreated();
    }

    @Override
    public TransferState getState() {
        if (state != TransferState.ERROR) {
            state = dl.getState();
        }

        return state;
    }

    @Override
    public long getBytesReceived() {
        return dl.getBytesReceived();
    }

    @Override
    public long getBytesSent() {
        return dl.getBytesSent();
    }

    @Override
    public long getDownloadSpeed() {
        return dl.getDownloadSpeed();
    }

    @Override
    public long getUploadSpeed() {
        return dl.getUploadSpeed();
    }

    @Override
    public long getETA() {
        return dl.getETA();
    }

    @Override
    public int getProgress() {
        return dl.getProgress();
    }

    @Override
    public boolean isComplete() {
        return dl.isComplete();
    }

    @Override
    public List<TransferItem> getItems() {
        return dl.getItems();
    }

    @Override
    public void remove() {
        try {
            dl.remove();
            manager.remove(this);
        } catch (Throwable e) {
            state = TransferState.ERROR;
        }
    }

    @Override
    public void remove(boolean deleteTorrent, boolean deleteData) {
        dl.remove(deleteTorrent, deleteData);
    }
}
