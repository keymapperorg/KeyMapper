package android.net;

import android.net.IIntResultListener;
import android.net.TetheringRequestParcel;
import android.net.ITetheringEventCallback;

oneway interface ITetheringConnector {
    void startTethering(in TetheringRequestParcel request, String callerPkg,
            String callingAttributionTag, IIntResultListener receiver);

    void stopTethering(int type, String callerPkg, String callingAttributionTag,
            IIntResultListener receiver);

    void registerTetheringEventCallback(ITetheringEventCallback callback, String callerPkg);

    void unregisterTetheringEventCallback(ITetheringEventCallback callback, String callerPkg);
}