package Tracker;

import BEcodeUtils.BencodeUtils;
import BEcodeUtils.Element;
import internal.Constants;
import internal.TorrentMeta;
import internal.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static internal.Constants.*;

/**
 * Created by ps on 30/3/17.
 * For UdpTrackerSession some extra work has to be done.
 * First we have to send a 'connection' request which will return a transaction id.
 * transaction id will be used in sending announce request.
 * Refer : http://www.bittorrent.org/beps/bep_0015.html
 */
public class UdpTrackerSession extends TrackerSession {

    DatagramSocket socket;
    private int transactionId;
    private Logger logger;
    private int ACTION_CONNECT=0;
    private int ACTION_ANNOUNCE=1;
    public UdpTrackerSession(TorrentMeta meta){
        super(meta);
        this.logger=Constants.logger;
    }
    public Object connect(TrackerRequestPacket packet) {
        URL url=null;
        ByteBuffer response=null;
        logger.debug("Request is of UDP type.");
        try {
            //url = new URL(getTrackerUrl(packet));
            URI uri=new URI(meta.getAnnounce());
            System.out.println();
            InetSocketAddress address=new InetSocketAddress(uri.getHost(),uri.getPort());
            System.out.println("InetAddress is: "+address.toString());
            socket=new DatagramSocket();
            socket.connect(address);

            //send a connection request first with given transaction id.
            logger.debug("Sending UDP  connection request to "+address.toString());
            byte[] data=createConnectionRequest();
            DatagramPacket datagramPacket=new DatagramPacket(data,data.length,address);
            socket.send(datagramPacket);
            ByteBuffer buffer=receive(socket);
            long connectionId=parse(buffer);
            logger.debug("Received connectionId :"+connectionId+" "+"sending announce request.");
            byte[] announceRequest=craftAnnounceRequest(connectionId,packet);
            DatagramPacket announcePacket=new DatagramPacket(announceRequest,announceRequest.length,address);
            socket.send(announcePacket);
            response=receive(socket);
            System.out.println(response.getInt());
            System.out.println("transactionId "+response.getInt()+" Interval "+response.getInt()+" leechers "+response.getInt()+" seeders "+response.getInt());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return response;
    }
    private byte[] createConnectionRequest(){
        ByteBuffer buffer=ByteBuffer.allocate(UDP_CONNECTION_MESSAGE_LEN);
        buffer.putLong(UDP_CONNECT_REQUEST_MAGIC);
        buffer.putInt(ACTION_CONNECT);
        this.transactionId=Utils.generateRandomNumber();
        System.out.println("Generated transaction id is :"+transactionId);
        buffer.putInt(transactionId);
        return buffer.array();
    }
    private ByteBuffer receive(DatagramSocket socket){
        byte array[]=new byte[UDP_PACKET_LENGTH];
        DatagramPacket  packet=new DatagramPacket(array,array.length);
        try {
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Received response from socket :"+packet.getLength());
        return ByteBuffer.wrap(packet.getData(),0,packet.getLength());
    }

    /*
        parse a connect response.
        A connectioon response contains following data.
        action(int).
        transactionId(int)
        connectionId(long)
     */
    private long  parse(ByteBuffer buffer){
        int action =buffer.getInt();
        System.out.println("Action is :"+action);
        int transactionId=buffer.getInt();
        long connectionId=buffer.getLong();
        System.out.println("transactiondId "+transactionId+" connectionId "+connectionId);
        return connectionId;
    }

    private byte[] craftAnnounceRequest(long connectionId,TrackerRequestPacket packet){
        ByteBuffer buffer=ByteBuffer.allocate(UDP_ANNOUNCE_MESSAGE_LEN);
        try {
            String str=new String(meta.getInfo_hash(),"ISO-8859-1");
            buffer.putLong(connectionId);
            buffer.putInt(ACTION_ANNOUNCE);
            buffer.putInt(transactionId);
            System.out.println("Info hash :"+str);
            buffer.put(str.getBytes("ISO-8859-1"));
            System.out.println("Peer id: "+ID);
            buffer.put(ID.getBytes("ISO-8859-1"));
            System.out.println("downloaded: "+packet.getDownloaded());
            buffer.putLong(packet.getDownloaded());
            System.out.println("left :"+packet.getLeft());
            buffer.putLong(packet.getLeft());
            System.out.println("uploaded :"+packet.getUploaded());
            buffer.putLong(packet.getUploaded());
            System.out.println("event :"+packet.getEvent().getValue());
            buffer.putInt(packet.getEvent().getValue());
            buffer.putInt(0); //default IP address.
            buffer.putInt(0); //default key.
            buffer.putInt(-1);//num_want(don't know what is that.)
            System.out.println("Port :"+6881);
            buffer.putShort((short) (6881 & 0xFFFF));
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return buffer.array();
    }

}
