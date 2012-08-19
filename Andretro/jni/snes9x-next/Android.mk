LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := retro_snes9xnext
LOCAL_SRC_FILES := libretro_snes9xnext.so
include $(PREBUILT_SHARED_LIBRARY)
