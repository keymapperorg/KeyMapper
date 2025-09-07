package com.android.internal.telephony;

interface ITelephony {
  // Requires Android 12+
  void setDataEnabledForReason(int subId, int reason, boolean enable, String callingPackage);

  // Max Android 11
  void setUserDataEnabled(int subId, boolean enable);
}