LOCAL_ROOT_PATH := $(call my-dir)
include $(CLEAR_VARS)

#Opus Lib
OPUS_DIR := opus-1.1.1
include $(LOCAL_ROOT_PATH)/opus-Android.mk
