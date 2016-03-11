/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package robotcomm;

/**
 *
 * @author Pearson
 */
import java.io.*; 
import java.net.*; 


public class TCPserver extends Thread {
    String clientSentence;
    public int flag = 0;//
    //public static void main(String argv[]) throws Exception   
    
   
    public void run()
    {         
        //String clientSentence;          
        String capitalizedSentence; 
        try {
        ServerSocket welcomeSocket = new ServerSocket(8080);  
        Socket connectionSocket = welcomeSocket.accept();      
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            //System.out.println("11111111111");
       
        while(true)          
        {       
            System.out.println("AA"); 
            clientSentence = inFromClient.readLine();
                setS(clientSentence);
            System.out.println("Received: " + clientSentence); 
            capitalizedSentence = clientSentence.toUpperCase() + '\n';
            outToClient.writeBytes(capitalizedSentence);
            int length_t = clientSentence.length();
            if(length_t > 0)
            {
                flag = 1;
                length_t = 0;
            }
        } 
        } catch (Exception e){
        
        }
    }
        
    public void setS(String command) {
            this.clientSentence = command;
        }
    
    public String getS() {
   
        return clientSentence;
    }
    }