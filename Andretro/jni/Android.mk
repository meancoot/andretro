LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_CPP_FEATURES := exceptions rtti

LOCAL_SRC_FILES = Driver.cpp

LOCAL_MODULE    := retro_wrap
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)

# Copy local path as each sub make will change it
FOCAL_PATH := $(LOCAL_PATH)
include $(FOCAL_PATH)/gambatte/Android.mk
include $(FOCAL_PATH)/stella/Android.mk
include $(FOCAL_PATH)/snes9x-next/Android.mk
include $(FOCAL_PATH)/Genesis-Plus-GX/Android.mk
include $(FOCAL_PATH)/fceu-next/Android.mk
