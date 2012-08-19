LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := retro_genesis
LOCAL_SRC_FILES := libretro_genesis.so
include $(PREBUILT_SHARED_LIBRARY)
