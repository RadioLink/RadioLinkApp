#LOCAL_PATH := $(call my-dir)
LOCAL_PATH := $(LOCAL_ROOT_PATH)/$(OPUS_DIR)

include $(CLEAR_VARS)

LOCAL_MODULE        := jniopus

include $(LOCAL_PATH)/celt_sources.mk
        include $(LOCAL_PATH)/silk_sources.mk
        include $(LOCAL_PATH)/opus_sources.mk

ifeq ($(TARGET_ARCH), arm)
CELT_SOURCES += $(CELT_SOURCES_ARM)
SILK_SOURCES += $(SILK_SOURCES_ARM)
endif

# TODO: add support for floating-point?
SILK_SOURCES += $(SILK_SOURCES_FIXED)
OPUS_SOURCES += $(OPUS_SOURCES_FLOAT)
# end fixed point

LOCAL_C_INCLUDES    := \
    $(LOCAL_ROOT_PATH) \
    $(LOCAL_PATH)/src/ \
    $(LOCAL_PATH)/include/ \
    $(LOCAL_PATH)/celt/ \
    $(LOCAL_PATH)/celt/arm/ \
    $(LOCAL_PATH)/celt/x86/ \
    $(LOCAL_PATH)/silk/ \
    $(LOCAL_PATH)/silk/float/ \
    $(LOCAL_PATH)/silk/fixed/ \
    $(LOCAL_PATH)/silk/arm/

LOCAL_SRC_FILES :=  $(CELT_SOURCES) $(SILK_SOURCES) $(OPUS_SOURCES) ../jniopus.c
LOCAL_CFLAGS    := -DOPUS_BUILD -DVAR_ARRAYS -DFIXED_POINT
LOCAL_LDLIBS    := -L$(SYSROOT)/usr/lib -llog -lz -landroid
LOCAL_CPPFLAGS  := -fexceptions

include $(BUILD_SHARED_LIBRARY)
include $(CLEAR_VARS)
