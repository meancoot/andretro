package org.libretro;

import java.nio.ByteBuffer;

public final class LibRetro
{
	public static final int  RETRO_API_VERSION = 1;

	public static final int  RETRO_DEVICE_MASK = 0xff;
	public static final int  RETRO_DEVICE_NONE = 0;
	public static final int  RETRO_DEVICE_JOYPAD = 1;
	public static final int  RETRO_DEVICE_MOUSE = 2;
	public static final int  RETRO_DEVICE_KEYBOARD = 3;
	public static final int  RETRO_DEVICE_LIGHTGUN = 4;
	public static final int  RETRO_DEVICE_ANALOG = 5;

	public static final int  RETRO_DEVICE_JOYPAD_MULTITAP = ((1 << 8) | RETRO_DEVICE_JOYPAD);
	public static final int  RETRO_DEVICE_LIGHTGUN_SUPER_SCOPE = ((1 << 8) | RETRO_DEVICE_LIGHTGUN);
	public static final int  RETRO_DEVICE_LIGHTGUN_JUSTIFIER = ((2 << 8) | RETRO_DEVICE_LIGHTGUN);
	public static final int  RETRO_DEVICE_LIGHTGUN_JUSTIFIERS = ((3 << 8) | RETRO_DEVICE_LIGHTGUN);

	public static final int  RETRO_DEVICE_ID_JOYPAD_B = 0;
	public static final int  RETRO_DEVICE_ID_JOYPAD_Y = 1;
	public static final int  RETRO_DEVICE_ID_JOYPAD_SELECT = 2;
	public static final int  RETRO_DEVICE_ID_JOYPAD_START = 3;
	public static final int  RETRO_DEVICE_ID_JOYPAD_UP = 4;
	public static final int  RETRO_DEVICE_ID_JOYPAD_DOWN = 5;
	public static final int  RETRO_DEVICE_ID_JOYPAD_LEFT = 6;
	public static final int  RETRO_DEVICE_ID_JOYPAD_RIGHT = 7;
	public static final int  RETRO_DEVICE_ID_JOYPAD_A = 8;
	public static final int  RETRO_DEVICE_ID_JOYPAD_X = 9;
	public static final int  RETRO_DEVICE_ID_JOYPAD_L = 10;
	public static final int  RETRO_DEVICE_ID_JOYPAD_R = 11;
	public static final int  RETRO_DEVICE_ID_JOYPAD_L2 = 12;
	public static final int  RETRO_DEVICE_ID_JOYPAD_R2 = 13;
	public static final int  RETRO_DEVICE_ID_JOYPAD_L3 = 14;
	public static final int  RETRO_DEVICE_ID_JOYPAD_R3 = 15;

	public static final int  RETRO_DEVICE_INDEX_ANALOG_LEFT = 0;
	public static final int  RETRO_DEVICE_INDEX_ANALOG_RIGHT = 1;
	public static final int  RETRO_DEVICE_ID_ANALOG_X = 0;
	public static final int  RETRO_DEVICE_ID_ANALOG_Y = 1;

	public static final int  RETRO_DEVICE_ID_MOUSE_X = 0;
	public static final int  RETRO_DEVICE_ID_MOUSE_Y = 1;
	public static final int  RETRO_DEVICE_ID_MOUSE_LEFT = 2;
	public static final int  RETRO_DEVICE_ID_MOUSE_RIGHT = 3;

	public static final int  RETRO_DEVICE_ID_LIGHTGUN_X = 0;
	public static final int  RETRO_DEVICE_ID_LIGHTGUN_Y = 1;
	public static final int  RETRO_DEVICE_ID_LIGHTGUN_TRIGGER = 2;
	public static final int  RETRO_DEVICE_ID_LIGHTGUN_CURSOR = 3;
	public static final int  RETRO_DEVICE_ID_LIGHTGUN_TURBO = 4;
	public static final int  RETRO_DEVICE_ID_LIGHTGUN_PAUSE = 5;
	public static final int  RETRO_DEVICE_ID_LIGHTGUN_START = 6;

	public static final int  RETRO_REGION_NTSC = 0;
	public static final int  RETRO_REGION_PAL = 1;

	public static final int  RETRO_MEMORY_MASK = 0xff;
	public static final int  RETRO_MEMORY_SAVE_RAM = 0;
	public static final int  RETRO_MEMORY_RTC = 1;
	public static final int  RETRO_MEMORY_SYSTEM_RAM = 2;
	public static final int  RETRO_MEMORY_VIDEO_RAM = 3;

	public static final int  RETRO_MEMORY_SNES_BSX_RAM = ((1 << 8) | RETRO_MEMORY_SAVE_RAM);
	public static final int  RETRO_MEMORY_SNES_BSX_PRAM = ((2 << 8) | RETRO_MEMORY_SAVE_RAM);
	public static final int  RETRO_MEMORY_SNES_SUFAMI_TURBO_A_RAM = ((3 << 8) | RETRO_MEMORY_SAVE_RAM);
	public static final int  RETRO_MEMORY_SNES_SUFAMI_TURBO_B_RAM = ((4 << 8) | RETRO_MEMORY_SAVE_RAM);
	public static final int  RETRO_MEMORY_SNES_GAME_BOY_RAM = ((5 << 8) | RETRO_MEMORY_SAVE_RAM);
	public static final int  RETRO_MEMORY_SNES_GAME_BOY_RTC = ((6 << 8) | RETRO_MEMORY_RTC);

	public static final int  RETRO_GAME_TYPE_BSX = 0x101;
	public static final int  RETRO_GAME_TYPE_BSX_SLOTTED = 0x102;
	public static final int  RETRO_GAME_TYPE_SUFAMI_TURBO = 0x103;
	public static final int  RETRO_GAME_TYPE_SUPER_GAME_BOY = 0x104;

    // TODO: Keyboard

	public static final int  RETRO_ENVIRONMENT_SET_ROTATION = 1;  // const unsigned * --
	public static final int  RETRO_ENVIRONMENT_GET_OVERSCAN = 2;  // bool * --
	public static final int  RETRO_ENVIRONMENT_GET_CAN_DUPE = 3;  // bool * --
	public static final int  RETRO_ENVIRONMENT_GET_VARIABLE = 4; // struct retro_variable * --
	public static final int  RETRO_ENVIRONMENT_SET_VARIABLES = 5;  // const struct retro_variable * --
	public static final int  RETRO_ENVIRONMENT_SET_MESSAGE = 6;  // const struct retro_message * --
	public static final int  RETRO_ENVIRONMENT_SHUTDOWN = 7;  // N/A (NULL) --
	public static final int  RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL = 8;
	public static final int  RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY = 9;
	public static final int  RETRO_ENVIRONMENT_SET_PIXEL_FORMAT = 10;
	
	public static class SystemInfo
	{
	    public String libraryName;
	    public String libraryVersion;
	    public String validExtensions;
	    
	    public boolean needFullPath;
	    public boolean blockExtract;
	}
	
	public static class AVInfo
	{
	    public int baseWidth;
	    public int baseHeight;
	    
	    public int maxWidth;
	    public int maxHeight;
	    
	    public float aspectRatio;
	    
	    public double fps;
	    public double sampleRate;
	}
	
	public static native boolean loadLibrary(String aPath, String aSystemDirectory);
	public static native void unloadLibrary();
	public static native void init();
	public static native void deinit();
	public static native int apiVersion();
	public static native void getSystemInfo(SystemInfo aInfo);
	public static native void getSystemAVInfo(AVInfo aInfo);
	public static native void setControllerPortDevice(int aPort, int aDevice);
	public static native void reset();
	public static native int run(Object aVideo, int[] aSize, short[] aAudio, int aJoypad, boolean aRewind);
	public static native int serializeSize();
//	public static native boolean serialize(byte[] aData, int aSize);
//	public static native boolean unserialize(byte[] aData, int aSize);
	public static native void cheatReset();
	public static native void cheatSet(int aIndex, boolean aEnabled, String aCode);
	public static native boolean loadGame(String aPath);
	public static native void unloadGame();
	public static native int getRegion();
	public static native int getMemorySize(int aID);
	public static native ByteBuffer getMemoryData(int aID);
	
	// Helpers
	public static native void setupRewinder(int aDataSize); // 0 or less disables
	
	public static native boolean writeMemoryRegion(int aID, String aFileName);
	public static native boolean readMemoryRegion(int aID, String aFileBase);

	public static native boolean serializeToFile(String aPath);
	public static native boolean unserializeFromFile(String aPath);
}
