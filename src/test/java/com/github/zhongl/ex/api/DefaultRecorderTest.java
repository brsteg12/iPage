package com.github.zhongl.ex.api;

import com.github.zhongl.ex.index.Md5Key;
import com.github.zhongl.ex.journal.Journal;
import com.github.zhongl.ex.journal.Revision;
import com.github.zhongl.ex.util.Entry;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.Callable;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public class DefaultRecorderTest {

    private DefaultRecorder recorder;

    @Test
    public void usage() throws Exception {
        Journal journal = mock(Journal.class);
        QuanlityOfService quanlityOfService = mock(QuanlityOfService.class);
        FlowControllor controllor = spy(new FlowControllor());
        recorder = new DefaultRecorder(journal, quanlityOfService, controllor);

        byte[] value = "value".getBytes();
        Md5Key key = Md5Key.generate(value);

        recorder.append(key, value);
        verify(quanlityOfService).append(journal, new Entry<Md5Key, byte[]>(key, value));
        verify(controllor, times(1)).call(any(Callable.class));

        recorder.remove(key);
        verify(quanlityOfService).append(journal, new Entry<Md5Key, byte[]>(key, DefaultRecorder.NULL_VALUE));
        verify(controllor, times(2)).call(any(Callable.class));

        Revision revision = new Revision(1L);
        recorder.eraseTo(revision);
        verify(journal).eraseTo(revision);

    }

    @After
    public void tearDown() throws Exception {
        recorder.stop();
    }
}
