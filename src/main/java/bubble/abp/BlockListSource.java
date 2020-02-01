package bubble.abp;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.HttpUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.io.FileUtil.basename;

@NoArgsConstructor @Accessors(chain=true) @Slf4j
public class BlockListSource {

    public static final String INCLUDE_PREFIX = "!#include ";
    @Getter @Setter private String url;
    @Getter @Setter private String format;

    @Getter @Setter private Long lastDownloaded;
    public long age () { return lastDownloaded == null ? Long.MAX_VALUE : now() - lastDownloaded; }

    @Getter @Setter private BlockList blockList = new BlockList();

    public InputStream getUrlInputStream() throws IOException { return getUrlInputStream(url); }
    public static InputStream getUrlInputStream(String url) throws IOException { return HttpUtil.get(url); }

    public BlockListSource download() throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(getUrlInputStream()))) {
            String line;
            boolean firstLine = true;
            while ( (line = r.readLine()) != null ) {
                if (empty(line)) continue;
                line = line.trim();
                if (firstLine && line.startsWith("[") && line.endsWith("]")) {
                    format = line.substring(1, line.length()-1);
                }
                firstLine = false;
                addLine(url, line);
            }
        }
        lastDownloaded = now();
        return this;
    }

    public void addLine(String url, String line) throws IOException {
        if (line.startsWith(INCLUDE_PREFIX) && !empty(url)) {
            final String includePath = line.substring(INCLUDE_PREFIX.length()).trim();
            final String base = basename(url);
            final String urlPrefix = url.substring(0, url.length() - base.length());
            final String includeUrl = urlPrefix + includePath;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(getUrlInputStream(includeUrl)))) {
                String includeLine;
                while ( (includeLine = r.readLine()) != null ) {
                    addLine(includeUrl, includeLine);
                }
            } catch (Exception e) {
                throw new IOException("addLine: error including path: "+includeUrl+": "+shortError(e));
            }

        } else if (line.startsWith("!")) {
            // comment, nothing to add
            return;
        }
        try {
            if (line.startsWith("@@")) {
                blockList.addToWhitelist(BlockSpec.parse(line));
            } else {
                blockList.addToBlacklist(BlockSpec.parse(line));
            }
        } catch (Exception e) {
            log.warn("download("+url+"): error parsing line (skipping due to "+shortError(e)+"): " + line);
        }
    }

    public void addEntries(String[] entries) throws IOException { for (String entry : entries) addLine(null, entry); }

}
