package com.alibaba.datax.common.util;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Kernel32
 *
 * @author jingwk
 * @version 1.0
 * @since 2019/11/09
 */

public interface Kernel32 extends Library {
    public static Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);

    public long GetProcessId(Long hProcess);
}
