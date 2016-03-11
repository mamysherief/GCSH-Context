package robotcomm;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
public class Bluetooth{
    private static LocalDevice localDevice; // local Bluetooth Manager
    private static DiscoveryAgent discoveryAgent; // discovery agent
    /**
     * Initialize Bluetooth unit on PC
     */
    static void btInit()  {
        try {
            localDevice = null;
            discoveryAgent = null;
            // Retrieve the local device to get to the Bluetooth Manager
            localDevice = LocalDevice.getLocalDevice();
            // Servers set the discoverable mode to GIAC
            localDevice.setDiscoverable(DiscoveryAgent.GIAC);
            // Clients retrieve the discovery agent
            discoveryAgent = localDevice.getDiscoveryAgent();
        }
        catch (BluetoothStateException bse) {
            bse.printStackTrace();
        }
        System.out.println("Bluetooth Initialization Complete");
    }
   
    static String detectLocal() {
        String address = "";
        LocalDevice ld;
       
        try {
            ld = LocalDevice.getLocalDevice();
            address = localDevice.getBluetoothAddress();
            System.out.println("Local Device Properties: " + ld.getProperty(address) + " Friendly Name = " + ld.getFriendlyName());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }
   
    static String readKBInput() {
        String inread = "";
        InputStreamReader in = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(in);
        try {
                 inread = br.readLine();
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }               
        return inread;
    }
    static Vector remoteDiscovery() {
        Vector devicesDiscovered = new Vector();
        RemoteDevice rd;
        int i= 0;
        RemoteDeviceDiscoveryR rdd = new RemoteDeviceDiscoveryR();
        devicesDiscovered = rdd.getRemoteDevices();
        while (i < devicesDiscovered.size()) {
            rd = (RemoteDevice) devicesDiscovered.get(i);
            try {
                System.out.println(i + ".. " + rd.getFriendlyName(false) + "\t\t -- \t" + rd.getBluetoothAddress());
            }
            catch (IOException ioe) {
            }
            i++;
        }
        System.out.print("Enter device number to search service on:- ");
        i = Integer.parseInt(readKBInput());
        ServiceSearcherR ss = new ServiceSearcherR((RemoteDevice) devicesDiscovered.get(i));
        System.out.println("== BACK FROM REMOTE DICOVERY:: " + devicesDiscovered.size() + " devices detected. \n");
        return ss.getServices();
    }
   
    public static void main(String[] args) {
        Vector services;
        String clientURL = "";
        btInit();
        System.out.println("Bluetooth Discovered on this machine at address " + detectLocal());
         services = remoteDiscovery();
          if (services.size()  > 0) {
            clientURL = (String) services.remove(0);
            System.out.println("**** In MAIN **** Service URL = " + clientURL + ".");
        }
        RFcommClientRobot clientc = new RFcommClientRobot(clientURL);
        clientc.start();
    }
}
 
//**********************************************************************
class RemoteDeviceDiscoveryR {
    private Vector/*<RemoteDevice>*/ devicesDiscovered = new Vector();
    public RemoteDeviceDiscoveryR() {   
    }
    public Vector getRemoteDevices() {
        try {
            discoveryMain();
        }
        catch (IOException ie) {
            ie.printStackTrace();
        }
        catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        return devicesDiscovered;
    }
    private void discoveryMain() throws IOException, InterruptedException {
        final Object inquiryCompletedEvent = new Object();
        devicesDiscovered.clear();
        DiscoveryListener listener = new DiscoveryListener() {
            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                devicesDiscovered.addElement(btDevice);
            }
            public void inquiryCompleted(int discType) {
                synchronized(inquiryCompletedEvent){
                    inquiryCompletedEvent.notifyAll();
                }
            }
            public void serviceSearchCompleted(int transID, int respCode) {
            }
            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
            }
        };
        synchronized(inquiryCompletedEvent) {
            boolean started = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, listener);
            if (started) {
                System.out.println("wait for device inquiry to complete...");
                inquiryCompletedEvent.wait();
            }
        }
    }   
}
//*****************************************
class ServiceSearcherR {   
    static final UUID OBEX_FILE_TRANSFER = new UUID(0x1106);
    static final UUID OBEX_OBJECT_PUSH = new UUID(0x1106);        //?????
    private    int leng = 0;
    private Vector remoteDevices;
    RemoteDevice btDevice;
    public static final Vector/*<String>*/ serviceFound = new Vector();
    public ServiceSearcherR(RemoteDevice btDevice) {
        this.btDevice = btDevice;
        try {
            System.out.println("Searching for service.... device = " + btDevice.getFriendlyName(false) + "....");
            mainSearcher( "0000110100001000800000805F9B34FB");
        }
        catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        catch (IOException ie) {
            ie.printStackTrace();
        }
       
    }
    public Vector getServices() {
        return serviceFound;
    }
    private void mainSearcher(String servString) throws IOException, InterruptedException {
        serviceFound.clear();
        UUID serviceUUID = OBEX_OBJECT_PUSH;
        if ((servString.length() > 0)) {
            serviceUUID = new UUID(servString, false);
        }
        final Object serviceSearchCompletedEvent = new Object();
        DiscoveryListener listener = new DiscoveryListener() {
            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
            }
            public void inquiryCompleted(int discType) {
            }
            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
               
                for (int i = 0; i < servRecord.length; i++) {
                    String url = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                    if (url == null) {
                        continue;
                    }
                    serviceFound.add(url);
                    DataElement serviceName = servRecord[i].getAttributeValue(0x0100);
                    if (serviceName != null) {
                        System.out.println("service " + serviceName.getValue() + " found " + url);
                    } else {
                        System.out.println("service found " + url);
                    }
                    leng++;
                }
            }
            public void serviceSearchCompleted(int transID, int respCode) {
                System.out.println("service search completed! " + leng  + " service(s) found.");
                synchronized(serviceSearchCompletedEvent){
                    serviceSearchCompletedEvent.notifyAll();
                }
            }
        };
        UUID[] searchUuidSet = new UUID[] { serviceUUID };
        int[] attrIDs =  new int[] {
                0x0100 // Service name
        };
        if (btDevice != null) {
            synchronized(serviceSearchCompletedEvent) {
                System.out.println("search services on " + btDevice.getBluetoothAddress() + " " + btDevice.getFriendlyName(false));
                LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(attrIDs, searchUuidSet, btDevice, listener);
                serviceSearchCompletedEvent.wait();
            }
        }
    }
}
 
//*****************************************************************
class RFcommClientRobot extends Thread {
    private static final String myServiceUUID = "0000110100001000800000805F9B34FB";        //"2d26618601fb47c28d9f10b8ec891363";
    private UUID MYSERVICEUUID_UUID = new UUID(myServiceUUID, false);
    private String serviceURL;
    private OutputStream os;
   
    public RFcommClientRobot() {
       
    }
    public RFcommClientRobot(String serviceURL) {
        this.serviceURL = serviceURL;
    }
    static byte readInput() {
        String inread = "";
        InputStreamReader in = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(in);
        try {
                 inread = br.readLine();
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }       
        byte b = (byte) inread.charAt(0);
       // byte b =byte() inread.charAt(0);
      // String hex =Integer.toHexString(b)
        return b;
    }
    public void run() {
        try {
             TCPserver t = new TCPserver();
             t.start();
            String url = "btspp://B0D09C5720EA:3";
            StreamConnection con = (StreamConnection) Connector.open(serviceURL);
            System.out.println("**** In RFcommclient run method **** Service URL = " + serviceURL + ".");
            os = con.openOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(os);
            System.out.println("Client Connection opened at " + con.toString());
            byte buffer[] = new byte[6];
            
            int ii = 0;        
            while (ii < 6) {
                //System.out.print("\nPlease Enter command:- ");
               /* if(flag == 1){
                 buffer[0] ='h';//readInput();
                 .flag == 0;
                }*/
                
                // System.out.println(t.getS());
                //if (buffer[0] == 'q') break;
                //if (buffer[0] == 0) continue;
                if(t.getS() == null ? ("h") == null : t.getS().equals("h")){// hand up
                    	System.out.print("\n HAND UP ");// FF FE
                    	String gesture = "FF FE";   //7F 7E FF = 1111 1111 -> 0111 1111 `
                        //byte message[] =  gesture.getBytes();
                    	//String s = new String(message);
                    	//String stringToReverse = URLEncoder.encode(gesture, "UTF-8");
                       	//os.write(stringToReverse);
                       	//os.flush();
                        buffer[0] = '1';
                    	
                        System.out.println("WRITEＣＨＡＲＳ"+gesture);
                        dataOutputStream.flush();
                        os.write(buffer);
                        t.setS("");
                    	
                } 
                  if(t.getS() == null ? ("j") == null : t.getS().equals("j")){// hand up
                    	System.out.print("\n HAND Down ");// FF FE
                    	String gesture = "FF FE";   //7F 7E FF = 1111 1111 -> 0111 1111 `
                        //byte message[] =  gesture.getBytes();
                    	//String s = new String(message);
                    	//String stringToReverse = URLEncoder.encode(gesture, "UTF-8");
                       	//os.write(stringToReverse);
                       	//os.flush();
                        buffer[0] = '2';
                    	
                        System.out.println("WRITEＣＨＡＲＳ"+gesture);
                        dataOutputStream.flush();
                        os.write(buffer);
                        t.setS("");
                    	
                } 
               //os.write(buffer);
               // os.write(buffer);
            }
            /*
            if(buffer[0]== 'h'){// hand up
                    	System.out.print("\n HAND UP ");// FF FE
                    	String gesture = "FF FE";   //7F 7E FF = 1111 1111 -> 0111 1111 `
                        //byte message[] =  gesture.getBytes();
                    	//String s = new String(message);
                    	//String stringToReverse = URLEncoder.encode(gesture, "UTF-8");
                       	//os.write(stringToReverse);
                       	//os.flush();
                    	
                    	dataOutputStream.writeChars(gesture);
                        System.out.println("WRITEＣＨＡＲＳ"+gesture);
                        dataOutputStream.flush();
                    	
                }
            */
            con.close();
        }
        catch ( IOException e ) {
            System.err.println("Unable to Connect to Bluetooth Device.  Check connection! \nProgram is now QUITTING...");
            return;
        }
    }
}

