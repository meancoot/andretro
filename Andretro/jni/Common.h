#ifndef MJNI_COMMON_H
#define MJNI_COMMON_H

#include <string>
#include <map>
#include <vector>
#include <jni.h>
#include <android/log.h>

#define Log(...) __android_log_print(ANDROID_LOG_INFO, "andretro", __VA_ARGS__);

class FileReader
{
    private:
        std::vector<uint8_t> data;

    public:
        FileReader(const char* aPath)
        {
            FILE* file = fopen(aPath, "rb");
            
            if(file)
            {
                fseek(file, 0, SEEK_END);
                data.resize(ftell(file));
                fseek(file, 0, SEEK_SET);
                
                fread(&data[0], data.size(), 1, file);
                
                fclose(file);
            }
        }
        
        const uint8_t* base() const
        {
            return &data[0];
        }
        
        size_t size() const
        {
            return data.size();
        }
};

struct JavaClass
{
	jclass classID;
	std::map<std::string, jfieldID> fields;

	JavaClass(JNIEnv* aEnv, jobject aObject, int aFieldCount, const char* const* aNames, const char* const* aSigs)
	{
		classID = aEnv->GetObjectClass(aObject);

		for(int i = 0; i != aFieldCount; i ++)
		{
			fields[aNames[i]] = aEnv->GetFieldID(classID, aNames[i], aSigs[i]);
		}
	}

	jfieldID operator[](const char* aName)
	{
		return fields[aName];
	}
};

// jstring wrapper
struct JavaChars
{
	JNIEnv* env;
	jstring string;
	const char* chars;

	JavaChars(JNIEnv* aEnv, jstring aString)
	{
		env = aEnv;
		string = aString;
		chars = env->GetStringUTFChars(string, 0);
	}

	~JavaChars()
	{
	    env->ReleaseStringUTFChars(string, chars);
	}

	operator const char*() const
	{
		return chars;
	}

	// No copy
private:
	JavaChars(const JavaChars&);
	JavaChars& operator= (const JavaChars&);

};

// Build a string with checking (later anyway)
inline jstring JavaString(JNIEnv* aEnv, const char* aString)
{
	return aEnv->NewStringUTF(aString);
}

#endif
