LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := retro_gambatte
LOCAL_SRC_FILES := libretro_gambatte.so
include $(PREBUILT_SHARED_LIBRARY)
