package io.github.sds100.keymapper.priv;

interface IPrivService {
   // Destroy method defined by Shizuku server. This is required
   // for Shizuku user services.
   // See demo/service/UserService.java in the Shizuku-API repository.
   void destroy() = 16777114;

   String sendEvent() = 1;
}