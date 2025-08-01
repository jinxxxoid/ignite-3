/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import sun.misc.Unsafe;

/**
 * Wrapper for the {@link sun.misc.Unsafe} class.
 *
 * <p>All memory access operations have the following properties:
 * <ul>
 * <li>All {@code putXxx(long addr, xxx val)}, {@code getXxx(long addr)}, {@code putXxx(byte[] arr, long off, xxx val)},
 * {@code getXxx(byte[] arr, long off)} and corresponding methods with {@code LE} suffix are alignment aware
 * and can be safely used with unaligned pointers.</li>
 * <li>All {@code putXxxField(Object obj, long fieldOff, xxx val)} and {@code getXxxField(Object obj, long fieldOff)}
 * methods are not alignment aware and can't be safely used with unaligned pointers. These methods can be safely used
 * for object field values access because all object fields addresses are aligned.</li>
 * <li>All {@code putXxxLE(...)} and {@code getXxxLE(...)} methods assumes that the byte order is fixed as little-endian
 * while native byte order is big-endian. So it is client code responsibility to check native byte order before
 * invoking these methods.</li>
 * </ul>
 */
public abstract class GridUnsafe {
    /** Native byte order. */
    public static final ByteOrder NATIVE_BYTE_ORDER = ByteOrder.nativeOrder();

    /** Unsafe. */
    static final Unsafe UNSAFE = unsafe();

    /** Page size. */
    private static final int PAGE_SIZE = UNSAFE.pageSize();

    /** Empty page. */
    private static final byte[] EMPTY_PAGE = new byte[PAGE_SIZE];

    /** Unaligned flag. */
    private static final boolean UNALIGNED = unaligned();

    /** Per-byte copy threshold. */
    private static final long PER_BYTE_THRESHOLD = 0L;

    /** Flag indicating whether system's native order is big endian. */
    public static final boolean IS_BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

    /** Address size. */
    public static final int ADDR_SIZE = UNSAFE.addressSize();

    /** {@code byte} array offset. */
    public static final long BYTE_ARR_OFF = UNSAFE.arrayBaseOffset(byte[].class);

    /** {@code byte} array offset. */
    public static final int BYTE_ARR_INT_OFF = UNSAFE.arrayBaseOffset(byte[].class);

    /** {@code short} array offset. */
    public static final long SHORT_ARR_OFF = UNSAFE.arrayBaseOffset(short[].class);

    /** {@code int} array offset. */
    public static final long INT_ARR_OFF = UNSAFE.arrayBaseOffset(int[].class);

    /** {@code long} array offset. */
    public static final long LONG_ARR_OFF = UNSAFE.arrayBaseOffset(long[].class);

    /** {@code float} array offset. */
    public static final long FLOAT_ARR_OFF = UNSAFE.arrayBaseOffset(float[].class);

    /** {@code double} array offset. */
    public static final long DOUBLE_ARR_OFF = UNSAFE.arrayBaseOffset(double[].class);

    /** {@code char} array offset. */
    public static final long CHAR_ARR_OFF = UNSAFE.arrayBaseOffset(char[].class);

    /** {@code boolean} array offset. */
    public static final long BOOLEAN_ARR_OFF = UNSAFE.arrayBaseOffset(boolean[].class);

    /** {@link java.nio.Buffer#address} field offset. */
    private static final long DIRECT_BUF_ADDR_OFF = bufferAddressOffset();

    /**
     * Ensure singleton.
     */
    private GridUnsafe() {
        // No-op.
    }

    /**
     * Wraps a pointer to unmanaged memory into a direct byte buffer.
     *
     * @param ptr Pointer to wrap.
     * @param len Memory location length.
     * @return Byte buffer wrapping the given memory.
     */
    public static ByteBuffer wrapPointer(long ptr, int len) {
        return PointerWrapping.wrapPointer(ptr, len);
    }


    /**
     * Returns allocated direct buffer.
     *
     * @param len Length.
     * @return Allocated direct buffer.
     */
    public static ByteBuffer allocateBuffer(int len) {
        long ptr = allocateMemory(len);

        return wrapPointer(ptr, len);
    }

    /**
     * Frees buffer.
     *
     * @param buf Direct buffer allocated by {@link #allocateBuffer(int)}.
     */
    public static void freeBuffer(ByteBuffer buf) {
        long ptr = bufferAddress(buf);

        freeMemory(ptr);
    }

    /**
     * Returns reallocated direct buffer.
     *
     * @param buf Buffer.
     * @param len New length.
     * @return Reallocated direct buffer.
     */
    public static ByteBuffer reallocateBuffer(ByteBuffer buf, int len) {
        long ptr = bufferAddress(buf);

        long newPtr = reallocateMemory(ptr, len);

        return wrapPointer(newPtr, len);
    }

    /**
     * Gets a boolean value from an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @return Boolean value from object field.
     */
    public static boolean getBooleanField(Object obj, long fieldOff) {
        return UNSAFE.getBoolean(obj, fieldOff);
    }

    /**
     * Stores a boolean value into an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @param val      Value.
     */
    public static void putBooleanField(Object obj, long fieldOff, boolean val) {
        UNSAFE.putBoolean(obj, fieldOff, val);
    }

    /**
     * Gets a byte value from an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @return Byte value from object field.
     */
    public static byte getByteField(Object obj, long fieldOff) {
        return UNSAFE.getByte(obj, fieldOff);
    }

    /**
     * Stores a byte value into an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @param val      Value.
     */
    public static void putByteField(Object obj, long fieldOff, byte val) {
        UNSAFE.putByte(obj, fieldOff, val);
    }

    /**
     * Gets a short value from an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @return Short value from object field.
     */
    public static short getShortField(Object obj, long fieldOff) {
        return UNSAFE.getShort(obj, fieldOff);
    }

    /**
     * Stores a short value into an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @param val      Value.
     */
    public static void putShortField(Object obj, long fieldOff, short val) {
        UNSAFE.putShort(obj, fieldOff, val);
    }

    /**
     * Gets a char value from an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @return Char value from object field.
     */
    public static char getCharField(Object obj, long fieldOff) {
        return UNSAFE.getChar(obj, fieldOff);
    }

    /**
     * Stores a char value into an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @param val      Value.
     */
    public static void putCharField(Object obj, long fieldOff, char val) {
        UNSAFE.putChar(obj, fieldOff, val);
    }

    /**
     * Gets an integer value from an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @return Integer value from object field.
     */
    public static int getIntField(Object obj, long fieldOff) {
        return UNSAFE.getInt(obj, fieldOff);
    }

    /**
     * Stores an integer value into an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @param val      Value.
     */
    public static void putIntField(Object obj, long fieldOff, int val) {
        UNSAFE.putInt(obj, fieldOff, val);
    }

    /**
     * Gets a long value from an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @return Long value from object field.
     */
    public static long getLongField(Object obj, long fieldOff) {
        return UNSAFE.getLong(obj, fieldOff);
    }

    /**
     * Stores a long value into an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @param val      Value.
     */
    public static void putLongField(Object obj, long fieldOff, long val) {
        UNSAFE.putLong(obj, fieldOff, val);
    }

    /**
     * Gets a float value from an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @return Float value from object field.
     */
    public static float getFloatField(Object obj, long fieldOff) {
        return UNSAFE.getFloat(obj, fieldOff);
    }

    /**
     * Stores a float value into an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @param val      Value.
     */
    public static void putFloatField(Object obj, long fieldOff, float val) {
        UNSAFE.putFloat(obj, fieldOff, val);
    }

    /**
     * Gets a double value from an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @return Double value from object field.
     */
    public static double getDoubleField(Object obj, long fieldOff) {
        return UNSAFE.getDouble(obj, fieldOff);
    }

    /**
     * Stores a double value into an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @param val      Value.
     */
    public static void putDoubleField(Object obj, long fieldOff, double val) {
        UNSAFE.putDouble(obj, fieldOff, val);
    }

    /**
     * Gets a reference from an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @return Reference from object field.
     */
    public static Object getObjectField(Object obj, long fieldOff) {
        return UNSAFE.getObject(obj, fieldOff);
    }

    /**
     * Stores a reference value into an object field.
     *
     * @param obj      Object.
     * @param fieldOff Field offset.
     * @param val      Value.
     */
    public static void putObjectField(Object obj, long fieldOff, Object val) {
        UNSAFE.putObject(obj, fieldOff, val);
    }

    /**
     * Gets a boolean value from a byte array.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @return Boolean value from byte array.
     */
    public static boolean getBoolean(byte[] arr, long off) {
        return UNSAFE.getBoolean(arr, off);
    }

    /**
     * Stores a boolean value into a byte array.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putBoolean(byte[] arr, long off, boolean val) {
        UNSAFE.putBoolean(arr, off, val);
    }

    /**
     * Gets a byte value from a byte array.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @return Byte value from byte array.
     */
    public static byte getByte(byte[] arr, long off) {
        return UNSAFE.getByte(arr, off);
    }

    /**
     * Gets byte value from given address.
     *
     * @param addr Address.
     * @return Byte value from given address.
     */
    public static byte getByte(long addr) {
        return UNSAFE.getByte(addr);
    }

    /**
     * Stores a byte value into a byte array.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putByte(byte[] arr, long off, byte val) {
        UNSAFE.putByte(arr, off, val);
    }

    /**
     * Stores given byte value.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putByte(long addr, byte val) {
        UNSAFE.putByte(addr, val);
    }

    /**
     * Gets a short value from a byte array. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @return Short value from byte array.
     */
    public static short getShort(byte[] arr, long off) {
        return UNALIGNED ? UNSAFE.getShort(arr, off) : getShortByByte(arr, off, IS_BIG_ENDIAN);
    }

    /**
     * Gets short value from given address. Alignment aware.
     *
     * @param addr Address.
     * @return Short value from given address.
     */
    public static short getShort(long addr) {
        return UNALIGNED ? UNSAFE.getShort(addr) : getShortByByte(addr, IS_BIG_ENDIAN);
    }

    /**
     * Stores a short value into a byte array. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putShort(byte[] arr, long off, short val) {
        if (UNALIGNED) {
            UNSAFE.putShort(arr, off, val);
        } else {
            putShortByByte(arr, off, val, IS_BIG_ENDIAN);
        }
    }

    /**
     * Stores given short value. Alignment aware.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putShort(long addr, short val) {
        if (UNALIGNED) {
            UNSAFE.putShort(addr, val);
        } else {
            putShortByByte(addr, val, IS_BIG_ENDIAN);
        }
    }

    /**
     * Gets a char value from a byte array. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @return Char value from byte array.
     */
    public static char getChar(byte[] arr, long off) {
        return UNALIGNED ? UNSAFE.getChar(arr, off) : getCharByByte(arr, off, IS_BIG_ENDIAN);
    }

    /**
     * Gets char value from given address. Alignment aware.
     *
     * @param addr Address.
     * @return Char value from given address.
     */
    public static char getChar(long addr) {
        return UNALIGNED ? UNSAFE.getChar(addr) : getCharByByte(addr, IS_BIG_ENDIAN);
    }

    /**
     * Stores a char value into a byte array. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putChar(byte[] arr, long off, char val) {
        if (UNALIGNED) {
            UNSAFE.putChar(arr, off, val);
        } else {
            putCharByByte(arr, off, val, IS_BIG_ENDIAN);
        }
    }

    /**
     * Stores given char value. Alignment aware.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putChar(long addr, char val) {
        if (UNALIGNED) {
            UNSAFE.putChar(addr, val);
        } else {
            putCharByByte(addr, val, IS_BIG_ENDIAN);
        }
    }

    /**
     * Gets an integer value from a byte array. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @return Integer value from byte array.
     */
    public static int getInt(byte[] arr, long off) {
        return UNALIGNED ? UNSAFE.getInt(arr, off) : getIntByByte(arr, off, IS_BIG_ENDIAN);
    }

    /**
     * Gets integer value from given address. Alignment aware.
     *
     * @param addr Address.
     * @return Integer value from given address.
     */
    public static int getInt(long addr) {
        return UNALIGNED ? UNSAFE.getInt(addr) : getIntByByte(addr, IS_BIG_ENDIAN);
    }

    /**
     * Stores an integer value into a byte array. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putInt(byte[] arr, long off, int val) {
        if (UNALIGNED) {
            UNSAFE.putInt(arr, off, val);
        } else {
            putIntByByte(arr, off, val, IS_BIG_ENDIAN);
        }
    }

    /**
     * Stores given integer value. Alignment aware.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putInt(long addr, int val) {
        if (UNALIGNED) {
            UNSAFE.putInt(addr, val);
        } else {
            putIntByByte(addr, val, IS_BIG_ENDIAN);
        }
    }

    /**
     * Gets a long value from a byte array. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @return Long value from byte array.
     */
    public static long getLong(byte[] arr, long off) {
        return UNALIGNED ? UNSAFE.getLong(arr, off) : getLongByByte(arr, off, IS_BIG_ENDIAN);
    }

    /**
     * Gets long value from given address. Alignment aware.
     *
     * @param addr Address.
     * @return Long value from given address.
     */
    public static long getLong(long addr) {
        return UNALIGNED ? UNSAFE.getLong(addr) : getLongByByte(addr, IS_BIG_ENDIAN);
    }

    /**
     * Stores a long value into a byte array. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putLong(byte[] arr, long off, long val) {
        if (UNALIGNED) {
            UNSAFE.putLong(arr, off, val);
        } else {
            putLongByByte(arr, off, val, IS_BIG_ENDIAN);
        }
    }

    /**
     * Stores given integer value. Alignment aware.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putLong(long addr, long val) {
        if (UNALIGNED) {
            UNSAFE.putLong(addr, val);
        } else {
            putLongByByte(addr, val, IS_BIG_ENDIAN);
        }
    }

    /**
     * Gets a float value from a byte array. Alignment aware.
     *
     * @param arr Object.
     * @param off Offset.
     * @return Float value from byte array.
     */
    public static float getFloat(byte[] arr, long off) {
        return UNALIGNED ? UNSAFE.getFloat(arr, off) : Float.intBitsToFloat(getIntByByte(arr, off, IS_BIG_ENDIAN));
    }

    /**
     * Gets float value from given address. Alignment aware.
     *
     * @param addr Address.
     * @return Float value from given address.
     */
    public static float getFloat(long addr) {
        return UNALIGNED ? UNSAFE.getFloat(addr) : Float.intBitsToFloat(getIntByByte(addr, IS_BIG_ENDIAN));
    }

    /**
     * Stores a float value into a byte array. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putFloat(byte[] arr, long off, float val) {
        if (UNALIGNED) {
            UNSAFE.putFloat(arr, off, val);
        } else {
            putIntByByte(arr, off, Float.floatToIntBits(val), IS_BIG_ENDIAN);
        }
    }

    /**
     * Stores given float value. Alignment aware.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putFloat(long addr, float val) {
        if (UNALIGNED) {
            UNSAFE.putFloat(addr, val);
        } else {
            putIntByByte(addr, Float.floatToIntBits(val), IS_BIG_ENDIAN);
        }
    }

    /**
     * Gets a double value from a byte array. Alignment aware.
     *
     * @param arr byte array.
     * @param off Offset.
     * @return Double value from byte array. Alignment aware.
     */
    public static double getDouble(byte[] arr, long off) {
        return UNALIGNED ? UNSAFE.getDouble(arr, off) : Double.longBitsToDouble(getLongByByte(arr, off, IS_BIG_ENDIAN));
    }

    /**
     * Gets double value from given address. Alignment aware.
     *
     * @param addr Address.
     * @return Double value from given address.
     */
    public static double getDouble(long addr) {
        return UNALIGNED ? UNSAFE.getDouble(addr) : Double.longBitsToDouble(getLongByByte(addr, IS_BIG_ENDIAN));
    }

    /**
     * Stores a double value into a byte array. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putDouble(byte[] arr, long off, double val) {
        if (UNALIGNED) {
            UNSAFE.putDouble(arr, off, val);
        } else {
            putLongByByte(arr, off, Double.doubleToLongBits(val), IS_BIG_ENDIAN);
        }
    }

    /**
     * Stores given double value. Alignment aware.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putDouble(long addr, double val) {
        if (UNALIGNED) {
            UNSAFE.putDouble(addr, val);
        } else {
            putLongByByte(addr, Double.doubleToLongBits(val), IS_BIG_ENDIAN);
        }
    }

    /**
     * Gets short value from byte array assuming that value stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @return Short value from byte array.
     */
    public static short getShortLittleEndian(byte[] arr, long off) {
        return UNALIGNED ? Short.reverseBytes(UNSAFE.getShort(arr, off)) : getShortByByte(arr, off, false);
    }

    /**
     * Gets short value from given address assuming that value stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param addr Address.
     * @return Short value from given address.
     */
    public static short getShortLittleEndian(long addr) {
        return UNALIGNED ? Short.reverseBytes(UNSAFE.getShort(addr)) : getShortByByte(addr, false);
    }

    /**
     * Stores short value into byte array assuming that value should be stored in little-endian byte order and native byte order is
     * big-endian. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putShortLittleEndian(byte[] arr, long off, short val) {
        if (UNALIGNED) {
            UNSAFE.putShort(arr, off, Short.reverseBytes(val));
        } else {
            putShortByByte(arr, off, val, false);
        }
    }

    /**
     * Stores given short value assuming that value should be stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putShortLittleEndian(long addr, short val) {
        if (UNALIGNED) {
            UNSAFE.putShort(addr, Short.reverseBytes(val));
        } else {
            putShortByByte(addr, val, false);
        }
    }

    /**
     * Gets char value from byte array assuming that value stored in little-endian byte order and native byte order is big-endian. Alignment
     * aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @return Char value from byte array.
     */
    public static char getCharLittleEndian(byte[] arr, long off) {
        return UNALIGNED ? Character.reverseBytes(UNSAFE.getChar(arr, off)) : getCharByByte(arr, off, false);
    }

    /**
     * Gets char value from given address assuming that value stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param addr Address.
     * @return Char value from given address.
     */
    public static char getCharLittleEndian(long addr) {
        return UNALIGNED ? Character.reverseBytes(UNSAFE.getChar(addr)) : getCharByByte(addr, false);
    }

    /**
     * Stores char value into byte array assuming that value should be stored in little-endian byte order and native byte order is
     * big-endian. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putCharLittleEndian(byte[] arr, long off, char val) {
        if (UNALIGNED) {
            UNSAFE.putChar(arr, off, Character.reverseBytes(val));
        } else {
            putCharByByte(arr, off, val, false);
        }
    }

    /**
     * Stores given char value assuming that value should be stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putCharLittleEndian(long addr, char val) {
        if (UNALIGNED) {
            UNSAFE.putChar(addr, Character.reverseBytes(val));
        } else {
            putCharByByte(addr, val, false);
        }
    }

    /**
     * Gets integer value from byte array assuming that value stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @return Integer value from byte array.
     */
    public static int getIntLittleEndian(byte[] arr, long off) {
        return UNALIGNED ? Integer.reverseBytes(UNSAFE.getInt(arr, off)) : getIntByByte(arr, off, false);
    }

    /**
     * Gets integer value from given address assuming that value stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param addr Address.
     * @return Integer value from given address.
     */
    public static int getIntLittleEndian(long addr) {
        return UNALIGNED ? Integer.reverseBytes(UNSAFE.getInt(addr)) : getIntByByte(addr, false);
    }

    /**
     * Stores integer value into byte array assuming that value should be stored in little-endian byte order and native byte order is
     * big-endian. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putIntLittleEndian(byte[] arr, long off, int val) {
        if (UNALIGNED) {
            UNSAFE.putInt(arr, off, Integer.reverseBytes(val));
        } else {
            putIntByByte(arr, off, val, false);
        }
    }

    /**
     * Stores given integer value assuming that value should be stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putIntLittleEndian(long addr, int val) {
        if (UNALIGNED) {
            UNSAFE.putInt(addr, Integer.reverseBytes(val));
        } else {
            putIntByByte(addr, val, false);
        }
    }

    /**
     * Gets long value from byte array assuming that value stored in little-endian byte order and native byte order is big-endian. Alignment
     * aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @return Long value from byte array.
     */
    public static long getLongLittleEndian(byte[] arr, long off) {
        return UNALIGNED ? Long.reverseBytes(UNSAFE.getLong(arr, off)) : getLongByByte(arr, off, false);
    }

    /**
     * Gets long value from given address assuming that value stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param addr Address.
     * @return Long value from given address.
     */
    public static long getLongLittleEndian(long addr) {
        return UNALIGNED ? Long.reverseBytes(UNSAFE.getLong(addr)) : getLongByByte(addr, false);
    }

    /**
     * Stores long value into byte array assuming that value should be stored in little-endian byte order and native byte order is
     * big-endian. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putLongLittleEndian(byte[] arr, long off, long val) {
        if (UNALIGNED) {
            UNSAFE.putLong(arr, off, Long.reverseBytes(val));
        } else {
            putLongByByte(arr, off, val, false);
        }
    }

    /**
     * Stores given integer value assuming that value should be stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putLongLittleEndian(long addr, long val) {
        if (UNALIGNED) {
            UNSAFE.putLong(addr, Long.reverseBytes(val));
        } else {
            putLongByByte(addr, val, false);
        }
    }

    /**
     * Gets float value from byte array assuming that value stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @return Float value from byte array.
     */
    public static float getFloatLittleEndian(byte[] arr, long off) {
        return Float.intBitsToFloat(
                UNALIGNED ? Integer.reverseBytes(UNSAFE.getInt(arr, off)) : getIntByByte(arr, off, false)
        );
    }

    /**
     * Gets float value from given address assuming that value stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param addr Address.
     * @return Float value from given address.
     */
    public static float getFloatLittleEndian(long addr) {
        return Float.intBitsToFloat(UNALIGNED ? Integer.reverseBytes(UNSAFE.getInt(addr)) : getIntByByte(addr, false));
    }

    /**
     * Stores float value into byte array assuming that value should be stored in little-endian byte order and native byte order is
     * big-endian. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putFloatLittleEndian(byte[] arr, long off, float val) {
        int intVal = Float.floatToIntBits(val);

        if (UNALIGNED) {
            UNSAFE.putInt(arr, off, Integer.reverseBytes(intVal));
        } else {
            putIntByByte(arr, off, intVal, false);
        }
    }

    /**
     * Stores given float value assuming that value should be stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putFloatLittleEndian(long addr, float val) {
        int intVal = Float.floatToIntBits(val);

        if (UNALIGNED) {
            UNSAFE.putInt(addr, Integer.reverseBytes(intVal));
        } else {
            putIntByByte(addr, intVal, false);
        }
    }

    /**
     * Gets double value from byte array assuming that value stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @return Double value from byte array.
     */
    public static double getDoubleLittleEndian(byte[] arr, long off) {
        return Double.longBitsToDouble(
                UNALIGNED ? Long.reverseBytes(UNSAFE.getLong(arr, off)) : getLongByByte(arr, off, false)
        );
    }

    /**
     * Gets double value from given address assuming that value stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param addr Address.
     * @return Double value from given address.
     */
    public static double getDoubleLittleEndian(long addr) {
        return Double.longBitsToDouble(
                UNALIGNED ? Long.reverseBytes(UNSAFE.getLong(addr)) : getLongByByte(addr, false)
        );
    }

    /**
     * Stores double value into byte array assuming that value should be stored in little-endian byte order and native byte order is
     * big-endian. Alignment aware.
     *
     * @param arr Byte array.
     * @param off Offset.
     * @param val Value.
     */
    public static void putDoubleLittleEndian(byte[] arr, long off, double val) {
        long longVal = Double.doubleToLongBits(val);

        if (UNALIGNED) {
            UNSAFE.putLong(arr, off, Long.reverseBytes(longVal));
        } else {
            putLongByByte(arr, off, longVal, false);
        }
    }

    /**
     * Stores given double value assuming that value should be stored in little-endian byte order and native byte order is big-endian.
     * Alignment aware.
     *
     * @param addr Address.
     * @param val  Value.
     */
    public static void putDoubleLittleEndian(long addr, double val) {
        long longVal = Double.doubleToLongBits(val);

        if (UNALIGNED) {
            UNSAFE.putLong(addr, Long.reverseBytes(longVal));
        } else {
            putLongByByte(addr, longVal, false);
        }
    }

    /**
     * Returns static field offset.
     *
     * @param field Field.
     * @return Static field offset.
     */
    public static long staticFieldOffset(Field field) {
        return UNSAFE.staticFieldOffset(field);
    }

    /**
     * Returns object field offset.
     *
     * @param field Field.
     * @return Object field offset.
     */
    public static long objectFieldOffset(Field field) {
        return UNSAFE.objectFieldOffset(field);
    }

    /**
     * Returns static field base.
     *
     * @param field Field.
     * @return Static field base.
     */
    public static Object staticFieldBase(Field field) {
        return UNSAFE.staticFieldBase(field);
    }

    /**
     * Allocates memory.
     *
     * @param size Size.
     * @return address.
     */
    public static long allocateMemory(long size) {
        return UNSAFE.allocateMemory(size);
    }

    /**
     * Reallocates memory.
     *
     * @param addr Address.
     * @param len  Length.
     * @return address.
     */
    public static long reallocateMemory(long addr, long len) {
        return UNSAFE.reallocateMemory(addr, len);
    }

    /**
     * Fills memory with given value.
     *
     * @param addr Address.
     * @param len  Length.
     * @param val  Value.
     */
    public static void setMemory(long addr, long len, byte val) {
        UNSAFE.setMemory(addr, len, val);
    }

    /**
     * Fills memory with zeroes.
     *
     * @param addr Address.
     * @param len Length.
     */
    public static void zeroMemory(long addr, long len) {
        long off = 0;

        for (; off + PAGE_SIZE <= len; off += PAGE_SIZE) {
            GridUnsafe.copyMemory(EMPTY_PAGE, GridUnsafe.BYTE_ARR_OFF, null, addr + off, PAGE_SIZE);
        }

        if (len != off) {
            GridUnsafe.copyMemory(EMPTY_PAGE, GridUnsafe.BYTE_ARR_OFF, null, addr + off, len - off);
        }
    }

    /**
     * Copy memory between offheap locations.
     *
     * @param srcAddr Source address.
     * @param dstAddr Destination address.
     * @param len     Length.
     */
    public static void copyOffheapOffheap(long srcAddr, long dstAddr, long len) {
        if (len <= PER_BYTE_THRESHOLD) {
            for (int i = 0; i < len; i++) {
                UNSAFE.putByte(dstAddr + i, UNSAFE.getByte(srcAddr + i));
            }
        } else {
            UNSAFE.copyMemory(srcAddr, dstAddr, len);
        }
    }

    /**
     * Copy memory from offheap to heap.
     *
     * @param srcAddr Source address.
     * @param dstBase Destination base.
     * @param dstOff  Destination offset.
     * @param len     Length.
     */
    public static void copyOffheapHeap(long srcAddr, Object dstBase, long dstOff, long len) {
        if (len <= PER_BYTE_THRESHOLD) {
            for (int i = 0; i < len; i++) {
                UNSAFE.putByte(dstBase, dstOff + i, UNSAFE.getByte(srcAddr + i));
            }
        } else {
            UNSAFE.copyMemory(null, srcAddr, dstBase, dstOff, len);
        }
    }

    /**
     * Copy memory from heap to offheap.
     *
     * @param srcBase Source base.
     * @param srcOff  Source offset.
     * @param dstAddr Destination address.
     * @param len     Length.
     */
    public static void copyHeapOffheap(Object srcBase, long srcOff, long dstAddr, long len) {
        if (len <= PER_BYTE_THRESHOLD) {
            for (int i = 0; i < len; i++) {
                UNSAFE.putByte(dstAddr + i, UNSAFE.getByte(srcBase, srcOff + i));
            }
        } else {
            UNSAFE.copyMemory(srcBase, srcOff, null, dstAddr, len);
        }
    }

    /**
     * Copies memory.
     *
     * @param src Source.
     * @param dst Dst.
     * @param len Length.
     */
    public static void copyMemory(long src, long dst, long len) {
        UNSAFE.copyMemory(src, dst, len);
    }

    /**
     * Sets all bytes in a given block of memory to a copy of another block.
     *
     * @param srcBase Source base.
     * @param srcOff  Source offset.
     * @param dstBase Dst base.
     * @param dstOff  Dst offset.
     * @param len     Length.
     */
    public static void copyMemory(Object srcBase, long srcOff, Object dstBase, long dstOff, long len) {
        if (len <= PER_BYTE_THRESHOLD && srcBase != null && dstBase != null) {
            for (int i = 0; i < len; i++) {
                UNSAFE.putByte(dstBase, dstOff + i, UNSAFE.getByte(srcBase, srcOff + i));
            }
        } else {
            UNSAFE.copyMemory(srcBase, srcOff, dstBase, dstOff, len);
        }
    }

    /**
     * Frees memory.
     *
     * @param addr Address.
     */
    public static void freeMemory(long addr) {
        UNSAFE.freeMemory(addr);
    }

    /**
     * Returns the offset of the first element in the storage allocation of a given array class.
     *
     * @param cls Class.
     * @return the offset of the first element in the storage allocation of a given array class.
     */
    public static int arrayBaseOffset(Class cls) {
        return UNSAFE.arrayBaseOffset(cls);
    }

    /**
     * Allocates instance of given class.
     *
     * @param cls Class.
     * @return Allocated instance.
     * @throws InstantiationException If failed to instantiate class.
     */
    public static Object allocateInstance(Class cls) throws InstantiationException {
        return UNSAFE.allocateInstance(cls);
    }

    /**
     * Integer CAS.
     *
     * @param obj Object.
     * @param off Offset.
     * @param exp Expected.
     * @param upd Upd.
     * @return {@code True} if operation completed successfully, {@code false} - otherwise.
     */
    public static boolean compareAndSwapInt(Object obj, long off, int exp, int upd) {
        return UNSAFE.compareAndSwapInt(obj, off, exp, upd);
    }

    /**
     * Long CAS.
     *
     * @param obj Object.
     * @param off Offset.
     * @param exp Expected.
     * @param upd Upd.
     * @return {@code True} if operation completed successfully, {@code false} - otherwise.
     */
    public static boolean compareAndSwapLong(Object obj, long off, long exp, long upd) {
        return UNSAFE.compareAndSwapLong(obj, off, exp, upd);
    }

    /**
     * Atomically increments value stored in an integer pointed by {@code ptr}.
     *
     * @param ptr Pointer to an integer.
     * @return Updated value.
     */
    public static int incrementAndGetInt(long ptr) {
        return UNSAFE.getAndAddInt(null, ptr, 1) + 1;
    }

    /**
     * Atomically increments value stored in an integer pointed by {@code ptr}.
     *
     * @param ptr Pointer to an integer.
     * @return Updated value.
     */
    public static int decrementAndGetInt(long ptr) {
        return UNSAFE.getAndAddInt(null, ptr, -1) - 1;
    }

    /**
     * Gets byte value with volatile semantic.
     *
     * @param obj Object.
     * @param off Offset.
     * @return Byte value.
     */
    public static byte getByteVolatile(Object obj, long off) {
        return UNSAFE.getByteVolatile(obj, off);
    }

    /**
     * Stores byte value with volatile semantic.
     *
     * @param obj Object.
     * @param off Offset.
     * @param val Value.
     */
    public static void putByteVolatile(Object obj, long off, byte val) {
        UNSAFE.putByteVolatile(obj, off, val);
    }

    /**
     * Gets integer value with volatile semantic.
     *
     * @param obj Object.
     * @param off Offset.
     * @return Integer value.
     */
    public static int getIntVolatile(Object obj, long off) {
        return UNSAFE.getIntVolatile(obj, off);
    }

    /**
     * Stores integer value with volatile semantic.
     *
     * @param obj Object.
     * @param off Offset.
     * @param val Value.
     */
    public static void putIntVolatile(Object obj, long off, int val) {
        UNSAFE.putIntVolatile(obj, off, val);
    }

    /**
     * Gets long value with volatile semantic.
     *
     * @param obj Object.
     * @param off Offset.
     * @return Long value.
     */
    public static long getLongVolatile(Object obj, long off) {
        return UNSAFE.getLongVolatile(obj, off);
    }

    /**
     * Stores long value with volatile semantic.
     *
     * @param obj Object.
     * @param off Offset.
     * @param val Value.
     */
    public static void putLongVolatile(Object obj, long off, long val) {
        UNSAFE.putLongVolatile(obj, off, val);
    }

    /**
     * Stores reference value with volatile semantic.
     *
     * @param obj Object.
     * @param off Offset.
     * @param val Value.
     */
    public static void putObjectVolatile(Object obj, long off, Object val) {
        UNSAFE.putObjectVolatile(obj, off, val);
    }

    /**
     * Returns page size.
     *
     * @return Page size.
     */
    public static int pageSize() {
        return PAGE_SIZE;
    }

    /**
     * Returns address of {@link Buffer} instance.
     *
     * @param buf Buffer.
     * @return Buffer memory address.
     */
    public static long bufferAddress(ByteBuffer buf) {
        assert buf.isDirect();
        return UNSAFE.getLong(buf, DIRECT_BUF_ADDR_OFF);
    }

    /**
     * Invokes some method on {@code sun.misc.Unsafe} instance.
     *
     * @param mtd  Method.
     * @param args Arguments.
     * @return Method invocation result.
     */
    public static Object invoke(Method mtd, Object... args) {
        try {
            return mtd.invoke(UNSAFE, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Unsafe invocation failed [cls=" + UNSAFE.getClass() + ", mtd=" + mtd + ']', e);
        }
    }

    /**
     * Cleans direct {@code java.nio.ByteBuffer}.
     *
     * @param buf Direct buffer.
     */
    public static void cleanDirectBuffer(ByteBuffer buf) {
        assert buf.isDirect();

        UNSAFE.invokeCleaner(buf);
    }

    /**
     * Returns unaligned flag.
     */
    private static boolean unaligned() {
        String arch = System.getProperty("os.arch");

        return "i386".equals(arch) || "x86".equals(arch) || "amd64".equals(arch) || "x86_64".equals(arch);
    }

    /**
     * Returns instance of Unsafe class.
     *
     * @return Instance of Unsafe class.
     */
    private static Unsafe unsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (SecurityException ignored) {
            try {
                return AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Unsafe>() {
                            @Override
                            public Unsafe run() throws Exception {
                                Field f = Unsafe.class.getDeclaredField("theUnsafe");

                                f.setAccessible(true);

                                return (Unsafe) f.get(null);
                            }
                        });
            } catch (PrivilegedActionException e) {
                throw new RuntimeException("Could not initialize intrinsics.", e.getCause());
            }
        }
    }

    /**
     * Return buffer address offset.
     *
     * @return Buffer address offset.
     */
    private static long bufferAddressOffset() {
        final ByteBuffer maybeDirectBuf = ByteBuffer.allocateDirect(1);

        Field addrField = AccessController.doPrivileged(new PrivilegedAction<Field>() {
            @Override
            public Field run() {
                try {
                    Field addrFld = Buffer.class.getDeclaredField("address");

                    addrFld.setAccessible(true);

                    if (addrFld.getLong(maybeDirectBuf) == 0) {
                        throw new RuntimeException("java.nio.DirectByteBuffer.address field is unavailable.");
                    }

                    return addrFld;
                } catch (Exception e) {
                    throw new RuntimeException("java.nio.DirectByteBuffer.address field is unavailable.", e);
                }
            }
        });

        return UNSAFE.objectFieldOffset(addrField);
    }

    /**
     * Returns {@code short} value.
     *
     * @param obj       Object.
     * @param off       Offset.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     * @return {@code short} value.
     */
    private static short getShortByByte(Object obj, long off, boolean bigEndian) {
        if (bigEndian) {
            return (short) (UNSAFE.getByte(obj, off) << 8 | (UNSAFE.getByte(obj, off + 1) & 0xff));
        } else {
            return (short) (UNSAFE.getByte(obj, off + 1) << 8 | (UNSAFE.getByte(obj, off) & 0xff));
        }
    }

    /**
     * Returns {@code short} value.
     *
     * @param addr      Address.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     * @return {@code short} value.
     */
    private static short getShortByByte(long addr, boolean bigEndian) {
        if (bigEndian) {
            return (short) (UNSAFE.getByte(addr) << 8 | (UNSAFE.getByte(addr + 1) & 0xff));
        } else {
            return (short) (UNSAFE.getByte(addr + 1) << 8 | (UNSAFE.getByte(addr) & 0xff));
        }
    }

    /**
     * Sets {@code short} value.
     *
     * @param obj       Object.
     * @param off       Offset.
     * @param val       Value.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     */
    private static void putShortByByte(Object obj, long off, short val, boolean bigEndian) {
        if (bigEndian) {
            UNSAFE.putByte(obj, off, (byte) (val >> 8));
            UNSAFE.putByte(obj, off + 1, (byte) val);
        } else {
            UNSAFE.putByte(obj, off + 1, (byte) (val >> 8));
            UNSAFE.putByte(obj, off, (byte) val);
        }
    }

    /**
     * Sets {@code short} value.
     *
     * @param addr      Address.
     * @param val       Value.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     */
    private static void putShortByByte(long addr, short val, boolean bigEndian) {
        if (bigEndian) {
            UNSAFE.putByte(addr, (byte) (val >> 8));
            UNSAFE.putByte(addr + 1, (byte) val);
        } else {
            UNSAFE.putByte(addr + 1, (byte) (val >> 8));
            UNSAFE.putByte(addr, (byte) val);
        }
    }

    /**
     * Returns {@code char} value.
     *
     * @param obj       Object.
     * @param off       Offset.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     * @return {@code char} value.
     */
    private static char getCharByByte(Object obj, long off, boolean bigEndian) {
        if (bigEndian) {
            return (char) (UNSAFE.getByte(obj, off) << 8 | (UNSAFE.getByte(obj, off + 1) & 0xff));
        } else {
            return (char) (UNSAFE.getByte(obj, off + 1) << 8 | (UNSAFE.getByte(obj, off) & 0xff));
        }
    }

    /**
     * Returns {@code char} value.
     *
     * @param addr      Address.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     * @return {@code char} value.
     */
    private static char getCharByByte(long addr, boolean bigEndian) {
        if (bigEndian) {
            return (char) (UNSAFE.getByte(addr) << 8 | (UNSAFE.getByte(addr + 1) & 0xff));
        } else {
            return (char) (UNSAFE.getByte(addr + 1) << 8 | (UNSAFE.getByte(addr) & 0xff));
        }
    }

    /**
     * Sets {@code char} value.
     *
     * @param obj       Object.
     * @param addr      Address.
     * @param val       Value.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     */
    private static void putCharByByte(Object obj, long addr, char val, boolean bigEndian) {
        if (bigEndian) {
            UNSAFE.putByte(obj, addr, (byte) (val >> 8));
            UNSAFE.putByte(obj, addr + 1, (byte) val);
        } else {
            UNSAFE.putByte(obj, addr + 1, (byte) (val >> 8));
            UNSAFE.putByte(obj, addr, (byte) val);
        }
    }

    /**
     * Sets {@code char} value.
     *
     * @param addr      Address.
     * @param val       Value.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     */
    private static void putCharByByte(long addr, char val, boolean bigEndian) {
        if (bigEndian) {
            UNSAFE.putByte(addr, (byte) (val >> 8));
            UNSAFE.putByte(addr + 1, (byte) val);
        } else {
            UNSAFE.putByte(addr + 1, (byte) (val >> 8));
            UNSAFE.putByte(addr, (byte) val);
        }
    }

    /**
     * Returns {@code int} value.
     *
     * @param obj       Object.
     * @param addr      Address.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     * @return {@code int} value.
     */
    private static int getIntByByte(Object obj, long addr, boolean bigEndian) {
        if (bigEndian) {
            return (((int) UNSAFE.getByte(obj, addr)) << 24)
                    | (((int) UNSAFE.getByte(obj, addr + 1) & 0xff) << 16)
                    | (((int) UNSAFE.getByte(obj, addr + 2) & 0xff) << 8)
                    | (((int) UNSAFE.getByte(obj, addr + 3) & 0xff));
        } else {
            return (((int) UNSAFE.getByte(obj, addr + 3)) << 24)
                    | (((int) UNSAFE.getByte(obj, addr + 2) & 0xff) << 16)
                    | (((int) UNSAFE.getByte(obj, addr + 1) & 0xff) << 8)
                    | (((int) UNSAFE.getByte(obj, addr) & 0xff));
        }
    }

    /**
     * Sets {@code int} value.
     *
     * @param addr      Address.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     * @return {@code int} value.
     */
    private static int getIntByByte(long addr, boolean bigEndian) {
        if (bigEndian) {
            return (((int) UNSAFE.getByte(addr)) << 24)
                    | (((int) UNSAFE.getByte(addr + 1) & 0xff) << 16)
                    | (((int) UNSAFE.getByte(addr + 2) & 0xff) << 8)
                    | (((int) UNSAFE.getByte(addr + 3) & 0xff));
        } else {
            return (((int) UNSAFE.getByte(addr + 3)) << 24)
                    | (((int) UNSAFE.getByte(addr + 2) & 0xff) << 16)
                    | (((int) UNSAFE.getByte(addr + 1) & 0xff) << 8)
                    | (((int) UNSAFE.getByte(addr) & 0xff));
        }
    }

    /**
     * Sets {@code int} value.
     *
     * @param obj       Object.
     * @param addr      Address.
     * @param val       Value.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     */
    private static void putIntByByte(Object obj, long addr, int val, boolean bigEndian) {
        if (bigEndian) {
            UNSAFE.putByte(obj, addr, (byte) (val >> 24));
            UNSAFE.putByte(obj, addr + 1, (byte) (val >> 16));
            UNSAFE.putByte(obj, addr + 2, (byte) (val >> 8));
            UNSAFE.putByte(obj, addr + 3, (byte) (val));
        } else {
            UNSAFE.putByte(obj, addr + 3, (byte) (val >> 24));
            UNSAFE.putByte(obj, addr + 2, (byte) (val >> 16));
            UNSAFE.putByte(obj, addr + 1, (byte) (val >> 8));
            UNSAFE.putByte(obj, addr, (byte) (val));
        }
    }

    /**
     * Sets {@code int} value.
     *
     * @param addr      Address.
     * @param val       Value.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     */
    private static void putIntByByte(long addr, int val, boolean bigEndian) {
        if (bigEndian) {
            UNSAFE.putByte(addr, (byte) (val >> 24));
            UNSAFE.putByte(addr + 1, (byte) (val >> 16));
            UNSAFE.putByte(addr + 2, (byte) (val >> 8));
            UNSAFE.putByte(addr + 3, (byte) (val));
        } else {
            UNSAFE.putByte(addr + 3, (byte) (val >> 24));
            UNSAFE.putByte(addr + 2, (byte) (val >> 16));
            UNSAFE.putByte(addr + 1, (byte) (val >> 8));
            UNSAFE.putByte(addr, (byte) (val));
        }
    }

    /**
     * Returns {@code long} value.
     *
     * @param obj       Object.
     * @param addr      Address.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     * @return {@code long} value.
     */
    private static long getLongByByte(Object obj, long addr, boolean bigEndian) {
        if (bigEndian) {
            return (((long) UNSAFE.getByte(obj, addr)) << 56)
                    | (((long) UNSAFE.getByte(obj, addr + 1) & 0xff) << 48)
                    | (((long) UNSAFE.getByte(obj, addr + 2) & 0xff) << 40)
                    | (((long) UNSAFE.getByte(obj, addr + 3) & 0xff) << 32)
                    | (((long) UNSAFE.getByte(obj, addr + 4) & 0xff) << 24)
                    | (((long) UNSAFE.getByte(obj, addr + 5) & 0xff) << 16)
                    | (((long) UNSAFE.getByte(obj, addr + 6) & 0xff) << 8)
                    | (((long) UNSAFE.getByte(obj, addr + 7) & 0xff));
        } else {
            return (((long) UNSAFE.getByte(obj, addr + 7)) << 56)
                    | (((long) UNSAFE.getByte(obj, addr + 6) & 0xff) << 48)
                    | (((long) UNSAFE.getByte(obj, addr + 5) & 0xff) << 40)
                    | (((long) UNSAFE.getByte(obj, addr + 4) & 0xff) << 32)
                    | (((long) UNSAFE.getByte(obj, addr + 3) & 0xff) << 24)
                    | (((long) UNSAFE.getByte(obj, addr + 2) & 0xff) << 16)
                    | (((long) UNSAFE.getByte(obj, addr + 1) & 0xff) << 8)
                    | (((long) UNSAFE.getByte(obj, addr) & 0xff));
        }
    }

    /**
     * Returns {@code long} value.
     *
     * @param addr      Address.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     * @return {@code long} value.
     */
    private static long getLongByByte(long addr, boolean bigEndian) {
        if (bigEndian) {
            return (((long) UNSAFE.getByte(addr)) << 56)
                    | (((long) UNSAFE.getByte(addr + 1) & 0xff) << 48)
                    | (((long) UNSAFE.getByte(addr + 2) & 0xff) << 40)
                    | (((long) UNSAFE.getByte(addr + 3) & 0xff) << 32)
                    | (((long) UNSAFE.getByte(addr + 4) & 0xff) << 24)
                    | (((long) UNSAFE.getByte(addr + 5) & 0xff) << 16)
                    | (((long) UNSAFE.getByte(addr + 6) & 0xff) << 8)
                    | (((long) UNSAFE.getByte(addr + 7) & 0xff));
        } else {
            return (((long) UNSAFE.getByte(addr + 7)) << 56)
                    | (((long) UNSAFE.getByte(addr + 6) & 0xff) << 48)
                    | (((long) UNSAFE.getByte(addr + 5) & 0xff) << 40)
                    | (((long) UNSAFE.getByte(addr + 4) & 0xff) << 32)
                    | (((long) UNSAFE.getByte(addr + 3) & 0xff) << 24)
                    | (((long) UNSAFE.getByte(addr + 2) & 0xff) << 16)
                    | (((long) UNSAFE.getByte(addr + 1) & 0xff) << 8)
                    | (((long) UNSAFE.getByte(addr) & 0xff));
        }
    }

    /**
     * Sets {@code long} value.
     *
     * @param obj       Object.
     * @param addr      Address.
     * @param val       Value.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     */
    private static void putLongByByte(Object obj, long addr, long val, boolean bigEndian) {
        if (bigEndian) {
            UNSAFE.putByte(obj, addr, (byte) (val >> 56));
            UNSAFE.putByte(obj, addr + 1, (byte) (val >> 48));
            UNSAFE.putByte(obj, addr + 2, (byte) (val >> 40));
            UNSAFE.putByte(obj, addr + 3, (byte) (val >> 32));
            UNSAFE.putByte(obj, addr + 4, (byte) (val >> 24));
            UNSAFE.putByte(obj, addr + 5, (byte) (val >> 16));
            UNSAFE.putByte(obj, addr + 6, (byte) (val >> 8));
            UNSAFE.putByte(obj, addr + 7, (byte) (val));
        } else {
            UNSAFE.putByte(obj, addr + 7, (byte) (val >> 56));
            UNSAFE.putByte(obj, addr + 6, (byte) (val >> 48));
            UNSAFE.putByte(obj, addr + 5, (byte) (val >> 40));
            UNSAFE.putByte(obj, addr + 4, (byte) (val >> 32));
            UNSAFE.putByte(obj, addr + 3, (byte) (val >> 24));
            UNSAFE.putByte(obj, addr + 2, (byte) (val >> 16));
            UNSAFE.putByte(obj, addr + 1, (byte) (val >> 8));
            UNSAFE.putByte(obj, addr, (byte) (val));
        }
    }

    /**
     * Sets {@code long} value.
     *
     * @param addr      Address.
     * @param val       Value.
     * @param bigEndian Order of value bytes in memory. If {@code true} - big-endian, otherwise little-endian.
     */
    private static void putLongByByte(long addr, long val, boolean bigEndian) {
        if (bigEndian) {
            UNSAFE.putByte(addr, (byte) (val >> 56));
            UNSAFE.putByte(addr + 1, (byte) (val >> 48));
            UNSAFE.putByte(addr + 2, (byte) (val >> 40));
            UNSAFE.putByte(addr + 3, (byte) (val >> 32));
            UNSAFE.putByte(addr + 4, (byte) (val >> 24));
            UNSAFE.putByte(addr + 5, (byte) (val >> 16));
            UNSAFE.putByte(addr + 6, (byte) (val >> 8));
            UNSAFE.putByte(addr + 7, (byte) (val));
        } else {
            UNSAFE.putByte(addr + 7, (byte) (val >> 56));
            UNSAFE.putByte(addr + 6, (byte) (val >> 48));
            UNSAFE.putByte(addr + 5, (byte) (val >> 40));
            UNSAFE.putByte(addr + 4, (byte) (val >> 32));
            UNSAFE.putByte(addr + 3, (byte) (val >> 24));
            UNSAFE.putByte(addr + 2, (byte) (val >> 16));
            UNSAFE.putByte(addr + 1, (byte) (val >> 8));
            UNSAFE.putByte(addr, (byte) (val));
        }
    }

    /**
     * Reads a byte array from the memory.
     *
     * @param addr Start address.
     * @param off  Offset.
     * @param len  Bytes length.
     * @return Bytes from given address.
     */
    public static byte[] getBytes(long addr, int off, int len) {
        assert addr > 0 : addr;
        assert off >= 0;
        assert len >= 0;

        byte[] bytes = new byte[len];

        copyMemory(null, addr + off, bytes, BYTE_ARR_OFF, len);

        return bytes;
    }

    /**
     * Returns {@code True} if equals.
     *
     * @param ptr1 First pointer.
     * @param ptr2 Second pointer.
     * @param size Memory size.
     * @return {@code True} if equals.
     */
    public static boolean compare(long ptr1, long ptr2, int size) {
        assert ptr1 > 0 : ptr1;
        assert ptr2 > 0 : ptr2;
        assert size > 0 : size;

        if (ptr1 == ptr2) {
            return true;
        }

        int words = size / 8;

        for (int i = 0; i < words; i++) {
            long w1 = getLong(ptr1);
            long w2 = getLong(ptr2);

            if (w1 != w2) {
                return false;
            }

            ptr1 += 8;
            ptr2 += 8;
        }

        int left = size % 8;

        for (int i = 0; i < left; i++) {
            byte b1 = getByte(ptr1);
            byte b2 = getByte(ptr2);

            if (b1 != b2) {
                return false;
            }

            ptr1++;
            ptr2++;
        }

        return true;
    }
}
