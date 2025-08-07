/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /Users/sethd/Library/Android/sdk/build-tools/35.0.0/aidl --lang=ndk -o /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/cpp/aidl -h /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/cpp -I /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/aidl /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/aidl/io/github/sds100/keymapper/sysbridge/IEvdevCallback.aidl
 */
#pragma once

#include <cstdint>
#include <memory>
#include <optional>
#include <string>
#include <vector>
#include <android/binder_interface_utils.h>
#ifdef BINDER_STABILITY_SUPPORT
#include <android/binder_stability.h>
#endif  // BINDER_STABILITY_SUPPORT

namespace aidl {
namespace io {
namespace github {
namespace sds100 {
namespace keymapper {
namespace sysbridge {
class IEvdevCallbackDelegator;

class IEvdevCallback : public ::ndk::ICInterface {
public:
  typedef IEvdevCallbackDelegator DefaultDelegator;
  static const char* descriptor;
  IEvdevCallback();
  virtual ~IEvdevCallback();

  static constexpr uint32_t TRANSACTION_onEvdevEventLoopStarted = FIRST_CALL_TRANSACTION + 0;
  static constexpr uint32_t TRANSACTION_onEvdevEvent = FIRST_CALL_TRANSACTION + 1;

  static std::shared_ptr<IEvdevCallback> fromBinder(const ::ndk::SpAIBinder& binder);
  static binder_status_t writeToParcel(AParcel* parcel, const std::shared_ptr<IEvdevCallback>& instance);
  static binder_status_t readFromParcel(const AParcel* parcel, std::shared_ptr<IEvdevCallback>* instance);
  static bool setDefaultImpl(const std::shared_ptr<IEvdevCallback>& impl);
  static const std::shared_ptr<IEvdevCallback>& getDefaultImpl();
  virtual ::ndk::ScopedAStatus onEvdevEventLoopStarted() = 0;
  virtual ::ndk::ScopedAStatus onEvdevEvent(int32_t in_deviceId, int64_t in_timeSec, int64_t in_timeUsec, int32_t in_type, int32_t in_code, int32_t in_value, int32_t in_androidCode) = 0;
private:
  static std::shared_ptr<IEvdevCallback> default_impl;
};
class IEvdevCallbackDefault : public IEvdevCallback {
public:
  ::ndk::ScopedAStatus onEvdevEventLoopStarted() override;
  ::ndk::ScopedAStatus onEvdevEvent(int32_t in_deviceId, int64_t in_timeSec, int64_t in_timeUsec, int32_t in_type, int32_t in_code, int32_t in_value, int32_t in_androidCode) override;
  ::ndk::SpAIBinder asBinder() override;
  bool isRemote() override;
};
}  // namespace sysbridge
}  // namespace keymapper
}  // namespace sds100
}  // namespace github
}  // namespace io
}  // namespace aidl
