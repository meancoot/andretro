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
	typedef void (*andretro_video_refresh)(void* aOut, const void* data, unsigned width, unsigned height, size_t pitch);
	unsigned pixelFormat;

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

    static void retro_video_refresh_imp(const void *data, unsigned width, unsigned height, size_t pitch)
    {
    	static const andretro_video_refresh refreshByMode[3] = {&refresh_15, &refresh_noconv<uint32_t>, &refresh_noconv<uint16_t>};

    	if(data)
    	{
    		void* outData = (void*)env->GetDirectBufferAddress(env->GetObjectField(videoFrame, (*frame_class)["pixels"]));
    		refreshByMode[pixelFormat](outData, data, width, height, pitch);
    	}

    	env->SetIntField(videoFrame, (*frame_class)["width"], width);
    	env->SetIntField(videoFrame, (*frame_class)["height"], height);
    	env->SetIntField(videoFrame, (*frame_class)["pixelFormat"], VIDEO::pixelFormat);
    	env->SetIntField(videoFrame, (*frame_class)["rotation"], rotation);
    	env->SetFloatField(videoFrame, (*frame_class)["aspect"], avInfo.geometry.aspect_ratio);
    }
}

namespace INPUT
{
	jint keyboard[RETROK_LAST];
	jint joypads[8];

	static void retro_input_poll_imp(void)
	{
		// Joystick
		jintArray js = (jintArray)env->GetObjectField(videoFrame, (*frame_class)["buttons"]);
		env->GetIntArrayRegion(js, 0, 8, joypads);

		// Keyboard
		jintArray kbd = (jintArray)env->GetObjectField(videoFrame, (*frame_class)["keyboard"]);
		env->GetIntArrayRegion(kbd, 0, RETROK_LAST, keyboard);
	}

	static int16_t retro_input_state_imp(unsigned port, unsigned device, unsigned index, unsigned id)
	{
		switch(device)
		{
			case RETRO_DEVICE_JOYPAD:
			{
				return (joypads[port] >> id) & 1;
			}

			case RETRO_DEVICE_KEYBOARD:
			{
				return keyboard[id];
			}

			default:
			{
				return 0;
			}
		}
	}
}

namespace AUDIO
{
	jshortArray audioData;
	jint audioLength;

	void prepareFrame()
	{
		audioData = (jshortArray)env->GetObjectField(videoFrame, (*frame_class)["audio"]);
		audioLength = 0;
	}

	void endFrame()
	{
		env->SetShortField(videoFrame, (*frame_class)["audioSamples"], audioLength);
		audioLength = 0;
	}

	void retro_audio_sample_imp(int16_t left, int16_t right)
	{
		int16_t data[] = {left, right};
	    env->SetShortArrayRegion(audioData, audioLength, 2, data);
		audioLength += 2;
	}

	static size_t retro_audio_sample_batch_imp(const int16_t *data, size_t frames)
	{
	    env->SetShortArrayRegion(audioData, audioLength, frames * 2, data);
		audioLength += frames * 2;
		return frames; //TODO: ?
	}
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

		if(newFormat < 3)
		{
			VIDEO::pixelFormat = newFormat;
			return true;
		}

		return false;
	}

	return false;
}

// Renders a single audio frame. Should only be used if implementation generates a single sample at a time.
// Format is signed 16-bit native endian.

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
    module->set_video_refresh(VIDEO::retro_video_refresh_imp);
    module->set_audio_sample(AUDIO::retro_audio_sample_imp);
    module->set_audio_sample_batch(AUDIO::retro_audio_sample_batch_imp);
    module->set_input_poll(INPUT::retro_input_poll_imp);
    module->set_input_state(INPUT::retro_input_state_imp);

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

JNIFUNC(void, run)(JNIARGS, jobject aVideo, jboolean aRewind)
{
    // TODO
    env = aEnv;

    videoFrame = aVideo;
    
    AUDIO::prepareFrame();

    const bool rewound = aRewind && rewinder.eatFrame(module);

    if(!aRewind || rewound)
    {
    	module->run();

    	if(!aRewind)
    	{
    		rewinder.stashFrame(module);
    	}
    }

    AUDIO::endFrame();
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
            systemInfo_class.reset(new JavaClass(aEnv, aEnv->FindClass("org/libretro/LibRetro$SystemInfo"), sizeof(n) / sizeof(n[0]), n, s));
        }
    
        {
            static const char* const n[] = {"baseWidth", "baseHeight", "maxWidth", "maxHeight", "aspectRatio", "fps", "sampleRate"};
            static const char* const s[] = {"I", "I", "I", "I", "F", "D", "D"};
            avInfo_class.reset(new JavaClass(aEnv, aEnv->FindClass("org/libretro/LibRetro$AVInfo"), sizeof(n) / sizeof(n[0]), n, s));
        }
        
        {
        	static const char* const n[] = {"pixels", "width", "height", "pixelFormat", "rotation", "aspect", "keyboard", "buttons", "audio", "audioSamples"};
        	static const char* const s[] = {"Ljava/nio/ByteBuffer;", "I", "I", "I", "I", "F", "[I", "[I", "[S", "I"};
        	frame_class.reset(new JavaClass(aEnv, aEnv->FindClass("org/libretro/LibRetro$VideoFrame"), sizeof(n) / sizeof(n[0]), n, s));
        }

        return true;
    }
    catch(...)
    {
        return false;
    }
}
