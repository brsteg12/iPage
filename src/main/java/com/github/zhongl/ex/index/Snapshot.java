package com.github.zhongl.ex.index;

import com.github.zhongl.ex.codec.Codec;
import com.github.zhongl.ex.page.Appendable;
import com.github.zhongl.ex.page.*;
import com.github.zhongl.ex.page.Number;
import com.github.zhongl.ex.util.Entry;
import com.github.zhongl.ex.util.FutureCallbacks;
import com.github.zhongl.ex.util.Nils;
import com.google.common.collect.PeekingIterator;

import javax.annotation.concurrent.GuardedBy;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkState;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
abstract class Snapshot extends Binder<Object> {
    public Snapshot(File dir, Codec codec) throws IOException {super(dir, codec);}

    @GuardedBy("readOnly")
    public Cursor get(Md5Key key) {
        return ((Partition) pages.get(binarySearchPageIndex(key))).get(key); // index will always in [0, pages.size)
    }

    public Snapshot merge(Iterator<Entry<Md5Key, Cursor>> sortedIterator) throws IOException {

        if (isEmpty()) {
            merge(sortedIterator, this);
            return this;
        }

        File parentFile = dir.getParentFile();
        String child = (Long.parseLong(dir.getName()) + 1) + "";
        Snapshot snapshot = newSnapshotOn(new File(parentFile, child));

        merge(sortedIterator, snapshot);

        remove();

        return snapshot;
    }

    public void remove() {
        while (!pages.isEmpty()) removeHeadPage();
        checkState(dir.delete());
    }

    @Override
    protected Number parseNumber(String text) { return new Md5Key(text); }

    protected final void merge(
            PeekingIterator<Entry<Md5Key, Cursor>> aItr,
            PeekingIterator<Entry<Md5Key, Cursor>> bItr,
            Appendable appendable
    ) throws IOException {

        while (aItr.hasNext() && bItr.hasNext()) {

            Entry<Md5Key, Cursor> a = aItr.peek();
            Entry<Md5Key, Cursor> b = bItr.peek();
            Entry<Md5Key, Cursor> c;

            int result = a.compareTo(b);

            if (result < 0) c = aItr.next();      // a <  b, use a
            else if (result > 0) c = bItr.next(); // a >  b, use b
            else {                                // a == b, use b instead a
                c = bItr.next();
                aItr.next();
            }

            if (c.value() == Nils.CURSOR) continue; // remove entry

            appendable.append(c, FutureCallbacks.<Cursor>ignore());
            boolean force = !aItr.hasNext() && !bItr.hasNext();
            if (force) appendable.force();
        }

    }

    protected final void mergeRestOf(Iterator<Entry<Md5Key, Cursor>> iterator, Appendable merged) throws IOException {
        while (iterator.hasNext()) {
            Entry<Md5Key, Cursor> entry = iterator.next();
            if (entry.value() == Nils.CURSOR) continue;
            merged.append(entry, FutureCallbacks.<Cursor>ignore());
        }
        merged.force();
    }

    protected abstract void merge(Iterator<Entry<Md5Key, Cursor>> sortedIterator, Snapshot snapshot) throws IOException;

    protected abstract Snapshot newSnapshotOn(File dir) throws IOException;

    protected abstract boolean isEmpty();

    protected void removeHeadPage() {
        Page page = pages.remove(0);
        page.close();
        checkState(page.file().delete());
    }
}