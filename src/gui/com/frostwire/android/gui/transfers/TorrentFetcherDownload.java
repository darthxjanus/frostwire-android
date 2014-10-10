/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
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

import com.frostwire.bittorrent.BTEngine;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.logging.Logger;
import com.frostwire.transfers.BittorrentDownload;
import com.frostwire.transfers.TransferItem;
import com.frostwire.transfers.TransferState;
import com.frostwire.util.HttpClientFactory;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author gubatron
 * @author aldenml
 */
public class TorrentFetcherDownload implements BittorrentDownload {

    private static final Logger LOG = Logger.getLogger(TorrentFetcherDownload.class);

    private final TransferManager manager;
    private final TorrentDownloadInfo info;
    private final Date created;

    private final String uri;

    private TransferState state;

    public TorrentFetcherDownload(TransferManager manager, TorrentDownloadInfo info) {
        this.manager = manager;
        this.info = info;
        this.created = new Date();

        this.uri = info.getTorrentUri();

        this.state = TransferState.DOWNLOADING_TORRENT;

        Thread t = new Thread(new FetcherRunnable(), "Torrent-Fetcher - " + uri);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public String getName() {
        return info.getTorrentUri();
    }

    @Override
    public String getDisplayName() {
        return info.getDisplayName();
    }

    @Override
    public TransferState getState() {
        return state;
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public long getSize() {
        return info.getSize();
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public List<TransferItem> getItems() {
        return Collections.emptyList();
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
    public String getInfoHash() {
        return info.getHash();
    }

    @Override
    public int getConnectedPeers() {
        return 0;
    }

    @Override
    public int getConnectedSeeds() {
        return 0;
    }

    @Override
    public int getTotalPeers() {
        return 0;
    }

    @Override
    public int getTotalSeeds() {
        return 0;
    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public boolean isDownloading() {
        return state == TransferState.DOWNLOADING;
    }

    @Override
    public boolean isSeeding() {
        return false;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public boolean isUploading() {
        return false;
    }

    @Override
    public void remove() {
        remove(false);
    }

    @Override
    public void remove(boolean deleteData) {
        // TODO:BITTORRENT
//        statusResId = TransferState.CANCELED;
//
//        if (delegate != null) {
//            delegate.remove(deleteData);
//        } else {
//            removed = true;
//            try {
//                torrentDownloader.cancel();
//            } catch (Throwable e) {
//                // ignore, I can't do anything
//                LOG.error("Error canceling torrent downloader", e);
//            }
//            try {
//                torrentDownloader.getFile().delete();
//            } catch (Throwable e) {
//                // ignore, I can't do anything
//                LOG.error("Error deleting file of torrent downloader", e);
//            }
//        }
        remove(false, false);
    }

    @Override
    public void remove(boolean deleteTorrent, boolean deleteData) {
        state = TransferState.CANCELED;
        manager.remove(this);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    // TODO:BITTORRENT
    /*
    private final class TorrentDownloaderListener implements VuzeTorrentDownloadListener {

        private AtomicBoolean finished = new AtomicBoolean(false);

        @Override
        public void onFinished(VuzeTorrentDownloader dl) {
            if (removed) {
                return;
            }
            if (finished.compareAndSet(false, true)) {
                try {

                    Set<String> selection = new HashSet<String>();
                    if (info.getRelativePath() != null) {
                        selection.add(info.getRelativePath());
                    }
                    VuzeDownloadManager dm = manager.createVDM(dl.getFile().getAbsolutePath(), selection);

                    delegate = new AzureusBittorrentDownload(manager, dm);

                } catch (Throwable e) {
                    statusResId = TransferState.ERROR;
                    LOG.error("Error creating the actual torrent download", e);
                }
            }
        }

        @Override
        public void onError(VuzeTorrentDownloader dl) {
            if (removed) {
                return;
            }
            statusResId = TransferState.ERROR;
        }
    }*/

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TorrentFetcherDownload)) {
            return false;
        }

        String n1 = this.getName();
        String n2 = ((TorrentFetcherDownload) o).getName();

        return n1.equalsIgnoreCase(n2);
    }

    private void downloadTorrent(final byte[] data) {
        boolean[] selection = info.getSelection();

        TorrentInfo ti = TorrentInfo.bdecode(data);
        
        try {
            BTEngine.getInstance().download(ti, null, selection);
        } finally {
            remove();
        }
    }

    private class FetcherRunnable implements Runnable {

        @Override
        public void run() {
            if (state == TransferState.CANCELED) {
                return;
            }

            try {
                byte[] data;
                if (uri.startsWith("http")) {
                    // use our http client, since we can handle referer
                    data = HttpClientFactory.newInstance().getBytes(uri, 30000, info.getDetailsUrl());
                } else {
                    data = BTEngine.getInstance().fetchMagnet(uri, 30000);
                }

                if (state == TransferState.CANCELED) {
                    return;
                }

                if (data != null) {
                    downloadTorrent(data);
                } else {
                    state = TransferState.ERROR;
                }
            } catch (Throwable e) {
                state = TransferState.ERROR;
                LOG.error("Error downloading torrent from uri", e);
            }
        }
    }
}
