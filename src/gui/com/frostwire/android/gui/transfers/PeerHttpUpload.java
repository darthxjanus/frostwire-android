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

import com.frostwire.android.R;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.transfers.TransferItem;
import com.frostwire.transfers.TransferState;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author gubatron
 * @author aldenml
 */
public class PeerHttpUpload implements UploadTransfer {

    private static final int STATUS_UPLOADING = 1;
    private static final int STATUS_COMPLETE = 2;
    private static final int STATUS_CANCELLED = 3;

    private static final int SPEED_AVERAGE_CALCULATION_INTERVAL_MILLISECONDS = 1000;

    private final TransferManager manager;
    private final FileDescriptor fd;
    private final Date dateCreated;

    private TransferState status;
    public long bytesSent;
    public long averageSpeed; // in bytes

    // variables to keep the upload rate of this transfer
    private long speedMarkTimestamp;
    private long totalSentSinceLastSpeedStamp;

    PeerHttpUpload(TransferManager manager, FileDescriptor fd) {
        this.manager = manager;
        this.fd = fd;
        this.dateCreated = new Date();

        status = TransferState.UPLOADING;
    }

    public FileDescriptor getFD() {
        return fd;
    }

    @Override
    public String getName() {
        return fd.title;
    }

    public String getDisplayName() {
        return fd.title;
    }

    @Override
    public File getSavePath() {
        return null;
    }

    public TransferState getState() {
        return status;
    }

    public int getProgress() {
        return isComplete() ? 100 : (int) ((bytesSent * 100) / fd.fileSize);
    }

    public long getSize() {
        return fd.fileSize;
    }

    public Date getCreated() {
        return dateCreated;
    }

    public long getBytesReceived() {
        return 0;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public long getDownloadSpeed() {
        return 0;
    }

    public long getUploadSpeed() {
        return averageSpeed;
    }

    public long getETA() {
        long speed = getUploadSpeed();
        return speed > 0 ? (fd.fileSize - getBytesSent()) / speed : Long.MAX_VALUE;
    }

    public boolean isComplete() {
        return bytesSent == fd.fileSize;
    }

    public boolean isUploading() {
        return TransferState.UPLOADING.equals(status);
    }

    public List<TransferItem> getItems() {
        return Collections.emptyList();
    }

    public void remove() {
        if (!TransferState.COMPLETE.equals(status)) {
            status = TransferState.CANCELED;
        }
        manager.remove(this);
    }

    public void addBytesSent(int n) {
        bytesSent += n;
        updateAverageUploadSpeed();
    }

    public void complete() {
        status = TransferState.COMPLETE;
        remove();
    }

    public boolean isCanceled() {
        return TransferState.CANCELED.equals(status);
    }

    private String getStatusString(int status) {
        int resId;
        switch (status) {
            case STATUS_UPLOADING:
                resId = (getUploadSpeed() < 102400) ? R.string.peer_http_upload_status_streaming : R.string.peer_http_upload_status_uploading;
                break;
            case STATUS_COMPLETE:
                resId = R.string.peer_http_upload_status_complete;
                break;
            case STATUS_CANCELLED:
                resId = R.string.peer_http_upload_status_cancelled;
                break;
            default:
                resId = R.string.peer_http_upload_status_unknown;
                break;
        }
        return String.valueOf(resId);
    }

    private void updateAverageUploadSpeed() {
        long now = System.currentTimeMillis();

        if (now - speedMarkTimestamp > SPEED_AVERAGE_CALCULATION_INTERVAL_MILLISECONDS) {
            averageSpeed = ((bytesSent - totalSentSinceLastSpeedStamp) * 1000) / (now - speedMarkTimestamp);
            speedMarkTimestamp = now;
            totalSentSinceLastSpeedStamp = bytesSent;
        }
    }

    public String getDetailsUrl() {
        return null;
    }
}
