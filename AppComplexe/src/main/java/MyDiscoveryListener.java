import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.bluetooth.BlueCoveConfigProperties;
import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.RemoteDeviceHelper;
import model.Position;
import model.User;

import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;

public class MyDiscoveryListener implements DiscoveryListener {

    private static Object lock=new Object();
    public ArrayList<RemoteDevice> devices;

    public MyDiscoveryListener() {
        devices = new ArrayList<RemoteDevice>();
    }
    public static void main(String[] args) {
        MyDiscoveryListener listener =  new MyDiscoveryListener();
        System.setProperty("bluecove.stack.first", BlueCoveImpl.STACK_WIDCOMM);
        try{
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            DiscoveryAgent agent = localDevice.getDiscoveryAgent();
            agent.startInquiry(DiscoveryAgent.GIAC, listener);

            try {
                synchronized(lock){
                    lock.wait();
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }


            System.out.println("Device Inquiry Completed. ");


            UUID[] uuidSet = new UUID[1];
            uuidSet[0]=new UUID(0x1105); //OBEX Object Push service

            int[] attrIDs =  new int[] {
                    0x0100 // Service name
            };

            for (RemoteDevice device : listener.devices) {
                agent.searchServices(
                        attrIDs,uuidSet,device,listener);

                System.out.println();
                try {
                    synchronized(lock){
                        lock.wait();
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }


                System.out.println("Service search finished.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass deviceClass) {
        String name;
        int rssi = 0;
        try {
            name = remoteDevice.getFriendlyName(true);
            rssi = RemoteDeviceHelper.readRSSI(remoteDevice);
        } catch (Exception e) {
            name = remoteDevice.getBluetoothAddress();
        }

        devices.add(remoteDevice);

        System.out.println("device found: " + name+"    "+rssi);

    }

    @Override
    public void inquiryCompleted(int arg0) {
        synchronized(lock){
            lock.notify();
        }
    }

    @Override
    public void serviceSearchCompleted(int arg0, int arg1) {
        synchronized (lock) {
            lock.notify();
        }
    }

    @Override
    public void servicesDiscovered(int transID, ServiceRecord[] serviceRecords) {
        for (int i = 0; i < serviceRecords.length; i++) {
            String url = serviceRecords[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
            if (url == null) {
                continue;
            }
            DataElement serviceName = serviceRecords[i].getAttributeValue(0x0100);
            if (serviceName != null) {
                System.out.println("service " + serviceName.getValue() + " found " + url);

                if(serviceName.getValue().equals("OBEX Object Push\u0000")){
                    sendMessageToDevice(url);
                }
            } else {
                System.out.println("service found " + url);
            }


        }
    }

    private static void sendMessageToDevice(String serverURL){
        try{
            System.out.println("Connecting to " + serverURL);

            ClientSession clientSession = (ClientSession) Connector.open(serverURL);
            HeaderSet hsConnectReply = clientSession.connect(null);
            if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
                System.out.println("Failed to connect");
                return;
            }

            HeaderSet hsOperation = clientSession.createHeaderSet();
            hsOperation.setHeader(HeaderSet.NAME, "Hello.txt");
            hsOperation.setHeader(HeaderSet.TYPE, "text");
            hsOperation.setHeader(HeaderSet.DESCRIPTION, "covid19");

            //Create PUT Operation
            Operation putOperation = clientSession.put(hsOperation);

            // Send some text to server
            User user = new User(InetAddress.getLocalHost().getHostName());
            user.addPosition(new Position(1, 200));
            user.addPosition(new Position(2, 200));
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(user);

            byte data[] = jsonString.getBytes("iso-8859-1");
            OutputStream os = putOperation.openOutputStream();
            os.write(data);
            os.close();

            putOperation.close();

            clientSession.disconnect(null);

            clientSession.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}