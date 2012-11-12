#include <jni.h>
#include <dlfcn.h>
#include <memory>

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
    jint rotation;

    retro_system_info systemInfo;
    retro_system_av_info avInfo;

    std::auto_ptr<JavaClass> avInfo_class;
    std::auto_ptr<JavaClass> systemInfo_class;
    std::auto_ptr<JavaClass> frame_class;
}

namespace VIDEO
{
    template<typename T>
    static void refresh_noconv(void* aOut, const void *data, unsigned width, unsigned height, size_t pitch)
    {
        T* outPixels = (T*)aOut;
        const T* inPixels = (const T*)data;
        const unsigned pixelPitch = pitch / sizeof(T);

        for(int i = 0; i != height; i ++, outPixels += width, inPixels += pixelPitch)
        {
            memcpy(outPixels, inPixels, width * sizeof(T));
        }
    }

    // retro_video_refresh for 0RGB1555: deprecated
    static void refresh_15(void* aOut, const void *data, unsigned width, unsigned height, size_t pitch)
    {
        const unsigned pixelPitch = pitch / 2;

        uint16_t* outPixels = (uint16_t*)aOut;
        const uint16_t* inPixels = (const uint16_t*)data;

        for(int i = 0; i != height; i ++)
        {
            for(int j = 0; j != width; j ++)
            {
                *outPixels++ = (*inPixels++) << 1;
            }

            inPixels += (pixelPitch - width);
        }
    }

    unsigned pixelFormat;

    typedef void (*andretro_video_refresh)(void* aOut, const void* data, unsigned width, unsigned height, size_t pitch);

    const andretro_video_refresh refreshByMode[3] = {&refresh_15, &refresh_noconv<uint32_t>, &refresh_noconv<uint16_t>};
    andretro_video_refresh refresher = refresh_15;
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
		unsigned newFormat = *(unsigned*)data;

		if(newFormat == 0 || newFormat == 2)
		{
			VIDEO::pixelFormat = newFormat;
			VIDEO::refresher = VIDEO::refreshByMode[VIDEO::pixelFormat];
			return true;
		}

		return false;
	}

	return false;
}

// Render a frame. Pixel format is 15-bit 0RGB1555 native endian unless changed (see RETRO_ENVIRONMENT_SET_PIXEL_FORMAT).
// Width and height specify dimensions of buffer.
// Pitch specifices length in bytes between two lines in buffer.
static void retro_video_refresh_imp(const void *data, unsigned width, unsigned height, size_t pitch)
{
	if(data)
	{
		void* outData = (void*)env->GetDirectBufferAddress(env->GetObjectField(videoFrame, (*frame_class)["pixels"]));
		VIDEO::refresher(outData, data, width, height, pitch);
	}

	env->SetIntField(videoFrame, (*frame_class)["width"], width);
	env->SetIntField(videoFrame, (*frame_class)["height"], height);
	env->SetIntField(videoFrame, (*frame_class)["pixelFormat"], VIDEO::pixelFormat);
	env->SetIntField(videoFrame, (*frame_class)["rotation"], rotation);
	env->SetFloatField(videoFrame, (*frame_class)["aspect"], avInfo.geometry.aspect_ratio);
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
#define JNIFUNC(RET, FUNCTION) extern "C" RET Java_org_libretro_LibRetro_ ## FUNCTION
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
    aEnv->SetObjectField(aSystemInfo, (*systemInfo_class)["libraryName"], JavaString(aEnv, systemInfo.library_name));
    aEnv->SetObjectField(aSystemInfo, (*systemInfo_class)["libraryVersion"], JavaString(aEnv, systemInfo.library_version));
    aEnv->SetObjectField(aSystemInfo, (*systemInfo_class)["validExtensions"], JavaString(aEnv, systemInfo.valid_extensions));
    aEnv->SetBooleanField(aSystemInfo, (*systemInfo_class)["needFullPath"], systemInfo.need_fullpath);
    aEnv->SetBooleanField(aSystemInfo, (*systemInfo_class)["blockExtract"], systemInfo.block_extract);
}

JNIFUNC(void, getSystemAVInfo)(JNIARGS, jobject aAVInfo)
{
    aEnv->SetIntField(aAVInfo, (*avInfo_class)["baseWidth"], avInfo.geometry.base_width);
    aEnv->SetIntField(aAVInfo, (*avInfo_class)["baseHeight"], avInfo.geometry.base_height);
    aEnv->SetIntField(aAVInfo, (*avInfo_class)["maxWidth"], avInfo.geometry.max_width);
    aEnv->SetIntField(aAVInfo, (*avInfo_class)["maxHeight"], avInfo.geometry.max_height);
    aEnv->SetFloatField(aAVInfo, (*avInfo_class)["aspectRatio"], avInfo.geometry.aspect_ratio);
    aEnv->SetDoubleField(aAVInfo, (*avInfo_class)["fps"], avInfo.timing.fps);
    aEnv->SetDoubleField(aAVInfo, (*avInfo_class)["sampleRate"], avInfo.timing.sample_rate);
}

JNIFUNC(void, setControllerPortDevice)(JNIARGS, jint port, jint device)
{
    module->set_controller_port_device(port, device);
}

JNIFUNC(void, reset)(JNIARGS)
{
    module->reset();
}

JNIFUNC(jint, run)(JNIARGS, jobject aVideo, jshortArray aAudio, jint aJoypad, jboolean aRewind)
{
    // TODO
    env = aEnv;

    videoFrame = aVideo;
    audioData = aAudio;
    audioLength = 0;
    joypad = aJoypad;
    
    const bool rewound = aRewind && rewinder.eatFrame(module);

    if(!aRewind || rewound)
    {
    	module->run();

    	if(!aRewind)
    	{
    		rewinder.stashFrame(module);
    	}
    }

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

// Preload native class data
JNIFUNC(jboolean, nativeInit)(JNIARGS)
{
    try
    {
        {
            static const char* const n[] = {"libraryName", "libraryVersion", "validExtensions", "needFullPath", "blockExtract"};
            static const char* const s[] = {"Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;", "Z", "Z"};
            systemInfo_class.reset(new JavaClass(aEnv, aEnv->FindClass("org/libretro/LibRetro$SystemInfo"), 5, n, s));
        }
    
        {
            static const char* const n[] = {"baseWidth", "baseHeight", "maxWidth", "maxHeight", "aspectRatio", "fps", "sampleRate"};
            static const char* const s[] = {"I", "I", "I", "I", "F", "D", "D"};
            avInfo_class.reset(new JavaClass(aEnv, aEnv->FindClass("org/libretro/LibRetro$AVInfo"), 7, n, s));
        }
        
        {
        	static const char* const n[] = {"pixels", "width", "height", "pixelFormat", "rotation", "aspect"};
        	static const char* const s[] = {"Ljava/nio/ByteBuffer;", "I", "I", "I", "I", "F"};
        	frame_class.reset(new JavaClass(aEnv, aEnv->FindClass("org/libretro/LibRetro$VideoFrame"), 6, n, s));
        }

        return true;
    }
    catch(...)
    {
        return false;
    }
}
