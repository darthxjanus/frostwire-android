/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(TM). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.transfers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.frostwire.transfers.TransferItem;
import com.frostwire.transfers.TransferState;
import com.frostwire.util.ByteUtils;
import com.frostwire.vuze.VuzeDownloadManager;
import com.frostwire.vuze.VuzeFileInfo;
import com.frostwire.vuze.VuzeFormatter;
import com.frostwire.vuze.VuzeUtils;
import com.frostwire.vuze.VuzeUtils.InfoSetQuery;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class AzureusBittorrentDownload implements BittorrentDownload {

    private final TransferManager manager;
    private final VuzeDownloadManager downloadManager;
    private final String hash;

    private List<TransferItem> items;

    private boolean partialDownload;
    private Set<VuzeFileInfo> fileInfoSet;
    private long lastChangedTime;

    public AzureusBittorrentDownload(TransferManager manager, VuzeDownloadManager downloadManager) {
        this.manager = manager;
        this.downloadManager = downloadManager;
        this.hash = ByteUtils.encodeHex(downloadManager.getHash());

        refreshData(); // super mutable
    }

    @Override
    public String getName() {
        return getDisplayName();
    }

    public String getDisplayName() {
        return downloadManager.getDisplayName();
    }

    public TransferState getState() {
        // TODO:BITTORRENT
        //return downloadManager.getStatus();
        return TransferState.ERROR;
    }

    public int getProgress() {
        refreshData();

        if (isComplete()) {
            return 100;
        }

        if (partialDownload) {
            long downloaded = 0;
            for (VuzeFileInfo fileInfo : fileInfoSet) {
                downloaded += fileInfo.getDownloaded();
            }
            return (int) ((downloaded * 100) / getSize());
        } else {
            return downloadManager.getDownloadCompleted();
        }
    }

    public long getSize() {
        return downloadManager.getSize();
    }

    public boolean isResumable() {
        return downloadManager.isResumable();
    }

    public boolean isPausable() {
        return downloadManager.isPausable();
    }

    public boolean isComplete() {
        return downloadManager.isComplete();
    }

    public boolean isDownloading() {
        return downloadManager.isDownloading();
    }

    public boolean isSeeding() {
        return downloadManager.isSeeding();
    }

    public List<TransferItem> getItems() {
        if (items.size() == 1) {
            return Collections.emptyList();
        }
        return items;
    }
    
    public void enqueue() {
        if (isPausable()) {
            // TODO:BITTORRENT
            //downloadManager.enqueue();
        }
    }

    public void pause() {
        if (isPausable()) {
            downloadManager.stop();
        }
    }

    public void resume() {
        if (isResumable()) {
            downloadManager.start();
        }
    }

    public File getSavePath() {
        return downloadManager.getSavePath();
    }

    public long getBytesReceived() {
        return downloadManager.getBytesReceived();
    }

    public long getBytesSent() {
        return downloadManager.getBytesSent();
    }

    public long getDownloadSpeed() {
        return downloadManager.getDownloadSpeed();
    }

    public long getUploadSpeed() {
        return downloadManager.getUploadSpeed();
    }

    public long getETA() {
        return downloadManager.getETA();
    }

    public Date getCreated() {
        return downloadManager.getCreationDate();
    }

    public String getPeers() {
        return VuzeFormatter.formatPeers(downloadManager.getPeers(), downloadManager.getConnectedPeers(), downloadManager.hasStarted(), downloadManager.hasScrape());
    }

    public String getSeeds() {
        return VuzeFormatter.formatSeeds(downloadManager.getSeeds(), downloadManager.getConnectedSeeds(), downloadManager.hasStarted(), downloadManager.hasScrape());
    }

    public String getHash() {
        return hash;
    }

    public String getSeedToPeerRatio() {
        return VuzeFormatter.formatSeedToPeerRatio(downloadManager.getConnectedSeeds(), downloadManager.getConnectedPeers());
    }

    public String getShareRatio() {
        return VuzeFormatter.formatShareRatio(downloadManager.getShareRatio());
    }

    @Override
    public void remove() {
        remove(false);
    }

    @Override
    public void remove(boolean deleteData) {
        manager.remove(this);
        VuzeUtils.removeDownload(downloadManager, deleteData, deleteData);
    }

    VuzeDownloadManager getDownloadManager() {
        return downloadManager;
    }

    public String getDetailsUrl() {
        return null;
    }

    private void refreshData() {
        if (lastChangedTime < downloadManager.getChangedTime()) {
            lastChangedTime = downloadManager.getChangedTime();
            fileInfoSet = VuzeUtils.getFileInfoSet(downloadManager, InfoSetQuery.NO_SKIPPED);
            partialDownload = !VuzeUtils.getFileInfoSet(downloadManager, InfoSetQuery.SKIPPED).isEmpty();

            items = new ArrayList<TransferItem>(fileInfoSet.size());
            for (VuzeFileInfo fileInfo : fileInfoSet) {
                items.add(new AzureusBittorrentDownloadItem(fileInfo));
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BittorrentDownload)) {
            return false;
        }

        return getHash().equals(((BittorrentDownload) o).getHash());
    }
}
