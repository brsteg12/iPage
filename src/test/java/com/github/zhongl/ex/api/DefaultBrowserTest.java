package com.github.zhongl.ex.api;

import com.github.zhongl.ex.actor.Actor;
import com.github.zhongl.ex.index.Index;
import com.github.zhongl.ex.index.Md5Key;
import com.github.zhongl.ex.journal.Checkpoint;
import com.github.zhongl.ex.page.Cursor;
import com.github.zhongl.ex.page.DefaultCursor;
import com.github.zhongl.ex.util.Entry;
import com.github.zhongl.ex.util.Nils;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ForwardingCollection;
import com.google.common.util.concurrent.FutureCallback;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public class DefaultBrowserTest {

    private DefaultBrowser browser;
    private byte[] value;
    private DurableMock durableMock;
    private Md5Key key;
    private Index index;
    private DefaultBrowserTest.ErasableMock erasableMock;
    private DefaultCursor cursor;

    @Before
    public void setUp() throws Exception {
        value = "value".getBytes();
        key = Md5Key.generate(value);
        durableMock = new DurableMock();
        erasableMock = new ErasableMock();
        index = mock(Index.class);
        browser = new DefaultBrowser(index);

        cursor = new DefaultCursor(0L, 1);
        doReturn(cursor).when(index).get(key);
    }

    @Test
    public void force() throws Exception {
        // TODO force
        byte[] value0 = "0".getBytes();
        Md5Key key0 = Md5Key.generate(value0);
        byte[] value1 = "1".getBytes();
        Md5Key key1 = Md5Key.generate(value1);
        byte[] value2 = "2".getBytes();
        Md5Key key2 = Md5Key.generate(value2);


        Entry<Md5Key, byte[]> append0 = new Entry<Md5Key, byte[]>(key0, value0);
        Entry<Md5Key, byte[]> append1 = new Entry<Md5Key, byte[]>(key1, value1);
        Entry<Md5Key, byte[]> append2 = new Entry<Md5Key, byte[]>(key2, value2);

        browser.update(append0);
        browser.update(append1);
        browser.update(append2);

        Entry<Md5Key, byte[]> remove0 = new Entry<Md5Key, byte[]>(key, Nils.BYTES);
        browser.update(remove0);

        Checkpoint checkpoint = new Checkpoint(1L);
        browser.force(checkpoint);

        durableMock.assertAppendingsHas(append0, append1, append2);
        durableMock.assertRemovingsHas(new Entry<Md5Key, Cursor>(key, cursor));
        durableMock.assertCheckpointIs(checkpoint);

    }

    @Test
    public void forceNothingsAndEraseImmediately() throws Exception {
        Entry<Md5Key, byte[]> append = new Entry<Md5Key, byte[]>(key, value);
        Entry<Md5Key, byte[]> remove = new Entry<Md5Key, byte[]>(key, Nils.BYTES);

        browser.update(append);
        browser.update(remove);

        Checkpoint checkpoint = new Checkpoint(1L);
        browser.force(checkpoint);
        erasableMock.assertCheckpointIs(checkpoint);
    }

    @Test
    public void missInCacheButGetByCursor() throws Exception {
        Entry<Md5Key, byte[]> entry = new Entry<Md5Key, byte[]>(key, value);

        browser.update(entry);
        browser.force(new Checkpoint(1L));

        durableMock.assertAppendingsHas(entry);

        Mergings mergings = new Mergings(singleton(new Entry<Md5Key, Cursor>(key, Nils.CURSOR)));

        browser.merge(mergings);

        mergings.waitForIterated();

        assertThat(browser.get(key), is(value));
        durableMock.assertCursorIs(cursor);
    }

    @Test
    public void removed() throws Exception {
        browser.update(new Entry<Md5Key, byte[]>(key, Nils.BYTES));
        assertThat(browser.get(key), is(nullValue()));
    }

    @Test
    public void phantom() throws Exception {
        browser.update(new Entry<Md5Key, byte[]>(key, value));
        assertThat(browser.get(key), is(value));

        browser.update(new Entry<Md5Key, byte[]>(key, Nils.BYTES));
        assertThat(browser.get(key), is(nullValue()));
    }

    @After
    public void tearDown() throws Exception {
        browser.stop();
        durableMock.stop();
        erasableMock.stop();
    }

    private class DurableMock extends Actor implements Durable {

        private volatile Iterator<Entry<Md5Key, byte[]>> appendings;
        private volatile Iterator<Entry<Md5Key, Cursor>> removings;
        private volatile Checkpoint checkpoint;
        private volatile Cursor cursor;

        private final CountDownLatch mergeLatch = new CountDownLatch(1);
        private final CountDownLatch getLatch = new CountDownLatch(1);

        @Override
        public void merge(
                Iterator<Entry<Md5Key, byte[]>> appendings,
                Iterator<Entry<Md5Key, Cursor>> removings,
                Checkpoint checkpoint) {
            this.appendings = appendings;
            this.removings = removings;
            this.checkpoint = checkpoint;
            mergeLatch.countDown();
        }

        @Override
        public void get(Cursor cursor, FutureCallback<byte[]> callback) {
            this.cursor = cursor;
            callback.onSuccess(value);
            getLatch.countDown();
        }

        public void assertAppendingsHas(Entry<Md5Key, byte[]>... entries) throws Exception {
            mergeLatch.await(1, SECONDS);
            for (Entry<Md5Key, byte[]> entry : entries) {
                assertThat(appendings.next(), is(entry));
            }
        }

        public void assertRemovingsHas(Entry<Md5Key, Cursor>... entries) throws Exception {
            mergeLatch.await(1, SECONDS);
            for (Entry<Md5Key, Cursor> entry : entries) {
                assertThat(removings.next(), is(entry));
            }
        }

        public void assertCheckpointIs(Checkpoint checkpoint) throws Exception {
            mergeLatch.await(1, SECONDS);
            assertThat(this.checkpoint, is(checkpoint));
        }

        public void assertCursorIs(Cursor cursor) throws Exception {
            getLatch.await(1, SECONDS);
            assertThat(this.cursor, is(cursor));
        }

    }

    private class ErasableMock extends Actor implements Erasable {

        private Checkpoint checkpoint;
        private CountDownLatch eraseLatch = new CountDownLatch(1);

        @Override
        public void erase(Checkpoint checkpoint) {
            eraseLatch.countDown();
            this.checkpoint = checkpoint;
        }

        public void assertCheckpointIs(Checkpoint checkpoint) throws Exception {
            eraseLatch.await(1L, SECONDS);
            assertThat(this.checkpoint, is(checkpoint));
        }
    }


    private class Mergings extends ForwardingCollection<Entry<Md5Key, Cursor>> {
        volatile int count;
        private final CountDownLatch latch;
        private final Set<Entry<Md5Key, Cursor>> entries;

        public Mergings(Set<Entry<Md5Key, Cursor>> entries) {
            this.entries = entries;
            this.latch = new CountDownLatch(1);
            count = 0;
        }

        @Override
        protected Collection<Entry<Md5Key, Cursor>> delegate() {
            return entries;
        }

        @Override
        public Iterator<Entry<Md5Key, Cursor>> iterator() {
            final Iterator<Entry<Md5Key, Cursor>> iterator = super.iterator();
            if (count++ == 0) return iterator; // skip index merging
            return new AbstractIterator<Entry<Md5Key, Cursor>>() {
                @Override
                protected Entry<Md5Key, Cursor> computeNext() {
                    try {
                        return iterator.hasNext() ? iterator.next() : endOfData();
                    } finally {
                        latch.countDown();
                    }
                }
            };
        }

        public void waitForIterated() throws InterruptedException {
            latch.await(1L, SECONDS);
        }
    }
}