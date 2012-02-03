package com.github.zhongl.ex.index;

import com.github.zhongl.ex.codec.Codec;
import com.github.zhongl.ex.lang.Entry;
import com.github.zhongl.ex.nio.ReadOnlyMappedBuffers;
import com.github.zhongl.ex.page.*;
import com.github.zhongl.ex.page.Number;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.Collections;
import java.util.RandomAccess;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
class Partition extends Page {
    private final Entries entries = new Entries();

    private int count;

    protected Partition(File file, Number number, int capacity, Codec codec) {
        super(file, number, capacity, codec);
    }

    @Override
    protected boolean checkOverflow(int size, int capacity) {
        if ((++count) <= Index.MAX_ENTRY_SIZE) return false;
        count = 0;
        return true;
    }

    @Override
    protected Batch newBatch(CursorFactory cursorFactory, int position, int estimateBufferSize) {
        return new DefaultBatch(cursorFactory, position, estimateBufferSize);
    }

    public Offset get(Md5Key key) {
        if (!file().exists()) return null;
        int index = Collections.binarySearch(entries, new Entry<Md5Key, Offset>(key, new Offset(-1L)));
        if (index < 0) return null;
        return entries.get(index).value();
    }

    private class Entries extends AbstractList<Entry<Md5Key, Offset>> implements RandomAccess {

        @Override
        public Entry<Md5Key, Offset> get(int index) {
            ByteBuffer buffer = ReadOnlyMappedBuffers.getOrMap(file());
            buffer.position(index * EntryCodec.LENGTH).limit((index + 1) * EntryCodec.LENGTH);
            return codec().decode(buffer);
        }

        @Override
        public int size() {
            return ReadOnlyMappedBuffers.getOrMap(file()).capacity() / EntryCodec.LENGTH;
        }
    }

}
