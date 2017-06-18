package tracker;

import bencodeutils.BencodeUtils;
import bencodeutils.Element;
import internal.Constants;
import internal.TorrentMeta;
import org.apache.commons.io.IOUtils;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static internal.Constants.TIMEOUT;


/**
 * Created by ps on 26/3/17.
 * Represents a http tracker session.
 * This class extends TrackerSession so as to create higher level hierarchy.
 * This class implements tracker HTTP/HTTPS protocol.
 * https://wiki.theory.org/index.php/BitTorrentSpecification#Tracker_HTTP.2FHTTPS_Protocol
 */
public class HttpTrackerSession extends TrackerSession{

    private HttpURLConnection connection;
    public HttpTrackerSession(TorrentMeta meta,String announceUrl){
        super(meta,announceUrl);
    }
    public Object connect(TrackerRequestPacket packet) {
        URL url=null;
        Element element=null;
        try {
            url = new URL(getTrackerUrl(packet));
            connection = ((HttpURLConnection)(url.openConnection()));
            connection.setConnectTimeout(TIMEOUT);
            InputStream stream=connection.getInputStream();
            System.out.println(connection.getResponseCode());
            Constants.logger.debug("Received tracker response with response code :"+connection.getResponseCode());
            byte data[]= IOUtils.toByteArray(stream);
            element= BencodeUtils.decode(data);
            Constants.logger.debug("Successfully decoded tracker responses.");
        } catch (MalformedURLException e) {
            return  null;
        } catch (IOException e) {
            return null;
        }
        return element;
    }
}
