/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /Users/sethd/Library/Android/sdk/build-tools/35.0.0/aidl --lang=ndk -o /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/cpp/aidl -h /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/cpp -I /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/aidl /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/aidl/io/github/sds100/keymapper/sysbridge/IEvdevCallback.aidl
 */
#pragma once

#include "aidl/io/github/sds100/keymapper/sysbridge/IEvdevCallback.h"

#include <android/binder_ibinder.h>

namespace aidl {
namespace io {
namespace github {
namespace sds100 {
namespace keymapper {
namespace sysbridge {
class BpEvdevCallback : public ::ndk::BpCInterface<IEvdevCallback> {
public:
  explicit BpEvdevCallback(const ::ndk::SpAIBinder& binder);
  virtual ~BpEvdevCallback();

  ::ndk::ScopedAStatus onEvdevEventLoopStarted() override;
  ::ndk::ScopedAStatus onEvdevEvent(const std::string& in_devicePath, int64_t in_timeSec, int64_t in_timeUsec, int32_t in_type, int32_t in_code, int32_t in_value, int32_t in_androidCode, bool* _aidl_return) override;

    ::ndk::ScopedAStatus onEmergencyKillSystemBridge() override;
};
}  // namespace sysbridge
}  // namespace keymapper
}  // namespace sds100
}  // namespace github
}  // namespace io
}  // namespace aidl
