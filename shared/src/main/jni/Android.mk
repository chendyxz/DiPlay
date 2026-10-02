LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := xcertplay_i2c
ifeq ($(DIPLAY_KITKAT),true)
LOCAL_ARM_NEON := false
endif
LOCAL_SRC_FILES := linux_i2c_jni.c
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := local_hotspot_radio
ifeq ($(DIPLAY_KITKAT),true)
LOCAL_ARM_NEON := false
endif
LOCAL_SRC_FILES := local_hotspot_radio.c
LOCAL_CFLAGS := -Wall -Wextra -Werror
include $(BUILD_SHARED_LIBRARY)
