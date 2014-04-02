import sun.misc.Unsafe;

import java.lang.reflect.Field;

class SuperArray {
    private final static int BYTE = 1;
    private final static int INT = 4;
    private long size;
    private long address;
    private long tail;
    Unsafe unsafe;
    public SuperArray(long size) throws Exception {
        Field f =  Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);
        this.size = size;
        address = unsafe.allocateMemory(size * INT);
        tail = 0;
    }
    public void set(long i, byte value) {
        unsafe.putByte(address + i * BYTE, value);
    }
    public void putInt(long i, int value){
        unsafe.putInt(address + i * INT,value);
        tail = ++i;
    }
    public void putInt(int value){
        unsafe.putInt(address + tail++ * INT,value);
    }
    public int get(long idx) {
        return unsafe.getByte(address + idx * BYTE);
    }
    public int getInt(long idx){ return unsafe.getInt(address + idx * INT);}
    public int getInt(){ return unsafe.getInt(address + --tail * INT);}
    public void finalize(){discard();}
    public void discard(){unsafe.freeMemory(address);}
    public long size() {
        return size;
    }
}