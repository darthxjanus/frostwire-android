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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.frostwire.transfers.DownloadTransfer;
import com.frostwire.transfers.TransferItem;
import com.frostwire.transfers.TransferState;
import org.apache.commons.io.FilenameUtils;

import android.os.SystemClock;
import android.util.Log;

import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.core.HttpFetcher;
import com.frostwire.android.core.HttpFetcherListener;
import com.frostwire.android.gui.Librarian;
import com.frostwire.android.gui.Peer;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.util.SystemUtils;
import com.frostwire.android.util.concurrent.AbstractRunnable;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public final class PeerHttpDownload implements DownloadTransfer {

    private static final String TAG = "FW.PeerHttpDownload";

    private static final int STATUS_DOWNLOADING = 1;
    private static final int STATUS_COMPLETE = 2;
    private static final int STATUS_ERROR = 3;
    private static final int STATUS_CANCELLED = 4;
    private static final int STATUS_WAITING = 5;

    private static final int SPEED_AVERAGE_CALCULATION_INTERVAL_MILLISECONDS = 1000;

    private final TransferManager manager;
    private final Peer peer;
    private final FileDescriptor fd;
    private final Date dateCreated;
    private final File savePath;

    private TransferState status;
    private long bytesReceived;
    public long averageSpeed; // in bytes

    // variables to keep the download rate of file transfer
    private long speedMarkTimestamp;
    private long totalReceivedSinceLastSpeedStamp;

    PeerHttpDownload(TransferManager manager, Peer peer, FileDescriptor fd) {
        this.manager = manager;
        this.peer = peer;
        this.fd = fd;
        this.dateCreated = new Date();
        this.savePath = new File(SystemUtils.getSaveDirectory(fd.fileType), cleanFileName(FilenameUtils.getName(fd.filePath)));
        status = TransferState.DOWNLOADING;
    }

    public Peer getPeer() {
        return peer;
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

    public TransferState getState() {
        return status;
    }

    public int getProgress() {
        return isComplete() ? 100 : (int) ((bytesReceived * 100) / fd.fileSize);
    }

    public long getSize() {
        return fd.fileSize;
    }

    public Date getCreated() {
        return dateCreated;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public long getBytesSent() {
        return 0;
    }

    public long getDownloadSpeed() {
        return (!isDownloading()) ? 0 : averageSpeed;
    }

    public long getUploadSpeed() {
        return 0;
    }

    public long getETA() {
        long speed = getDownloadSpeed();
        return speed > 0 ? (fd.fileSize - getBytesReceived()) / speed : Long.MAX_VALUE;
    }

    public boolean isComplete() {
        return bytesReceived == fd.fileSize;
    }

    public boolean isDownloading() {
        return TransferState.DOWNLOADING.equals(status);
    }

    public List<TransferItem> getItems() {
        return Collections.emptyList();
    }

    public File getSavePath() {
        return savePath;
    }

    public void remove() {
        remove(false);
    }

    public void remove(boolean deleteData) {
        if (!TransferState.COMPLETE.equals(status)) {
            status = TransferState.CANCELED;
        }
        if (!TransferState.COMPLETE.equals(status) || deleteData) {
            cleanup();
        }
        manager.remove(this);
    }

    public void start() {
        start(0, 0);
    }

    /**
     * 
     * @param delay in seconds.
     * @param retry
     */
    private void start(final int delay, final int retry) {
        Engine.instance().getThreadPool().execute(new AbstractRunnable(getDisplayName()) {
            public void run() {
                try {
                    status = TransferState.WAITING;
                    SystemClock.sleep(delay * 1000);

                    status = TransferState.DOWNLOADING;
                    String uri = peer.getDownloadUri(fd);
                    new HttpFetcher(uri).save(savePath, new DownloadListener(retry));
                    Librarian.instance().scan(savePath);
                } catch (Throwable e) {
                    error(e);
                }
            }
        });
    }

    private String getStatusString(int status) {
        int resId;
        switch (status) {
        case STATUS_DOWNLOADING:
            resId = R.string.peer_http_download_status_downloading;
            break;
        case STATUS_COMPLETE:
            resId = R.string.peer_http_download_status_complete;
            break;
        case STATUS_ERROR:
            resId = R.string.peer_http_download_status_error;
            break;
        case STATUS_CANCELLED:
            resId = R.string.peer_http_download_status_cancelled;
            break;
        case STATUS_WAITING:
            resId = R.string.peer_http_download_status_waiting;
            break;
        default:
            resId = R.string.peer_http_download_status_unknown;
            break;
        }
        return String.valueOf(resId);
    }

    private void updateAverageDownloadSpeed() {
        long now = System.currentTimeMillis();

        if (now - speedMarkTimestamp > SPEED_AVERAGE_CALCULATION_INTERVAL_MILLISECONDS) {
            averageSpeed = ((bytesReceived - totalReceivedSinceLastSpeedStamp) * 1000) / (now - speedMarkTimestamp);
            speedMarkTimestamp = now;
            totalReceivedSinceLastSpeedStamp = bytesReceived;
        }
    }

    private void complete() {
        status = TransferState.COMPLETE;
        manager.incrementDownloadsToReview();
        Engine.instance().notifyDownloadFinished(getDisplayName(), getSavePath());
        Librarian.instance().scan(savePath.getAbsoluteFile());
    }

    private void error(Throwable e) {
        if (!TransferState.CANCELED.equals(status)) {
            Log.e(TAG, "Error downloading file: " + fd + " from " + peer, e);
            status = TransferState.ERROR;
            cleanup();
        }
    }

    private void cleanup() {
        try {
            savePath.delete();
        } catch (Throwable tr) {
            // ignore
        }
    }
    
    // aldenml: figure out the proper way to solve this, since
    // illegal characters for file names are tied to the
    // limitations of the file system, hence are OS dependent
    /**
     * Sorted list of invalid filename character keycodes.
     * @author gubatron
     */
    public final static int[] illegalChars = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 34, 42, 47, 58, 60, 62, 63, 92, 124};
    
    public static String cleanFileName(String badFileName) {

        StringBuilder cleanName = new StringBuilder();
        for (int i = 0; i < badFileName.length(); i++) {
            int c = (int) badFileName.charAt(i);
            if (Arrays.binarySearch(illegalChars, c) < 0) {
                cleanName.append((char) c);
            }
        }
        return cleanName.toString();
    }
   

    private final class DownloadListener implements HttpFetcherListener {

        private final int retry;

        public DownloadListener(int retry) {
            this.retry = retry;
        }

        public void onData(byte[] data, int length) {
            bytesReceived += length;
            updateAverageDownloadSpeed();

            if (status == TransferState.CANCELED) {
                // ok, this is not the most elegant solution but it effectively breaks the
                // download logic flow.
                throw new RuntimeException("Invalid status, transfer cancelled");
            }
        }

        public void onSuccess(byte[] body) {
            complete();
        }

        public void onError(Throwable e, int statusCode, Map<String, String> headers) {
            try {
                if (statusCode == 503 && headers.containsKey("Retry-After") && retry < Constants.MAX_PEER_HTTP_DOWNLOAD_RETRIES) {
                    int delay = Integer.parseInt(headers.get("Retry-After"));
                    if (delay > 0) {
                        start(delay, retry + 1);
                    } else {
                        error(e);
                    }
                } else {
                    error(e);
                }
            } catch (Throwable tr) {
                error(tr);
            }
        }
    }

    public String getDetailsUrl() {
        return getPeer().getDownloadUri(getFD());
    }
}
