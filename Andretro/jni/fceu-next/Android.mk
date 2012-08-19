LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := retro_fceu
LOCAL_SRC_FILES := libretro_fceu.so
include $(PREBUILT_SHARED_LIBRARY)
