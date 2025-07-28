/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /Users/sethd/Library/Android/sdk/build-tools/35.0.0/aidl --lang=ndk -o /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/cpp/aidl -h /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/cpp -I /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/aidl /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/aidl/io/github/sds100/keymapper/sysbridge/IEvdevCallback.aidl
 */
#pragma once

#include "aidl/io/github/sds100/keymapper/sysbridge/IEvdevCallback.h"

#include <android/binder_ibinder.h>
#include <cassert>

#ifndef __BIONIC__
#ifndef __assert2
#define __assert2(a,b,c,d) ((void)0)
#endif
#endif

namespace aidl {
namespace io {
namespace github {
namespace sds100 {
namespace keymapper {
namespace sysbridge {
class BnEvdevCallback : public ::ndk::BnCInterface<IEvdevCallback> {
public:
  BnEvdevCallback();
  virtual ~BnEvdevCallback();
protected:
  ::ndk::SpAIBinder createBinder() override;
private:
};
class IEvdevCallbackDelegator : public BnEvdevCallback {
public:
  explicit IEvdevCallbackDelegator(const std::shared_ptr<IEvdevCallback> &impl) : _impl(impl) {
  }

  ::ndk::ScopedAStatus onEvdevEvent(int32_t in_deviceId, int64_t in_timeSec, int64_t in_timeUsec, int32_t in_type, int32_t in_code, int32_t in_value, int32_t in_androidCode) override {
    return _impl->onEvdevEvent(in_deviceId, in_timeSec, in_timeUsec, in_type, in_code, in_value, in_androidCode);
  }
protected:
private:
  std::shared_ptr<IEvdevCallback> _impl;
};

}  // namespace sysbridge
}  // namespace keymapper
}  // namespace sds100
}  // namespace github
}  // namespace io
}  // namespace aidl
