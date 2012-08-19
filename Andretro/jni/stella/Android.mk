LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := retro_stella
LOCAL_SRC_FILES := libretro_stella.so
include $(PREBUILT_SHARED_LIBRARY)
