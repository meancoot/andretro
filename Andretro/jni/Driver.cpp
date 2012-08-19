#include <jni.h>
#include <dlfcn.h>

#include "Common.h"
#include "libretro.h"

namespace
{
    void* handle;
    JNIEnv* env;
    jobject videoFrame;
    jintArray videoSize;
    jshortArray audioData;
    jint audioLength;
    jint joypad;
}

#define DEFINEFUNCTION(RET, NAME, ...) \
 typedef RET (*NAME##_t)(__VA_ARGS__); \
 static NAME##_t NAME##_ptr
 
#define LOADFUNCTION(NAME) \
 NAME##_ptr = (NAME##_t)dlsym(handle, #NAME); \
 if(0 == NAME##_ptr){Log(dlerror()); return false;}
 
DEFINEFUNCTION(void, retro_set_environment, retro_environment_t);
DEFINEFUNCTION(void, retro_set_video_refresh, retro_video_refresh_t);
DEFINEFUNCTION(void, retro_set_audio_sample, retro_audio_sample_t);
DEFINEFUNCTION(void, retro_set_audio_sample_batch, retro_audio_sample_batch_t);
DEFINEFUNCTION(void, retro_set_input_poll, retro_input_poll_t);
DEFINEFUNCTION(void, retro_set_input_state, retro_input_state_t);
DEFINEFUNCTION(void, retro_init, void);
DEFINEFUNCTION(void, retro_deinit, void);
DEFINEFUNCTION(unsigned, retro_api_version, void);
DEFINEFUNCTION(void, retro_get_system_info, struct retro_system_info *info);
DEFINEFUNCTION(void, retro_get_system_av_info, struct retro_system_av_info *info);
DEFINEFUNCTION(void, retro_set_controller_port_device, unsigned port, unsigned device);
DEFINEFUNCTION(void, retro_reset, void);
DEFINEFUNCTION(void, retro_run, void);
DEFINEFUNCTION(size_t, retro_serialize_size, void);
DEFINEFUNCTION(bool, retro_serialize, void *data, size_t size);
DEFINEFUNCTION(bool, retro_unserialize, const void *data, size_t size);
DEFINEFUNCTION(void, retro_cheat_reset, void);
DEFINEFUNCTION(void, retro_cheat_set, unsigned index, bool enabled, const char *code);
DEFINEFUNCTION(bool, retro_load_game, const struct retro_game_info *game);
DEFINEFUNCTION(bool, retro_load_game_special, unsigned game_type, const struct retro_game_info *info, size_t num_info);
DEFINEFUNCTION(void, retro_unload_game, void);
DEFINEFUNCTION(unsigned, retro_get_region, void);
DEFINEFUNCTION(void*, retro_get_memory_data, unsigned id);
DEFINEFUNCTION(size_t, retro_get_memory_size, unsigned id);


// Callbacks
//
// Environment callback. Gives implementations a way of performing uncommon tasks. Extensible.
static bool retro_environment_imp(unsigned cmd, void *data)
{
    return false;
}

// Render a frame. Pixel format is 15-bit 0RGB1555 native endian unless changed (see RETRO_ENVIRONMENT_SET_PIXEL_FORMAT).
// Width and height specify dimensions of buffer.
// Pitch specifices length in bytes between two lines in buffer.
static void retro_video_refresh_imp(const void *data, unsigned width, unsigned height, size_t pitch)
{
	uint16_t* outPixels = (uint16_t*)env->GetDirectBufferAddress(videoFrame);
    const uint8_t* inPixels = (uint8_t*)data;
    
    for(int i = 0; i != height; i ++)
    {
        const uint16_t* line = (const uint16_t*)&inPixels[i * pitch];
    
        for(int j = 0; j != width; j ++)
        {
            *outPixels++ = *line++ << 1;
        }
    }
    
    uint32_t size[2] = {width, height};
    env->SetIntArrayRegion(videoSize, 0, 2, (const int*)size);
}

// Renders a single audio frame. Should only be used if implementation generates a single sample at a time.
// Format is signed 16-bit native endian.
void retro_audio_sample_imp(int16_t left, int16_t right)
{
	int16_t data[] = {left, right};
    env->SetShortArrayRegion(audioData, audioLength, 2, data);
	audioLength += 2;
}

// Renders multiple audio frames in one go. One frame is defined as a sample of left and right channels, interleaved.
// I.e. int16_t buf[4] = { l, r, l, r }; would be 2 frames.
// Only one of the audio callbacks must ever be used.
static size_t retro_audio_sample_batch_imp(const int16_t *data, size_t frames)
{
    env->SetShortArrayRegion(audioData, audioLength, frames * 2, data);
	audioLength += frames * 2;
	return frames; //< ?
}

// Polls input.
static void retro_input_poll_imp(void)
{

}

// Queries for input for player 'port'. device will be masked with RETRO_DEVICE_MASK.
// Specialization of devices such as RETRO_DEVICE_JOYPAD_MULTITAP that have been set with retro_set_controller_port_device()
// will still use the higher level RETRO_DEVICE_JOYPAD to request input.
static int16_t retro_input_state_imp(unsigned port, unsigned device, unsigned index, unsigned id)
{
	if(0 == port && 1 == device)
	{
		return (joypad >> id) & 1;
	}

    return 0;
}

//
#define JNIFUNC(RET, FUNCTION) extern "C" RET Java_ ## org_libretro_LibRetro_ ## FUNCTION
#define JNIARGS JNIEnv* aEnv, jclass aClass

JNIFUNC(jboolean, loadLibrary)(JNIARGS, jstring path)
{
    JavaChars libname(aEnv, path);
    handle = dlopen(libname, RTLD_LAZY);
    
    if(0 == handle)
    {
    	Log(dlerror());
    	return false;
    }

    LOADFUNCTION(retro_set_environment);
    LOADFUNCTION(retro_set_video_refresh);
    LOADFUNCTION(retro_set_audio_sample);
    LOADFUNCTION(retro_set_audio_sample_batch);
    LOADFUNCTION(retro_set_input_poll);
    LOADFUNCTION(retro_set_input_state);
    LOADFUNCTION(retro_init);
    LOADFUNCTION(retro_deinit);
    LOADFUNCTION(retro_api_version);
    LOADFUNCTION(retro_get_system_info);
    LOADFUNCTION(retro_get_system_av_info);
    LOADFUNCTION(retro_set_controller_port_device);
    LOADFUNCTION(retro_reset);
    LOADFUNCTION(retro_run);
    LOADFUNCTION(retro_serialize_size);
    LOADFUNCTION(retro_serialize);
    LOADFUNCTION(retro_unserialize);
    LOADFUNCTION(retro_cheat_reset);
    LOADFUNCTION(retro_cheat_set);
    LOADFUNCTION(retro_load_game);
    LOADFUNCTION(retro_load_game_special);
    LOADFUNCTION(retro_unload_game);
    LOADFUNCTION(retro_get_region);
    LOADFUNCTION(retro_get_memory_data);
    LOADFUNCTION(retro_get_memory_size);

    return true;
}

JNIFUNC(void, init)(JNIARGS)
{
    retro_set_environment_ptr(retro_environment_imp);
    retro_set_video_refresh_ptr(retro_video_refresh_imp);
    retro_set_audio_sample_ptr(retro_audio_sample_imp);
    retro_set_audio_sample_batch_ptr(retro_audio_sample_batch_imp);
    retro_set_input_poll_ptr(retro_input_poll_imp);
    retro_set_input_state_ptr(retro_input_state_imp);

    retro_init_ptr();
}

JNIFUNC(void, deinit)(JNIARGS)
{
    retro_deinit_ptr();
}

JNIFUNC(jint, apiVersion)(JNIARGS)
{
    return retro_api_version_ptr();
}

JNIFUNC(void, getSystemInfo)(JNIARGS, jobject aSystemInfo)
{
    retro_system_info info;
    retro_get_system_info_ptr(&info);

	static const char* const n[] = {"libraryName", "libraryVersion", "validExtensions", "needFullPath", "blockExtract"};
	static const char* const s[] = {"Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Z", "Z"};
	static JavaClass sic(aEnv, aSystemInfo, 5, n, s);
	
    aEnv->SetObjectField(aSystemInfo, sic["libraryName"], JavaString(aEnv, info.library_name));
    aEnv->SetObjectField(aSystemInfo, sic["libraryVersion"], JavaString(aEnv, info.library_version));
    aEnv->SetObjectField(aSystemInfo, sic["validExtensions"], JavaString(aEnv, info.valid_extensions));
    aEnv->SetBooleanField(aSystemInfo, sic["needFullPath"], info.need_fullpath);
    aEnv->SetBooleanField(aSystemInfo, sic["blockExtract"], info.block_extract);
}

JNIFUNC(void, getSystemAVInfo)(JNIARGS, jobject aAVInfo)
{
    retro_system_av_info info;
    retro_get_system_av_info_ptr(&info);

    static const char* const n[] = {"baseWidth", "baseHeight", "maxWidth", "maxHeight", "aspectRatio", "fps", "sampleRate"};
    static const char* const s[] = {"I", "I", "I", "I", "F", "D", "D"};
    static JavaClass sic(aEnv, aAVInfo, 7, n, s);

    aEnv->SetIntField(aAVInfo, sic["baseWidth"], info.geometry.base_width);
    aEnv->SetIntField(aAVInfo, sic["baseHeight"], info.geometry.base_height);
    aEnv->SetIntField(aAVInfo, sic["maxWidth"], info.geometry.max_width);
    aEnv->SetIntField(aAVInfo, sic["maxHeight"], info.geometry.max_height);
    aEnv->SetFloatField(aAVInfo, sic["aspectRatio"], info.geometry.aspect_ratio);
    aEnv->SetDoubleField(aAVInfo, sic["fps"], info.timing.fps);
    aEnv->SetDoubleField(aAVInfo, sic["sampleRate"], info.timing.sample_rate);
}

JNIFUNC(void, setControllerPortDevice)(JNIARGS, jint port, jint device)
{
    retro_set_controller_port_device_ptr(port, device);
}

JNIFUNC(void, reset)(JNIARGS)
{
    retro_reset_ptr();
}

JNIFUNC(jint, run)(JNIARGS, jobject aVideo, jintArray aSize, jshortArray aAudio, jint aJoypad)
{
    // TODO
    env = aEnv;
    videoFrame = aVideo;
    videoSize = aSize;
    audioData = aAudio;
    audioLength = 0;
    joypad = aJoypad;
    
    retro_run_ptr();
    
    return audioLength;
}

JNIFUNC(jint, serializeSize)(JNIARGS)
{
    return retro_serialize_size_ptr();
}

JNIFUNC(jboolean, serialize)(JNIARGS, jobject aData, jint aSize, jint aOffset)
{
    uint8_t* data = (uint8_t*)env->GetDirectBufferAddress(aData);
    return retro_serialize_ptr(&data[aOffset], aSize - aOffset);
}

JNIFUNC(jboolean, unserialize)(JNIARGS, jobject aData, jint aSize, jint aOffset)
{
    const uint8_t* data = (uint8_t*)env->GetDirectBufferAddress(aData);
    return retro_unserialize_ptr(&data[aOffset], aSize - aOffset);
}

JNIFUNC(jboolean, serializeToFile)(JNIARGS, jstring aPath)
{
    size_t size = retro_serialize_size_ptr();

    if(size > 0)
    {        
        uint8_t buffer[size];

        if(retro_serialize_ptr(buffer, size))
        {
            JavaChars path(aEnv, aPath);
            FILE* file = fopen(path, "wb");
            
            if(file)
            {
                bool result = (1 == fwrite(buffer, size, 1, file));
                fclose(file);                
                return result;
            }
        }
    }
    
    return false;
}

JNIFUNC(jboolean, unserializeFromFile)(JNIARGS, jstring aPath)
{
    JavaChars path(aEnv, aPath);
    
    FileReader data = FileReader(path);

    if(data.size() >= retro_serialize_size_ptr())
    {
        return retro_unserialize_ptr(data.base(), data.size());
    }
    
    return false;
}

JNIFUNC(void, cheatReset)(JNIARGS)
{
    retro_cheat_reset_ptr();
}

JNIFUNC(void, cheatSet)(JNIARGS, jint index, jboolean enabled, jstring code)
{
    JavaChars codeN(aEnv, code);
    retro_cheat_set_ptr(index, enabled, codeN);
}

JNIFUNC(bool, loadGame)(JNIARGS, jstring path)
{
    //TODO: Cleanup
    JavaChars fileName(aEnv, path);
    
    retro_game_info info;

    info.path = fileName;
    
    FILE* file = fopen(fileName, "rb");
    fseek(file, 0, SEEK_END);
    info.size = ftell(file);
    fseek(file, 0, SEEK_SET);
    
    info.data = malloc(info.size);
    fread((void*)info.data, info.size, 1, file);
    fclose(file);
    info.meta = 0;

    return retro_load_game_ptr(&info);
}

JNIFUNC(void, unloadGame)(JNIARGS)
{
    retro_unload_game_ptr();
}

JNIFUNC(jint, getRegion)(JNIARGS)
{
    return retro_get_region_ptr();
}

// Gets region of memory.
//void *retro_get_memory_data(unsigned id);
//size_t retro_get_memory_size(unsigned id);
