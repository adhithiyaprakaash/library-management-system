import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Feeds a script to LibraryApp one line at a time and echoes each line, so a transcript looks like a real session. */
public class EchoRun {
    public static void main(String[] a) throws Exception {
        final Deque<String> lines = new ArrayDeque<>(Files.readAllLines(Paths.get(a[0])));
        System.setIn(new InputStream() {
            byte[] cur = new byte[0]; int pos = 0;
            public int read() { byte[] b = new byte[1]; return read(b, 0, 1) < 0 ? -1 : b[0]; }
            public int read(byte[] buf, int off, int len) {
                if (pos >= cur.length) {
                    if (lines.isEmpty()) return -1;
                    String l = lines.poll(); System.out.println(l);
                    cur = (l + "\n").getBytes(StandardCharsets.UTF_8); pos = 0;
                }
                int n = Math.min(len, cur.length - pos); System.arraycopy(cur, pos, buf, off, n); pos += n; return n;
            }
        });
        com.college.library.ui.LibraryApp.main(new String[] { a[1] });
    }
}
