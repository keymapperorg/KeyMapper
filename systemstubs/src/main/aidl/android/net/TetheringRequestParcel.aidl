package android.net;

import android.net.LinkAddress;

parcelable TetheringRequestParcel {
    int tetheringType;
    LinkAddress localIPv4Address;
    LinkAddress staticClientAddress;
    boolean exemptFromEntitlementCheck;
    boolean showProvisioningUi;
    int connectivityScope;
}