#include <jni.h>
#include <dlfcn.h>

#include "Common.h"
#include "libretro.h"
#include "Library.h"

#include "Rewinder.h"

namespace
{
    Library* module;
    
    FileReader ROM;
    Rewinder rewinder;
    
    const char* systemDirectory;

    JNIEnv* env;
    jshortArray audioData;
    jint audioLength;
    jint joypad;

    jobject videoFrame;
    jint lastWidth;
    jint lastHeight;
    jint rotation;
    jboolean haveFrame;

    retro_system_info systemInfo;
    retro_system_av_info avInfo;
}

// Callbacks
//
// Environment callback. Gives implementations a way of performing uncommon tasks. Extensible.
static bool retro_environment_imp(unsigned cmd, void *data)
{
	if(RETRO_ENVIRONMENT_SET_ROTATION == cmd)
	{
		rotation = (*(unsigned*)data) & 3;
		return true;
	}
	else if(RETRO_ENVIRONMENT_GET_OVERSCAN == cmd)
	{
		// TODO: true causes snes9x-next to shit a brick
		*(uint8_t*)data = false;
		return true;
	}
	else if(RETRO_ENVIRONMENT_GET_CAN_DUPE == cmd)
	{
		*(uint8_t*)data = true;
		return true;
	}
	else if(RETRO_ENVIRONMENT_GET_VARIABLE == cmd)
	{
		// TODO
		return false;
	}
	else if(RETRO_ENVIRONMENT_SET_VARIABLES == cmd)
	{
		// HACK
		return false;
	}
	else if(RETRO_ENVIRONMENT_SET_MESSAGE == cmd)
	{
		// TODO
		return false;
	}
	else if(RETRO_ENVIRONMENT_SHUTDOWN == cmd)
	{
		// TODO
		return false;
	}
	else if(RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL == cmd)
	{
		// TODO
		return false;
	}
	else if(RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY == cmd)
	{
		*(const char**)data = systemDirectory;
		return true;
	}
	else if(RETRO_ENVIRONMENT_SET_PIXEL_FORMAT == cmd)
	{
		return false;
	}

	return false;
}

// Render a frame. Pixel format is 15-bit 0RGB1555 native endian unless changed (see RETRO_ENVIRONMENT_SET_PIXEL_FORMAT).
// Width and height specify dimensions of buffer.
// Pitch specifices length in bytes between two lines in buffer.
template<typename T>
void blizzit(const void* data, unsigned width, unsigned height, size_t pitch)
{
	const unsigned itersPerLine = width / (sizeof(T) / 2);

	T* outPixels = (T*)env->GetDirectBufferAddress(videoFrame);
	const uint8_t* inPixels = (uint8_t*)data;

	for(int i = 0; i != height; i ++)
	{
		const T* line = (const T*)&inPixels[i * pitch];

		for(int j = 0; j != itersPerLine; j ++)
		{
			*outPixels++ = *line++ << 1;
		}
	}
}

static void retro_video_refresh_imp(const void *data, unsigned width, unsigned height, size_t pitch)
{
	// TODO: Handle SIGBUS is not 16 or 32 bit aligned!
	// TODO: Maybe use NEON instructions?

	haveFrame = true;

	if(0 != (pitch & 3) || 0 != (((uintptr_t)data) & 3) || 0 != (width & 3))
	{
		blizzit<uint16_t>(data, width, height, pitch);
	}
	else
	{
		blizzit<uint32_t>(data, width, height, pitch);
	}
    
    lastWidth = width;
    lastHeight = height;
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

JNIFUNC(jboolean, loadLibrary)(JNIARGS, jstring path, jstring systemDir)
{
    JavaChars libname(aEnv, path);
    
    delete module;
    module = 0;
    
    try
    {
        module = new Library(libname);
        systemDirectory = strdup(JavaChars(aEnv, systemDir));
        return true;
    }
    catch(...)
    {
        return false;
    }
}

JNIFUNC(void, unloadLibrary)(JNIARGS)
{
    delete module;
    module = 0;

    memset(&systemInfo, 0, sizeof(systemInfo));

    rotation = 0;
}

JNIFUNC(void, init)(JNIARGS)
{
    module->set_environment(retro_environment_imp);
    module->set_video_refresh(retro_video_refresh_imp);
    module->set_audio_sample(retro_audio_sample_imp);
    module->set_audio_sample_batch(retro_audio_sample_batch_imp);
    module->set_input_poll(retro_input_poll_imp);
    module->set_input_state(retro_input_state_imp);

    module->init();

    module->get_system_info(&systemInfo);
}

JNIFUNC(void, deinit)(JNIARGS)
{
    module->deinit();
}

JNIFUNC(jint, apiVersion)(JNIARGS)
{
    return module->api_version();
}

JNIFUNC(void, getSystemInfo)(JNIARGS, jobject aSystemInfo)
{
	static const char* const n[] = {"libraryName", "libraryVersion", "validExtensions", "needFullPath", "blockExtract"};
	static const char* const s[] = {"Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Z", "Z"};
	static JavaClass sic(aEnv, aSystemInfo, 5, n, s);
	
    aEnv->SetObjectField(aSystemInfo, sic["libraryName"], JavaString(aEnv, systemInfo.library_name));
    aEnv->SetObjectField(aSystemInfo, sic["libraryVersion"], JavaString(aEnv, systemInfo.library_version));
    aEnv->SetObjectField(aSystemInfo, sic["validExtensions"], JavaString(aEnv, systemInfo.valid_extensions));
    aEnv->SetBooleanField(aSystemInfo, sic["needFullPath"], systemInfo.need_fullpath);
    aEnv->SetBooleanField(aSystemInfo, sic["blockExtract"], systemInfo.block_extract);
}

JNIFUNC(void, getSystemAVInfo)(JNIARGS, jobject aAVInfo)
{
    static const char* const n[] = {"baseWidth", "baseHeight", "maxWidth", "maxHeight", "aspectRatio", "fps", "sampleRate"};
    static const char* const s[] = {"I", "I", "I", "I", "F", "D", "D"};
    static JavaClass sic(aEnv, aAVInfo, 7, n, s);

    aEnv->SetIntField(aAVInfo, sic["baseWidth"], avInfo.geometry.base_width);
    aEnv->SetIntField(aAVInfo, sic["baseHeight"], avInfo.geometry.base_height);
    aEnv->SetIntField(aAVInfo, sic["maxWidth"], avInfo.geometry.max_width);
    aEnv->SetIntField(aAVInfo, sic["maxHeight"], avInfo.geometry.max_height);
    aEnv->SetFloatField(aAVInfo, sic["aspectRatio"], avInfo.geometry.aspect_ratio);
    aEnv->SetDoubleField(aAVInfo, sic["fps"], avInfo.timing.fps);
    aEnv->SetDoubleField(aAVInfo, sic["sampleRate"], avInfo.timing.sample_rate);
}

JNIFUNC(void, setControllerPortDevice)(JNIARGS, jint port, jint device)
{
    module->set_controller_port_device(port, device);
}

JNIFUNC(void, reset)(JNIARGS)
{
    module->reset();
}

JNIFUNC(jint, run)(JNIARGS, jobject aVideo, jintArray aSize, jshortArray aAudio, jint aJoypad, jboolean aRewind)
{
    // TODO
    env = aEnv;
    videoFrame = aVideo;
    audioData = aAudio;
    audioLength = 0;
    joypad = aJoypad;
    haveFrame = false;
    
    const bool rewound = aRewind && rewinder.eatFrame(module);

    if(!aRewind || rewound)
    {
    	module->run();

    	if(!aRewind)
    	{
    		rewinder.stashFrame(module);
    	}
    }

    // Upload video size (done here in case of dupe)
    const jint size[3] = {lastWidth, lastHeight, rotation};
    static const jint zeroSize[3] = {0};
    env->SetIntArrayRegion(aSize, 0, 3, haveFrame ? size : zeroSize);

    return audioLength;
}

JNIFUNC(jint, serializeSize)(JNIARGS)
{
    return module->serialize_size();
}

JNIFUNC(void, cheatReset)(JNIARGS)
{
    module->cheat_reset();
}

JNIFUNC(void, cheatSet)(JNIARGS, jint index, jboolean enabled, jstring code)
{
    JavaChars codeN(aEnv, code);
    module->cheat_set(index, enabled, codeN);
}

JNIFUNC(bool, loadGame)(JNIARGS, jstring path)
{
    JavaChars fileName(aEnv, path);
    
    retro_game_info info = {fileName, 0, 0, 0};
    
    if(!systemInfo.need_fullpath)
    {
        if(ROM.load(fileName))
        {
            info.data = ROM.base();
            info.size = ROM.size();
        }
        else
        {
            return false;
        }
    }

    if(module->load_game(&info))
    {
        module->get_system_av_info(&avInfo);

        rewinder.gameLoaded(module);
        return true;
    }
    
    return false;
}

JNIFUNC(void, unloadGame)(JNIARGS)
{
    module->unload_game();
    memset(&avInfo, 0, sizeof(avInfo));
    
    ROM.close();
}

JNIFUNC(jint, getRegion)(JNIARGS)
{
    return module->get_region();
}

JNIFUNC(jobject, getMemoryData)(JNIARGS, int aID)
{
    void* const memoryData = module->get_memory_data(aID);
    const size_t memorySize = module->get_memory_size(aID);
    
    return (memoryData && memorySize) ? aEnv->NewDirectByteBuffer(memoryData, memorySize) : 0;
}

JNIFUNC(int, getMemorySize)(JNIARGS, int aID)
{
    return module->get_memory_size(aID);
}

// Extensions: Rewinder
JNIFUNC(void, setupRewinder)(JNIARGS, int aSize)
{
	rewinder.setSize(aSize);
}

// Extensions: Read or write a memory region into a specified file.
JNIFUNC(jboolean, writeMemoryRegion)(JNIARGS, int aID, jstring aFileName)
{
    const size_t size = module->get_memory_size(aID);
    void* const data = module->get_memory_data(aID);
    
    if(size && data)
    {
        DumpFile(JavaChars(aEnv, aFileName), data, size);
    }
    
    return true;
}

JNIFUNC(jboolean, readMemoryRegion)(JNIARGS, int aID, jstring aFileName)
{
    const size_t size = module->get_memory_size(aID);
    void* const data = module->get_memory_data(aID);
    
    if(size && data)
    {
        ReadFile(JavaChars(aEnv, aFileName), data, size);
    }
    
    return true;
}

// Extensions: Serialize/Unserialize using a file
JNIFUNC(jboolean, serializeToFile)(JNIARGS, jstring aPath)
{
    const size_t size = module->serialize_size();

    if(size > 0)
    {        
        uint8_t buffer[size];
        if(module->serialize(buffer, size))
        {
            return DumpFile(JavaChars(aEnv, aPath), buffer, size);
        }
    }
    
    return false;
}

JNIFUNC(jboolean, unserializeFromFile)(JNIARGS, jstring aPath)
{
    const size_t size = module->serialize_size();
    
    if(size > 0)
    {
        uint8_t buffer[size];
        
        if(ReadFile(JavaChars(aEnv, aPath), buffer, size))
        {
            return module->unserialize(buffer, size);
        }
    }

    return false;
}
