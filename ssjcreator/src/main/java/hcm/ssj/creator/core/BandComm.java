/*
 * BandComm.java
 * Copyright (c) 2017
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura
 * *****************************************************
 * This file is part of the Social Signal Interpretation for Java (SSJ) framework
 * developed at the Lab for Human Centered Multimedia of the University of Augsburg.
 *
 * SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a
 * one-to-one port of SSI to Java, it is an approximation. Nor does SSJ pretend
 * to offer SSI's comprehensive functionality and performance (this is java after all).
 * Nevertheless, SSJ borrows a lot of programming patterns from SSI.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package hcm.ssj.creator.core;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.notifications.MessageFlags;
import com.microsoft.band.notifications.VibrationType;
import com.microsoft.band.tiles.BandTile;
import com.microsoft.band.tiles.pages.FlowPanel;
import com.microsoft.band.tiles.pages.FlowPanelOrientation;
import com.microsoft.band.tiles.pages.HorizontalAlignment;
import com.microsoft.band.tiles.pages.PageData;
import com.microsoft.band.tiles.pages.PageLayout;
import com.microsoft.band.tiles.pages.TextButton;
import com.microsoft.band.tiles.pages.TextButtonData;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import hcm.ssj.creator.R;

/**
 * Created by Johnny on 01.02.2017.
 */

public class BandComm {

    BandClient client;

    public static final int BTN_YES = 1;
    public static final int BTN_NO = 2;

    public static final UUID tileId = UUID.nameUUIDFromBytes("SSJCreator-tile".getBytes());//fromString("cc0D508F-70A3-47D4-BBA3-812BADB1F8Aa");
    public static final UUID pageId = UUID.nameUUIDFromBytes("SSJCreator-page".getBytes());//UUID.fromString("b1234567-89ab-cdef-0123-456789abcd00");

    Activity activity = null;
    private String tileTitle = "SSJ";

    public BandComm(Activity activity)
    {
        this.activity = activity;
    }

    public void create()
    {
        new StartTask().execute();
    }

    public void destroy()
    {
        new StopTask().execute();
    }

    public void popup(String title, String body)
    {
        new PingTask(title, body).execute();
    }

    public boolean isBandInitialized()
    {
        try
        {
            if (getConnectedBandClient() && doesTileExist())
                return true;
            else
                return false;
        }
        catch (BandException | InterruptedException e) {
            Log.e(this.getClass().getSimpleName(), "exception", e);
            return false;
        }
    }

    private class StartTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            try {

                if (getConnectedBandClient())
                {
                    Log.i(this.getClass().getSimpleName(), "seting up Band");
                    if (addTile()) {
                        updatePages();
                    }
                    else
                    {
                        Log.e(this.getClass().getSimpleName(), "Cannot find Band tile, make sure the band has been set up.");
                        return false;
                    }
                } else {
                    Log.e(this.getClass().getSimpleName(), "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                    return false;
                }
            } catch (BandException e) {
                handleBandException(e);
                return false;
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), e.getMessage());
                return false;
            }

            return true;
        }
    }

    public class StopTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (getConnectedBandClient())
                {
                    Log.i(this.getClass().getSimpleName(), "removing tile from Band.");
                    removeTile();
                } else {
                    Log.e(this.getClass().getSimpleName(), "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                    return false;
                }
            } catch (BandException e) {
                handleBandException(e);
                return false;
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), e.getMessage());
                return false;
            }

            return true;
        }
    }

    private class PingTask extends AsyncTask<Void, Void, Boolean> {

        String title;
        String body;
        public PingTask(String title, String body)
        {
            this.title = title;
            this.body = body;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    sendMessage(title, body);
                } else {
                    Log.e(this.getClass().getSimpleName(), "Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                    return false;
                }
            } catch (BandException e) {
                handleBandException(e);
                return false;
            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), e.getMessage());
                return false;
            }

            return true;
        }
    }

    private boolean sendMessage(String title, String body) throws Exception {
        if (!doesTileExist()) {
            return false;
        }

        client.getNotificationManager().vibrate(VibrationType.ONE_TONE_HIGH);
        Thread.sleep(500);

        client.getNotificationManager().sendMessage(tileId, title, body, new Date(), MessageFlags.SHOW_DIALOG);
        Log.i(this.getClass().getSimpleName(), "notification sent to msband");

        //clear page to remove notification message
        client.getTileManager().removePages(tileId);
        updatePages();

        return true;
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                Log.e(this.getClass().getSimpleName(), "Band isn't paired with your phone.");
                return false;
            }
            client = BandClientManager.getInstance().create(activity.getApplicationContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        Log.i(this.getClass().getSimpleName(), "Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private boolean doesTileExist() throws BandIOException, InterruptedException, BandException {
        List<BandTile> tiles = client.getTileManager().getTiles().await();
        for (BandTile tile : tiles) {
            if (tile.getTileId().equals(tileId)) {
                return true;
            }
        }
        return false;
    }

    private boolean addTile() throws Exception {
        if (doesTileExist()) {
            return true;
        }

		/* Set the options */
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap tileIcon = BitmapFactory.decodeResource(activity.getApplicationContext().getResources(), R.drawable.logo_small, options);

        BandTile tile = new BandTile.Builder(tileId, tileTitle, tileIcon)
                .setPageLayouts(createButtonLayout())
                .build();

        if (client.getTileManager().addTile(activity, tile).await()) {
            Log.i(this.getClass().getSimpleName(), "Button Tile has been added to msband.");
            return true;
        } else {
            Log.e(this.getClass().getSimpleName(), "Unable to add button tile to ms band.");
            return false;
        }
    }

    private void removeTile() throws BandIOException, InterruptedException, BandException {
        if (doesTileExist()) {
            client.getTileManager().removeTile(tileId).await();
        }
    }

    private PageLayout createButtonLayout() {
        return new PageLayout(
                new FlowPanel(15, 0, 260, 105, FlowPanelOrientation.HORIZONTAL)
                        .addElements(new TextButton(0, 0, 100, 100).setMargins(5, 5, 5, 5).setId(BTN_YES).setPressedColor(Color.RED).setHorizontalAlignment(HorizontalAlignment.CENTER))
                        .addElements(new TextButton(100, 0, 100, 100).setMargins(5, 5, 5, 5).setId(BTN_NO).setPressedColor(Color.BLUE).setHorizontalAlignment(HorizontalAlignment.CENTER)));
    }

    private void updatePages() throws BandIOException {
        client.getTileManager().setPages(tileId,
                                         new PageData(pageId, 0)
                                                 .update(new TextButtonData(BTN_YES, "Start"))
                                                 .update(new TextButtonData(BTN_NO, "End")));
    }

    private void handleBandException(BandException e) {
        String exceptionMessage = "";
        switch (e.getErrorType()) {
            case DEVICE_ERROR:
                exceptionMessage = "Please make sure bluetooth is on and the band is in range.\n";
                break;
            case UNSUPPORTED_SDK_VERSION_ERROR:
                exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                break;
            case SERVICE_ERROR:
                exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                break;
            case BAND_FULL_ERROR:
                exceptionMessage = "Band is full. Please use Microsoft Health to remove a tile.\n";
                break;
            default:
                exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                break;
        }
        Log.e(this.getClass().getSimpleName(), exceptionMessage);
    }
}
