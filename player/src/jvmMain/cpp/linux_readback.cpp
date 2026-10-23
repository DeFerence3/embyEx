#include <jni.h>
#include <dlfcn.h>
#include <link.h>
#include <mpv/render.h>
#include <condition_variable>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <thread>
#include <unordered_map>

namespace mediampv { class mpv_handle_t; }

namespace {
// MediaMP 0.5.2 Linux x64 exports these Itanium ABI symbols. Resolve against the
// already-loaded library (including its libmpv dependency), never another system
// libmpv. No private object layout is read. Revalidate these symbols on upgrades.
struct Api {
    void* library = nullptr;
    std::shared_ptr<mediampv::mpv_handle_t> (*find)(jlong);
    mpv_handle* (*handle)(const mediampv::mpv_handle_t*);
    void (*notify)(mediampv::mpv_handle_t*);
    decltype(&mpv_render_context_create) create;
    decltype(&mpv_render_context_set_update_callback) callback;
    decltype(&mpv_render_context_update) update;
    decltype(&mpv_render_context_render) render;
    decltype(&mpv_render_context_free) release;

    template<class T> T symbol(const char* name) {
        auto result = reinterpret_cast<T>(dlsym(library, name));
        if (!result) throw std::runtime_error("Incompatible MediaMP Linux runtime.");
        return result;
    }
    Api() {
        dl_iterate_phdr([](dl_phdr_info* info, size_t, void* target) {
            std::string path(info->dlpi_name);
            if (path.ends_with("/libmediampv.so") || path == "libmediampv.so") {
                *static_cast<void**>(target) = dlopen(info->dlpi_name, RTLD_NOW | RTLD_NOLOAD);
                return 1;
            }
            return 0;
        }, &library);
        if (!library) throw std::runtime_error("MediaMP must be loaded before the renderer.");
        find = symbol<decltype(find)>("_ZN8mediampv11find_handleEl");
        handle = symbol<decltype(handle)>("_ZNK8mediampv12mpv_handle_t3mpvEv");
        notify = symbol<decltype(notify)>("_ZN8mediampv12mpv_handle_t20notify_render_updateEv");
        create = symbol<decltype(create)>("mpv_render_context_create");
        callback = symbol<decltype(callback)>("mpv_render_context_set_update_callback");
        update = symbol<decltype(update)>("mpv_render_context_update");
        render = symbol<decltype(render)>("mpv_render_context_render");
        release = symbol<decltype(release)>("mpv_render_context_free");
    }
};

Api& api() { static Api instance; return instance; }

struct FreePixels { void operator()(unsigned char* value) const { std::free(value); } };
using Pixels = std::unique_ptr<unsigned char, FreePixels>;

class Renderer {
    std::shared_ptr<mediampv::mpv_handle_t> owner;
    mpv_render_context* context = nullptr;
    std::mutex workMutex;
    std::condition_variable wake;
    bool stopping = false;
    bool pending = true;
    int width = 640;
    int height = 360;
    std::thread worker;
    std::once_flag shutdownOnce;
    std::mutex frameMutex;
    Pixels frame;
    size_t frameStride = 0;
    int frameWidth = 0;
    int frameHeight = 0;
    uint64_t frameState = 0;

    static void updated(void* opaque) {
        auto* self = static_cast<Renderer*>(opaque);
        {
            std::lock_guard lock(self->workMutex);
            self->pending = true;
        }
        self->wake.notify_one();
    }

    void run() {
        Pixels back;
        size_t capacity = 0;
        uint64_t serial = 0;
        uint64_t generation = 0;
        int lastWidth = 0;
        int lastHeight = 0;
        for (;;) {
            int w, h;
            {
                std::unique_lock lock(workMutex);
                wake.wait(lock, [&] { return stopping || pending; });
                if (stopping) break;
                pending = false;
                w = width;
                h = height;
            }
            const auto changes = api().update(context);
            const bool resized = w != lastWidth || h != lastHeight;
            if (!(changes & MPV_RENDER_UPDATE_FRAME) && !resized) continue;
            size_t stride = (size_t(w) * 4 + 63) & ~size_t(63);
            size_t bytes = stride * size_t(h);
            if (w > 0 && h > 0 && bytes > capacity) {
                void* allocation = nullptr;
                if (posix_memalign(&allocation, 64, bytes) == 0) {
                    back.reset(static_cast<unsigned char*>(allocation));
                    capacity = bytes;
                }
            }
            // Drain frames even while hidden or if allocation fails, so mpv's
            // playback/event thread never waits on a missing UI surface.
            int skip = w <= 0 || h <= 0 || !back || capacity < bytes;
            int dimensions[] = { w, h };
            char format[] = "rgb0";
            mpv_render_param parameters[] = {
                {MPV_RENDER_PARAM_SW_SIZE, dimensions},
                {MPV_RENDER_PARAM_SW_FORMAT, format},
                {MPV_RENDER_PARAM_SW_STRIDE, &stride},
                {MPV_RENDER_PARAM_SW_POINTER, back.get()},
                {MPV_RENDER_PARAM_SKIP_RENDERING, &skip},
                {MPV_RENDER_PARAM_INVALID, nullptr},
            };
            if (api().render(context, parameters) < 0 || skip) continue;
            if (resized) ++generation;
            lastWidth = w;
            lastHeight = h;
            {
                std::lock_guard lock(frameMutex);
                // Swap buffers instead of holding the frame lock while rendering.
                back.swap(frame);
                const size_t previousCapacity = frameStride * size_t(frameHeight);
                frameStride = stride;
                frameWidth = w;
                frameHeight = h;
                capacity = previousCapacity;
                frameState = ((generation & 0xFFFF) << 48) |
                    (uint64_t(w) << 30) | (uint64_t(h) << 16) | (++serial & 0xFFFF);
            }
            api().notify(owner.get());
        }
    }

public:
    explicit Renderer(jlong id) : owner(api().find(id)) {
        if (!owner) throw std::runtime_error("The player has already closed.");
        char type[] = MPV_RENDER_API_TYPE_SW;
        mpv_render_param parameters[] = {
            {MPV_RENDER_PARAM_API_TYPE, type}, {MPV_RENDER_PARAM_INVALID, nullptr},
        };
        if (api().create(&context, api().handle(owner.get()), parameters) < 0)
            throw std::runtime_error("Could not create the Linux video renderer.");
        api().callback(context, updated, this);
        try { worker = std::thread([this] { run(); }); }
        catch (...) { api().callback(context, nullptr, nullptr); api().release(context); throw; }
    }
    ~Renderer() { shutdown(); }
    void shutdown() {
        std::call_once(shutdownOnce, [&] {
            {
                std::lock_guard lock(workMutex);
                stopping = true;
            }
            wake.notify_one();
            worker.join();
            api().callback(context, nullptr, nullptr);
            api().release(context);
        });
    }
    bool resize(int w, int h) {
        if (w < 0 || h < 0 || w > 16383 || h > 16383) return false;
        {
            std::lock_guard lock(workMutex);
            if (stopping) return false;
            width = w;
            height = h;
            pending = true;
        }
        wake.notify_one();
        return true;
    }
    uint64_t state() { std::lock_guard lock(frameMutex); return frameState; }
    uint64_t copy(void* target, int w, int h) {
        std::lock_guard lock(frameMutex);
        if (!target || !frame || w != frameWidth || h != frameHeight) return 0;
        for (int row = 0; row < h; ++row)
            std::memcpy(static_cast<unsigned char*>(target) + size_t(row) * w * 4,
                        frame.get() + size_t(row) * frameStride, size_t(w) * 4);
        return frameState;
    }
};

std::mutex registryMutex;
std::unordered_map<jlong, std::shared_ptr<Renderer>> registry;
std::shared_ptr<Renderer> find(jlong id) {
    std::lock_guard lock(registryMutex);
    auto item = registry.find(id);
    return item == registry.end() ? nullptr : item->second;
}
} // namespace

#define JNI_METHOD(name) Java_app_deference_embycl_ui_player_LinuxReadbackBackend_##name
extern "C" {
JNIEXPORT jboolean JNICALL JNI_METHOD(create)(JNIEnv* env, jclass, jlong id) {
    try {
        auto renderer = std::make_shared<Renderer>(id);
        std::lock_guard lock(registryMutex);
        return registry.emplace(id, std::move(renderer)).second;
    } catch (const std::exception&) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),
                      "Could not initialize the Linux player renderer (MediaMP 0.5.2 required).");
        return false;
    }
}
JNIEXPORT void JNICALL JNI_METHOD(destroy)(JNIEnv*, jclass, jlong id) {
    std::shared_ptr<Renderer> removed;
    {
        std::lock_guard lock(registryMutex);
        auto item = registry.find(id);
        if (item == registry.end()) return;
        removed = std::move(item->second);
        registry.erase(item);
    }
    // Teardown waits for the renderer outside the registry lock.
    removed->shutdown();
}
JNIEXPORT jboolean JNICALL JNI_METHOD(resize)(JNIEnv*, jclass, jlong id, jint w, jint h) {
    auto renderer = find(id);
    return renderer && renderer->resize(w, h);
}
JNIEXPORT jlong JNICALL JNI_METHOD(state)(JNIEnv*, jclass, jlong id) {
    auto renderer = find(id);
    return renderer ? renderer->state() : 0;
}
JNIEXPORT jlong JNICALL JNI_METHOD(copy)(JNIEnv*, jclass, jlong id, jlong address, jint w, jint h) {
    auto renderer = find(id);
    return renderer ? renderer->copy(reinterpret_cast<void*>(address), w, h) : 0;
}
}
