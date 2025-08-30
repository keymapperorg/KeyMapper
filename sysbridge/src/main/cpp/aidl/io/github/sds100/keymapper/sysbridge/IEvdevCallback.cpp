/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /Users/sethd/Library/Android/sdk/build-tools/35.0.0/aidl --lang=ndk -o /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/cpp/aidl -h /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/cpp -I /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/aidl /Users/sethd/Projects/KeyMapper/foss/sysbridge/src/main/aidl/io/github/sds100/keymapper/sysbridge/IEvdevCallback.aidl
 */
#include "aidl/io/github/sds100/keymapper/sysbridge/IEvdevCallback.h"

#include <android/binder_parcel_utils.h>
#include <aidl/io/github/sds100/keymapper/sysbridge/BnEvdevCallback.h>
#include <aidl/io/github/sds100/keymapper/sysbridge/BpEvdevCallback.h>

namespace aidl {
namespace io {
namespace github {
namespace sds100 {
namespace keymapper {
namespace sysbridge {
static binder_status_t _aidl_io_github_sds100_keymapper_sysbridge_IEvdevCallback_onTransact(AIBinder* _aidl_binder, transaction_code_t _aidl_code, const AParcel* _aidl_in, AParcel* _aidl_out) {
  (void)_aidl_in;
  (void)_aidl_out;
  binder_status_t _aidl_ret_status = STATUS_UNKNOWN_TRANSACTION;
  std::shared_ptr<BnEvdevCallback> _aidl_impl = std::static_pointer_cast<BnEvdevCallback>(::ndk::ICInterface::asInterface(_aidl_binder));
  switch (_aidl_code) {
    case (FIRST_CALL_TRANSACTION + 0 /*onEvdevEventLoopStarted*/): {

      ::ndk::ScopedAStatus _aidl_status = _aidl_impl->onEvdevEventLoopStarted();
      _aidl_ret_status = STATUS_OK;
      break;
    }
    case (FIRST_CALL_TRANSACTION + 1 /*onEvdevEvent*/): {
      std::string in_devicePath;
      int64_t in_timeSec;
      int64_t in_timeUsec;
      int32_t in_type;
      int32_t in_code;
      int32_t in_value;
      int32_t in_androidCode;
      bool _aidl_return;

      _aidl_ret_status = ::ndk::AParcel_readData(_aidl_in, &in_devicePath);
      if (_aidl_ret_status != STATUS_OK) break;

      _aidl_ret_status = ::ndk::AParcel_readData(_aidl_in, &in_timeSec);
      if (_aidl_ret_status != STATUS_OK) break;

      _aidl_ret_status = ::ndk::AParcel_readData(_aidl_in, &in_timeUsec);
      if (_aidl_ret_status != STATUS_OK) break;

      _aidl_ret_status = ::ndk::AParcel_readData(_aidl_in, &in_type);
      if (_aidl_ret_status != STATUS_OK) break;

      _aidl_ret_status = ::ndk::AParcel_readData(_aidl_in, &in_code);
      if (_aidl_ret_status != STATUS_OK) break;

      _aidl_ret_status = ::ndk::AParcel_readData(_aidl_in, &in_value);
      if (_aidl_ret_status != STATUS_OK) break;

      _aidl_ret_status = ::ndk::AParcel_readData(_aidl_in, &in_androidCode);
      if (_aidl_ret_status != STATUS_OK) break;

      ::ndk::ScopedAStatus _aidl_status = _aidl_impl->onEvdevEvent(in_devicePath, in_timeSec, in_timeUsec, in_type, in_code, in_value, in_androidCode, &_aidl_return);
      _aidl_ret_status = AParcel_writeStatusHeader(_aidl_out, _aidl_status.get());
      if (_aidl_ret_status != STATUS_OK) break;

      if (!AStatus_isOk(_aidl_status.get())) break;

      _aidl_ret_status = ::ndk::AParcel_writeData(_aidl_out, _aidl_return);
      if (_aidl_ret_status != STATUS_OK) break;

      break;
    }
    case (FIRST_CALL_TRANSACTION + 2 /*onEmergencyKillSystemBridge*/): {

      ::ndk::ScopedAStatus _aidl_status = _aidl_impl->onEmergencyKillSystemBridge();
      _aidl_ret_status = AParcel_writeStatusHeader(_aidl_out, _aidl_status.get());
      if (_aidl_ret_status != STATUS_OK) break;

      if (!AStatus_isOk(_aidl_status.get())) break;

      break;
    }
  }
  return _aidl_ret_status;
}

static AIBinder_Class* _g_aidl_io_github_sds100_keymapper_sysbridge_IEvdevCallback_clazz = ::ndk::ICInterface::defineClass(IEvdevCallback::descriptor, _aidl_io_github_sds100_keymapper_sysbridge_IEvdevCallback_onTransact);

BpEvdevCallback::BpEvdevCallback(const ::ndk::SpAIBinder& binder) : BpCInterface(binder) {}
BpEvdevCallback::~BpEvdevCallback() {}

::ndk::ScopedAStatus BpEvdevCallback::onEvdevEventLoopStarted() {
  binder_status_t _aidl_ret_status = STATUS_OK;
  ::ndk::ScopedAStatus _aidl_status;
  ::ndk::ScopedAParcel _aidl_in;
  ::ndk::ScopedAParcel _aidl_out;

  _aidl_ret_status = AIBinder_prepareTransaction(asBinder().get(), _aidl_in.getR());
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_ret_status = AIBinder_transact(
    asBinder().get(),
    (FIRST_CALL_TRANSACTION + 0 /*onEvdevEventLoopStarted*/),
    _aidl_in.getR(),
    _aidl_out.getR(),
    FLAG_ONEWAY
    #ifdef BINDER_STABILITY_SUPPORT
    | FLAG_PRIVATE_LOCAL
    #endif  // BINDER_STABILITY_SUPPORT
    );
  if (_aidl_ret_status == STATUS_UNKNOWN_TRANSACTION && IEvdevCallback::getDefaultImpl()) {
    _aidl_status = IEvdevCallback::getDefaultImpl()->onEvdevEventLoopStarted();
    goto _aidl_status_return;
  }
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_error:
  _aidl_status.set(AStatus_fromStatus(_aidl_ret_status));
  _aidl_status_return:
  return _aidl_status;
}
::ndk::ScopedAStatus BpEvdevCallback::onEvdevEvent(const std::string& in_devicePath, int64_t in_timeSec, int64_t in_timeUsec, int32_t in_type, int32_t in_code, int32_t in_value, int32_t in_androidCode, bool* _aidl_return) {
  binder_status_t _aidl_ret_status = STATUS_OK;
  ::ndk::ScopedAStatus _aidl_status;
  ::ndk::ScopedAParcel _aidl_in;
  ::ndk::ScopedAParcel _aidl_out;

  _aidl_ret_status = AIBinder_prepareTransaction(asBinder().get(), _aidl_in.getR());
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_ret_status = ::ndk::AParcel_writeData(_aidl_in.get(), in_devicePath);
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_ret_status = ::ndk::AParcel_writeData(_aidl_in.get(), in_timeSec);
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_ret_status = ::ndk::AParcel_writeData(_aidl_in.get(), in_timeUsec);
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_ret_status = ::ndk::AParcel_writeData(_aidl_in.get(), in_type);
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_ret_status = ::ndk::AParcel_writeData(_aidl_in.get(), in_code);
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_ret_status = ::ndk::AParcel_writeData(_aidl_in.get(), in_value);
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_ret_status = ::ndk::AParcel_writeData(_aidl_in.get(), in_androidCode);
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_ret_status = AIBinder_transact(
    asBinder().get(),
    (FIRST_CALL_TRANSACTION + 1 /*onEvdevEvent*/),
    _aidl_in.getR(),
    _aidl_out.getR(),
    0
    #ifdef BINDER_STABILITY_SUPPORT
    | FLAG_PRIVATE_LOCAL
    #endif  // BINDER_STABILITY_SUPPORT
    );
  if (_aidl_ret_status == STATUS_UNKNOWN_TRANSACTION && IEvdevCallback::getDefaultImpl()) {
    _aidl_status = IEvdevCallback::getDefaultImpl()->onEvdevEvent(in_devicePath, in_timeSec, in_timeUsec, in_type, in_code, in_value, in_androidCode, _aidl_return);
    goto _aidl_status_return;
  }
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_ret_status = AParcel_readStatusHeader(_aidl_out.get(), _aidl_status.getR());
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  if (!AStatus_isOk(_aidl_status.get())) goto _aidl_status_return;
  _aidl_ret_status = ::ndk::AParcel_readData(_aidl_out.get(), _aidl_return);
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_error:
  _aidl_status.set(AStatus_fromStatus(_aidl_ret_status));
  _aidl_status_return:
  return _aidl_status;
}
::ndk::ScopedAStatus BpEvdevCallback::onEmergencyKillSystemBridge() {
  binder_status_t _aidl_ret_status = STATUS_OK;
  ::ndk::ScopedAStatus _aidl_status;
  ::ndk::ScopedAParcel _aidl_in;
  ::ndk::ScopedAParcel _aidl_out;

  _aidl_ret_status = AIBinder_prepareTransaction(asBinder().get(), _aidl_in.getR());
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_ret_status = AIBinder_transact(
    asBinder().get(),
    (FIRST_CALL_TRANSACTION + 2 /*onEmergencyKillSystemBridge*/),
    _aidl_in.getR(),
    _aidl_out.getR(),
    0
    #ifdef BINDER_STABILITY_SUPPORT
    | FLAG_PRIVATE_LOCAL
    #endif  // BINDER_STABILITY_SUPPORT
    );
  if (_aidl_ret_status == STATUS_UNKNOWN_TRANSACTION && IEvdevCallback::getDefaultImpl()) {
    _aidl_status = IEvdevCallback::getDefaultImpl()->onEmergencyKillSystemBridge();
    goto _aidl_status_return;
  }
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  _aidl_ret_status = AParcel_readStatusHeader(_aidl_out.get(), _aidl_status.getR());
  if (_aidl_ret_status != STATUS_OK) goto _aidl_error;

  if (!AStatus_isOk(_aidl_status.get())) goto _aidl_status_return;
  _aidl_error:
  _aidl_status.set(AStatus_fromStatus(_aidl_ret_status));
  _aidl_status_return:
  return _aidl_status;
}
// Source for BnEvdevCallback
BnEvdevCallback::BnEvdevCallback() {}
BnEvdevCallback::~BnEvdevCallback() {}
::ndk::SpAIBinder BnEvdevCallback::createBinder() {
  AIBinder* binder = AIBinder_new(_g_aidl_io_github_sds100_keymapper_sysbridge_IEvdevCallback_clazz, static_cast<void*>(this));
  #ifdef BINDER_STABILITY_SUPPORT
  AIBinder_markCompilationUnitStability(binder);
  #endif  // BINDER_STABILITY_SUPPORT
  return ::ndk::SpAIBinder(binder);
}
// Source for IEvdevCallback
const char* IEvdevCallback::descriptor = "io.github.sds100.keymapper.sysbridge.IEvdevCallback";
IEvdevCallback::IEvdevCallback() {}
IEvdevCallback::~IEvdevCallback() {}


std::shared_ptr<IEvdevCallback> IEvdevCallback::fromBinder(const ::ndk::SpAIBinder& binder) {
  if (!AIBinder_associateClass(binder.get(), _g_aidl_io_github_sds100_keymapper_sysbridge_IEvdevCallback_clazz)) {
    #if __ANDROID_API__ >= 31
    const AIBinder_Class* originalClass = AIBinder_getClass(binder.get());
    if (originalClass == nullptr) return nullptr;
    if (0 == strcmp(AIBinder_Class_getDescriptor(originalClass), descriptor)) {
      return ::ndk::SharedRefBase::make<BpEvdevCallback>(binder);
    }
    #endif
    return nullptr;
  }
  std::shared_ptr<::ndk::ICInterface> interface = ::ndk::ICInterface::asInterface(binder.get());
  if (interface) {
    return std::static_pointer_cast<IEvdevCallback>(interface);
  }
  return ::ndk::SharedRefBase::make<BpEvdevCallback>(binder);
}

binder_status_t IEvdevCallback::writeToParcel(AParcel* parcel, const std::shared_ptr<IEvdevCallback>& instance) {
  return AParcel_writeStrongBinder(parcel, instance ? instance->asBinder().get() : nullptr);
}
binder_status_t IEvdevCallback::readFromParcel(const AParcel* parcel, std::shared_ptr<IEvdevCallback>* instance) {
  ::ndk::SpAIBinder binder;
  binder_status_t status = AParcel_readStrongBinder(parcel, binder.getR());
  if (status != STATUS_OK) return status;
  *instance = IEvdevCallback::fromBinder(binder);
  return STATUS_OK;
}
bool IEvdevCallback::setDefaultImpl(const std::shared_ptr<IEvdevCallback>& impl) {
  // Only one user of this interface can use this function
  // at a time. This is a heuristic to detect if two different
  // users in the same process use this function.
  assert(!IEvdevCallback::default_impl);
  if (impl) {
    IEvdevCallback::default_impl = impl;
    return true;
  }
  return false;
}
const std::shared_ptr<IEvdevCallback>& IEvdevCallback::getDefaultImpl() {
  return IEvdevCallback::default_impl;
}
std::shared_ptr<IEvdevCallback> IEvdevCallback::default_impl = nullptr;
::ndk::ScopedAStatus IEvdevCallbackDefault::onEvdevEventLoopStarted() {
  ::ndk::ScopedAStatus _aidl_status;
  _aidl_status.set(AStatus_fromStatus(STATUS_UNKNOWN_TRANSACTION));
  return _aidl_status;
}
::ndk::ScopedAStatus IEvdevCallbackDefault::onEvdevEvent(const std::string& /*in_devicePath*/, int64_t /*in_timeSec*/, int64_t /*in_timeUsec*/, int32_t /*in_type*/, int32_t /*in_code*/, int32_t /*in_value*/, int32_t /*in_androidCode*/, bool* /*_aidl_return*/) {
  ::ndk::ScopedAStatus _aidl_status;
  _aidl_status.set(AStatus_fromStatus(STATUS_UNKNOWN_TRANSACTION));
  return _aidl_status;
}
::ndk::ScopedAStatus IEvdevCallbackDefault::onEmergencyKillSystemBridge() {
  ::ndk::ScopedAStatus _aidl_status;
  _aidl_status.set(AStatus_fromStatus(STATUS_UNKNOWN_TRANSACTION));
  return _aidl_status;
}
::ndk::SpAIBinder IEvdevCallbackDefault::asBinder() {
  return ::ndk::SpAIBinder();
}
bool IEvdevCallbackDefault::isRemote() {
  return false;
}
}  // namespace sysbridge
}  // namespace keymapper
}  // namespace sds100
}  // namespace github
}  // namespace io
}  // namespace aidl
