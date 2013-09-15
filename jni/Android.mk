LOCAL_PATH := $(call my-dir)

#
# FMOD Ex Shared Library
# 
include $(CLEAR_VARS)

LOCAL_MODULE            := fmodex
LOCAL_SRC_FILES         := ../../../fmodapi44201android/api/lib/$(TARGET_ARCH_ABI)/libfmodex.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../../../fmodapi44201android/api/inc

include $(PREBUILT_SHARED_LIBRARY)

#
# FMOD Ex Shared Library
# 
include $(CLEAR_VARS)

LOCAL_MODULE            := bass
LOCAL_SRC_FILES         := ../../../fmodapi44201android/api/lib/$(TARGET_ARCH_ABI)/libbass.so

include $(PREBUILT_SHARED_LIBRARY)

#
# Example Library
#
include $(CLEAR_VARS)

LOCAL_MODULE           := main
LOCAL_SRC_FILES        := main.c
LOCAL_LDLIBS 		   := -llog
LOCAL_SHARED_LIBRARIES := fmodex

include $(BUILD_SHARED_LIBRARY)
