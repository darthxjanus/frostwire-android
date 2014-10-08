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

import com.frostwire.transfers.TransferItem;
import com.frostwire.transfers.TransferState;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author gubatron
 * @author aldenml
 *
 */
final class InvalidBittorrentDownload implements BittorrentDownload, InvalidTransfer {

    private final int reasonResId;

    public InvalidBittorrentDownload(int reasonResId) {
        this.reasonResId = reasonResId;
    }

    public int getReasonResId() {
        return reasonResId;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public File getSavePath() {
        return null;
    }

    @Override
    public long getBytesReceived() {
        return 0;
    }

    @Override
    public long getBytesSent() {
        return 0;
    }

    @Override
    public long getDownloadSpeed() {
        return 0;
    }

    @Override
    public long getUploadSpeed() {
        return 0;
    }

    @Override
    public long getETA() {
        return 0;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public TransferState getState() {
        return TransferState.ERROR;
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public Date getCreated() {
        return null;
    }

    public boolean isComplete() {
        return false;
    }

    @Override
    public void remove() {
    }

    @Override
    public String getHash() {
        return null;
    }

    @Override
    public String getPeers() {
        return null;
    }

    @Override
    public String getSeeds() {
        return null;
    }

    @Override
    public String getSeedToPeerRatio() {
        return null;
    }

    @Override
    public String getShareRatio() {
        return null;
    }

    @Override
    public boolean isResumable() {
        return false;
    }

    @Override
    public boolean isPausable() {
        return false;
    }

    @Override
    public boolean isDownloading() {
        return false;
    }

    @Override
    public boolean isSeeding() {
        return false;
    }

    @Override
    public void pause() {
    }
    
    @Override
    public void enqueue() {
    }

    @Override
    public void resume() {
    }

    @Override
    public List<TransferItem> getItems() {
        return Collections.emptyList();
    }

    @Override
    public void remove(boolean deleteData) {
    }

    public String getDetailsUrl() {
        return null;
    }
}
