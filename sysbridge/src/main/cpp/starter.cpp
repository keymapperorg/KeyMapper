#include <cstdio>
#include <cstdlib>
#include <unistd.h>
#include <dirent.h>
#include <ctime>
#include <cstring>
#include <libgen.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <cerrno>
#include <string_view>
#include <termios.h>
#include "android.h"
#include "misc.h"
#include "selinux.h"
#include "cgroup.h"
#include "logging.h"

#ifdef DEBUG
#define JAVA_DEBUGGABLE
#endif

#define perrorf(...) fprintf(stderr, __VA_ARGS__)

#define EXIT_FATAL_SET_CLASSPATH 3
#define EXIT_FATAL_FORK 4
#define EXIT_FATAL_APP_PROCESS 5
#define EXIT_FATAL_UID 6
#define EXIT_FATAL_PM_PATH 7
#define EXIT_FATAL_KILL 9
#define EXIT_FATAL_BINDER_BLOCKED_BY_SELINUX 10

#define SERVER_NAME "keymapper_sysbridge"
#define SERVER_CLASS_PATH "io.github.sds100.keymapper.sysbridge.service.SystemBridge"

#if defined(__arm__)
#define ABI "armeabi-v7a"
#elif defined(__i386__)
#define ABI "x86"
#elif defined(__x86_64__)
#define ABI "x86_64"
#elif defined(__aarch64__)
#define ABI "arm64-v8a"
#endif

static void run_server(const char *apk_path, const char *lib_path, const char *main_class,
                       const char *process_name, const char *package_name,
                       const char *version_code) {
    if (setenv("CLASSPATH", apk_path, true)) {
        LOGE("can't set CLASSPATH\n");
        exit(EXIT_FATAL_SET_CLASSPATH);
    }

#define ARG(v) char **v = nullptr; \
    char buf_##v[PATH_MAX]; \
    size_t v_size = 0; \
    uintptr_t v_current = 0;
#define ARG_PUSH(v, arg) v_size += sizeof(char *); \
if (v == nullptr) { \
    v = (char **) malloc(v_size); \
} else { \
    v = (char **) realloc(v, v_size);\
} \
v_current = (uintptr_t) v + v_size - sizeof(char *); \
*((char **) v_current) = arg ? strdup(arg) : nullptr;

#define ARG_END(v) ARG_PUSH(v, nullptr)

#define ARG_PUSH_FMT(v, fmt, ...) snprintf(buf_##v, PATH_MAX, fmt, __VA_ARGS__); \
    ARG_PUSH(v, buf_##v)

#ifdef JAVA_DEBUGGABLE
#define ARG_PUSH_DEBUG_ONLY(v, arg) ARG_PUSH(v, arg)
#define ARG_PUSH_DEBUG_VM_PARAMS(v) \
    if (android::GetApiLevel() >= 30) { \
        ARG_PUSH(v, "-Xcompiler-option"); \
        ARG_PUSH(v, "--debuggable"); \
        ARG_PUSH(v, "-XjdwpProvider:adbconnection"); \
        ARG_PUSH(v, "-XjdwpOptions:suspend=n,server=y"); \
    } else if (android::GetApiLevel() >= 28) { \
        ARG_PUSH(v, "-Xcompiler-option"); \
        ARG_PUSH(v, "--debuggable"); \
        ARG_PUSH(v, "-XjdwpProvider:internal"); \
        ARG_PUSH(v, "-XjdwpOptions:transport=dt_android_adb,suspend=n,server=y"); \
    } else { \
        ARG_PUSH(v, "-Xcompiler-option"); \
        ARG_PUSH(v, "--debuggable"); \
        ARG_PUSH(v, "-agentlib:jdwp=transport=dt_android_adb,suspend=n,server=y"); \
    }
#else
#define ARG_PUSH_DEBUG_VM_PARAMS(v)
#define ARG_PUSH_DEBUG_ONLY(v, arg)
#endif

    ARG(argv)
    ARG_PUSH(argv, "/system/bin/app_process")
    ARG_PUSH_FMT(argv, "-Djava.class.path=%s", apk_path)
    ARG_PUSH_FMT(argv, "-Dkeymapper_sysbridge.library.path=%s", lib_path)
    ARG_PUSH_FMT(argv, "-Dkeymapper_sysbridge.package=%s", package_name)
    ARG_PUSH_FMT(argv, "-Dkeymapper_sysbridge.version=%s", version_code)
    ARG_PUSH_DEBUG_VM_PARAMS(argv)
    ARG_PUSH(argv, "/system/bin")
    ARG_PUSH_FMT(argv, "--nice-name=%s", process_name)
    ARG_PUSH(argv, main_class)
    ARG_PUSH_DEBUG_ONLY(argv, "--debug")
    ARG_END(argv)

    LOGD("exec app_process");

    if (execvp((const char *) argv[0], argv)) {
        exit(EXIT_FATAL_APP_PROCESS);
    }
}

static void start_server(const char *apk_path, const char *lib_path, const char *main_class,
                         const char *process_name, const char *package_name,
                         const char *version_code) {

    if (daemon(false, false) == 0) {
        LOGD("child");
        run_server(apk_path, lib_path, main_class, process_name, package_name, version_code);
    } else {
        LOGE("fatal: can't fork");
        exit(EXIT_FATAL_FORK);
    }
}

static int check_selinux(const char *s, const char *t, const char *c, const char *p) {
    int res = se::selinux_check_access(s, t, c, p, nullptr);
#ifndef DEBUG
    if (res != 0) {
#endif
    LOGI("selinux_check_access %s %s %s %s: %d", s, t, c, p, res);
#ifndef DEBUG
    }
#endif
    return res;
}

static int switch_cgroup() {
    int s_cuid, s_cpid;
    int spid = getpid();

    if (cgroup::get_cgroup(spid, &s_cuid, &s_cpid) != 0) {
        LOGW("can't read cgroup");
        return -1;
    }

    LOGI("cgroup is /uid_%d/pid_%d", s_cuid, s_cpid);

    if (cgroup::switch_cgroup(spid, -1, -1) != 0) {
        LOGW("can't switch cgroup");
        return -1;
    }

    if (cgroup::get_cgroup(spid, &s_cuid, &s_cpid) != 0) {
        LOGI("switch cgroup succeeded");
        return 0;
    }

    LOGW("can't switch self, current cgroup is /uid_%d/pid_%d", s_cuid, s_cpid);
    return -1;
}

char *context = nullptr;

int starter_main(int argc, char *argv[]) {
    char *apk_path = nullptr;
    char *lib_path = nullptr;
    char *package_name = nullptr;
    char *version = nullptr;

    // Get the apk path from the program arguments. This gets the path by setting the
    // start of the apk path array to after the "--apk=" by offsetting by 6 characters.
    for (int i = 0; i < argc; ++i) {
        if (strncmp(argv[i], "--apk=", 6) == 0) {
            apk_path = argv[i] + 6;
        } else if (strncmp(argv[i], "--lib=", 6) == 0) {
            lib_path = argv[i] + 6;
        } else if (strncmp(argv[i], "--package=", 10) == 0) {
            package_name = argv[i] + 10;
        } else if (strncmp(argv[i], "--version=", 10) == 0) {
            version = argv[i] + 10;
        }
    }

    LOGI("apk path = %s", apk_path);
    LOGI("lib path = %s", lib_path);
    LOGI("package name = %s", package_name);
    LOGI("version = %s", version);

    int uid = getuid();
    if (uid != 0 && uid != 2000) {
        LOGE("fatal: run system bridge from non root nor adb user (uid=%d).", uid);
        exit(EXIT_FATAL_UID);
    }

    se::init();

    if (uid == 0) {
        chown("/data/local/tmp/keymapper_sysbridge_starter", 2000, 2000);
        se::setfilecon("/data/local/tmp/keymapper_sysbridge_starter",
                       "u:object_r:shell_data_file:s0");
        switch_cgroup();

        int sdkLevel = 0;
        char buf[PROP_VALUE_MAX + 1];
        if (__system_property_get("ro.build.version.sdk", buf) > 0)
            sdkLevel = atoi(buf);

        if (sdkLevel >= 29) {
            LOGI("switching mount namespace to init...");
            switch_mnt_ns(1);
        }
    }

    if (uid == 0) {
        if (se::getcon(&context) == 0) {
            int res = 0;

            res |= check_selinux("u:r:untrusted_app:s0", context, "binder", "call");
            res |= check_selinux("u:r:untrusted_app:s0", context, "binder", "transfer");

            if (res != 0) {
                LOGE("fatal: the su you are using does not allow app (u:r:untrusted_app:s0) to connect to su (%s) with binder.",
                        context);
                exit(EXIT_FATAL_BINDER_BLOCKED_BY_SELINUX);
            }
            se::freecon(context);
        }
    }

    LOGI("starter begin");

    // kill old server
    LOGI("killing old process...");

    foreach_proc([](pid_t pid) {
        if (pid == getpid()) return;

        char name[1024];
        if (get_proc_name(pid, name, 1024) != 0) return;

        if (strcmp(SERVER_NAME, name) != 0) return;

        if (kill(pid, SIGKILL) == 0)
            LOGI("killed %d (%s)", pid, name);
        else if (errno == EPERM) {
            LOGE("fatal: can't kill %d, please try to stop existing sysbridge from app first.",
                    pid);
            exit(EXIT_FATAL_KILL);
        } else {
            LOGW("failed to kill %d (%s)", pid, name);
        }
    });

    if (access(apk_path, R_OK) == 0) {
        LOGI("use apk path from argv");
    }

    if (access(lib_path, R_OK) == 0) {
        LOGI("use lib path from argv");
    }

    if (!apk_path) {
        LOGE("fatal: can't get path of manager");
        exit(EXIT_FATAL_PM_PATH);
    }

    if (!lib_path) {
        LOGE("fatal: can't get path of native libraries");
        exit(EXIT_FATAL_PM_PATH);
    }

    if (access(apk_path, R_OK) != 0) {
        LOGE("fatal: can't access manager %s", apk_path);
        exit(EXIT_FATAL_PM_PATH);
    }

    LOGI("starting server...");
    LOGD("start_server");
    start_server(apk_path, lib_path, SERVER_CLASS_PATH, SERVER_NAME, package_name, version);
    exit(EXIT_SUCCESS);
}

using main_func = int (*)(int, char *[]);

static main_func applet_main[] = {starter_main, nullptr};

int main(int argc, char **argv) {
    std::string_view base = basename(argv[0]);

    LOGD("applet %s", base.data());

    constexpr const char *applet_names[] = {"keymapper_sysbridge_starter", nullptr};

    for (int i = 0; applet_names[i]; ++i) {
        if (base == applet_names[i]) {
            return (*applet_main[i])(argc, argv);
        }
    }

    return 1;
}
