// Generated by jextract

package com.littlekt.wgpu;

import java.lang.invoke.*;
import java.lang.foreign.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.foreign.ValueLayout.*;
import static java.lang.foreign.MemoryLayout.PathElement.*;

/**
 * {@snippet lang=c :
 * struct WGPUDeviceDescriptor {
 *     const WGPUChainedStruct *nextInChain;
 *     const char *label;
 *     size_t requiredFeatureCount;
 *     const WGPUFeatureName *requiredFeatures;
 *     const WGPURequiredLimits *requiredLimits;
 *     WGPUQueueDescriptor defaultQueue;
 *     WGPUDeviceLostCallback deviceLostCallback;
 *     void *deviceLostUserdata;
 * }
 * }
 */
public class WGPUDeviceDescriptor {

    WGPUDeviceDescriptor() {
        // Should not be called directly
    }

    private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
        WGPU.C_POINTER.withName("nextInChain"),
        WGPU.C_POINTER.withName("label"),
        WGPU.C_LONG_LONG.withName("requiredFeatureCount"),
        WGPU.C_POINTER.withName("requiredFeatures"),
        WGPU.C_POINTER.withName("requiredLimits"),
        WGPUQueueDescriptor.layout().withName("defaultQueue"),
        WGPU.C_POINTER.withName("deviceLostCallback"),
        WGPU.C_POINTER.withName("deviceLostUserdata")
    ).withName("WGPUDeviceDescriptor");

    /**
     * The layout of this struct
     */
    public static final GroupLayout layout() {
        return $LAYOUT;
    }

    private static final AddressLayout nextInChain$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("nextInChain"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * const WGPUChainedStruct *nextInChain
     * }
     */
    public static final AddressLayout nextInChain$layout() {
        return nextInChain$LAYOUT;
    }

    private static final long nextInChain$OFFSET = 0;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * const WGPUChainedStruct *nextInChain
     * }
     */
    public static final long nextInChain$offset() {
        return nextInChain$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * const WGPUChainedStruct *nextInChain
     * }
     */
    public static MemorySegment nextInChain(MemorySegment struct) {
        return struct.get(nextInChain$LAYOUT, nextInChain$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * const WGPUChainedStruct *nextInChain
     * }
     */
    public static void nextInChain(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(nextInChain$LAYOUT, nextInChain$OFFSET, fieldValue);
    }

    private static final AddressLayout label$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("label"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * const char *label
     * }
     */
    public static final AddressLayout label$layout() {
        return label$LAYOUT;
    }

    private static final long label$OFFSET = 8;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * const char *label
     * }
     */
    public static final long label$offset() {
        return label$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * const char *label
     * }
     */
    public static MemorySegment label(MemorySegment struct) {
        return struct.get(label$LAYOUT, label$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * const char *label
     * }
     */
    public static void label(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(label$LAYOUT, label$OFFSET, fieldValue);
    }

    private static final OfLong requiredFeatureCount$LAYOUT = (OfLong)$LAYOUT.select(groupElement("requiredFeatureCount"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * size_t requiredFeatureCount
     * }
     */
    public static final OfLong requiredFeatureCount$layout() {
        return requiredFeatureCount$LAYOUT;
    }

    private static final long requiredFeatureCount$OFFSET = 16;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * size_t requiredFeatureCount
     * }
     */
    public static final long requiredFeatureCount$offset() {
        return requiredFeatureCount$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * size_t requiredFeatureCount
     * }
     */
    public static long requiredFeatureCount(MemorySegment struct) {
        return struct.get(requiredFeatureCount$LAYOUT, requiredFeatureCount$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * size_t requiredFeatureCount
     * }
     */
    public static void requiredFeatureCount(MemorySegment struct, long fieldValue) {
        struct.set(requiredFeatureCount$LAYOUT, requiredFeatureCount$OFFSET, fieldValue);
    }

    private static final AddressLayout requiredFeatures$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("requiredFeatures"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * const WGPUFeatureName *requiredFeatures
     * }
     */
    public static final AddressLayout requiredFeatures$layout() {
        return requiredFeatures$LAYOUT;
    }

    private static final long requiredFeatures$OFFSET = 24;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * const WGPUFeatureName *requiredFeatures
     * }
     */
    public static final long requiredFeatures$offset() {
        return requiredFeatures$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * const WGPUFeatureName *requiredFeatures
     * }
     */
    public static MemorySegment requiredFeatures(MemorySegment struct) {
        return struct.get(requiredFeatures$LAYOUT, requiredFeatures$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * const WGPUFeatureName *requiredFeatures
     * }
     */
    public static void requiredFeatures(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(requiredFeatures$LAYOUT, requiredFeatures$OFFSET, fieldValue);
    }

    private static final AddressLayout requiredLimits$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("requiredLimits"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * const WGPURequiredLimits *requiredLimits
     * }
     */
    public static final AddressLayout requiredLimits$layout() {
        return requiredLimits$LAYOUT;
    }

    private static final long requiredLimits$OFFSET = 32;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * const WGPURequiredLimits *requiredLimits
     * }
     */
    public static final long requiredLimits$offset() {
        return requiredLimits$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * const WGPURequiredLimits *requiredLimits
     * }
     */
    public static MemorySegment requiredLimits(MemorySegment struct) {
        return struct.get(requiredLimits$LAYOUT, requiredLimits$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * const WGPURequiredLimits *requiredLimits
     * }
     */
    public static void requiredLimits(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(requiredLimits$LAYOUT, requiredLimits$OFFSET, fieldValue);
    }

    private static final GroupLayout defaultQueue$LAYOUT = (GroupLayout)$LAYOUT.select(groupElement("defaultQueue"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * WGPUQueueDescriptor defaultQueue
     * }
     */
    public static final GroupLayout defaultQueue$layout() {
        return defaultQueue$LAYOUT;
    }

    private static final long defaultQueue$OFFSET = 40;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * WGPUQueueDescriptor defaultQueue
     * }
     */
    public static final long defaultQueue$offset() {
        return defaultQueue$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * WGPUQueueDescriptor defaultQueue
     * }
     */
    public static MemorySegment defaultQueue(MemorySegment struct) {
        return struct.asSlice(defaultQueue$OFFSET, defaultQueue$LAYOUT.byteSize());
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * WGPUQueueDescriptor defaultQueue
     * }
     */
    public static void defaultQueue(MemorySegment struct, MemorySegment fieldValue) {
        MemorySegment.copy(fieldValue, 0L, struct, defaultQueue$OFFSET, defaultQueue$LAYOUT.byteSize());
    }

    private static final AddressLayout deviceLostCallback$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("deviceLostCallback"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * WGPUDeviceLostCallback deviceLostCallback
     * }
     */
    public static final AddressLayout deviceLostCallback$layout() {
        return deviceLostCallback$LAYOUT;
    }

    private static final long deviceLostCallback$OFFSET = 56;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * WGPUDeviceLostCallback deviceLostCallback
     * }
     */
    public static final long deviceLostCallback$offset() {
        return deviceLostCallback$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * WGPUDeviceLostCallback deviceLostCallback
     * }
     */
    public static MemorySegment deviceLostCallback(MemorySegment struct) {
        return struct.get(deviceLostCallback$LAYOUT, deviceLostCallback$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * WGPUDeviceLostCallback deviceLostCallback
     * }
     */
    public static void deviceLostCallback(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(deviceLostCallback$LAYOUT, deviceLostCallback$OFFSET, fieldValue);
    }

    private static final AddressLayout deviceLostUserdata$LAYOUT = (AddressLayout)$LAYOUT.select(groupElement("deviceLostUserdata"));

    /**
     * Layout for field:
     * {@snippet lang=c :
     * void *deviceLostUserdata
     * }
     */
    public static final AddressLayout deviceLostUserdata$layout() {
        return deviceLostUserdata$LAYOUT;
    }

    private static final long deviceLostUserdata$OFFSET = 64;

    /**
     * Offset for field:
     * {@snippet lang=c :
     * void *deviceLostUserdata
     * }
     */
    public static final long deviceLostUserdata$offset() {
        return deviceLostUserdata$OFFSET;
    }

    /**
     * Getter for field:
     * {@snippet lang=c :
     * void *deviceLostUserdata
     * }
     */
    public static MemorySegment deviceLostUserdata(MemorySegment struct) {
        return struct.get(deviceLostUserdata$LAYOUT, deviceLostUserdata$OFFSET);
    }

    /**
     * Setter for field:
     * {@snippet lang=c :
     * void *deviceLostUserdata
     * }
     */
    public static void deviceLostUserdata(MemorySegment struct, MemorySegment fieldValue) {
        struct.set(deviceLostUserdata$LAYOUT, deviceLostUserdata$OFFSET, fieldValue);
    }

    /**
     * Obtains a slice of {@code arrayParam} which selects the array element at {@code index}.
     * The returned segment has address {@code arrayParam.address() + index * layout().byteSize()}
     */
    public static MemorySegment asSlice(MemorySegment array, long index) {
        return array.asSlice(layout().byteSize() * index);
    }

    /**
     * The size (in bytes) of this struct
     */
    public static long sizeof() { return layout().byteSize(); }

    /**
     * Allocate a segment of size {@code layout().byteSize()} using {@code allocator}
     */
    public static MemorySegment allocate(SegmentAllocator allocator) {
        return allocator.allocate(layout());
    }

    /**
     * Allocate an array of size {@code elementCount} using {@code allocator}.
     * The returned segment has size {@code elementCount * layout().byteSize()}.
     */
    public static MemorySegment allocateArray(long elementCount, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(elementCount, layout()));
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction} (if any).
     * The returned segment has size {@code layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, Arena arena, Consumer<MemorySegment> cleanup) {
        return reinterpret(addr, 1, arena, cleanup);
    }

    /**
     * Reinterprets {@code addr} using target {@code arena} and {@code cleanupAction} (if any).
     * The returned segment has size {@code elementCount * layout().byteSize()}
     */
    public static MemorySegment reinterpret(MemorySegment addr, long elementCount, Arena arena, Consumer<MemorySegment> cleanup) {
        return addr.reinterpret(layout().byteSize() * elementCount, arena, cleanup);
    }
}

