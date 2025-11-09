package android.net;

import android.net.Network;
import android.net.TetheredClient;
import android.net.TetheringConfigurationParcel;
import android.net.TetheringCallbackStartedParcel;
import android.net.TetherStatesParcel;

/**
 * Callback class for receiving tethering changed events.
 * @hide
 */
oneway interface ITetheringEventCallback
{
    /** Called immediately after the callbacks are registered */
    void onCallbackStarted(in TetheringCallbackStartedParcel parcel);
    void onCallbackStopped(int errorCode);
    void onUpstreamChanged(in Network network);
    void onConfigurationChanged(in TetheringConfigurationParcel config);
    void onTetherStatesChanged(in TetherStatesParcel states);
    void onTetherClientsChanged(in List<TetheredClient> clients);
    void onOffloadStatusChanged(int status);
    void onSupportedTetheringTypes(long supportedBitmap);
}