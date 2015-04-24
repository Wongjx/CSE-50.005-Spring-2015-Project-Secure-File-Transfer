package SecureFileTransfer;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Server1 {
	private ArrayList<Socket> clients;
	private ArrayList<Thread> serverListeners;
	
	public ServerSocket serverSocket;
	private Thread acceptThread;
	private final int PORT = 1044;
	
	private final String CA_CERT_PATH="C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/CA.crt";
	private final String SERVER_CERT_PATH="C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/server_1306.crt";
	private final String PRIVATE_KEY_PATH="C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/privateServer.der";
	
	private X509Certificate CACert;
	public X509Certificate ServerCert;
	private PublicKey CAkey;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	public Cipher cipher;
	public Cipher dcipher;
	public	Cipher publicDcipher; 
	
	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, CertificateException, NoSuchProviderException, SignatureException, NoSuchPaddingException, IOException{
		Server1 server = new Server1();
		server.startServer();
		System.out.println("Server running!");
	}
	
	public Server1() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, CertificateException, InvalidKeyException, NoSuchProviderException, SignatureException, NoSuchPaddingException{
		clients = new ArrayList<Socket>();
		serverListeners = new ArrayList<Thread>();
		
		serverSocket=new ServerSocket(PORT);			//Open server port
		
		CACert= getX509Cert(CA_CERT_PATH); 				//Get CA Cert
		ServerCert= getX509Cert(SERVER_CERT_PATH);		//Get CA signed server cert 	
		privateKey = getPrivateKey(PRIVATE_KEY_PATH);	//Get private key		
		publicKey = ServerCert.getPublicKey();			//Get public key
		CAkey = CACert.getPublicKey();					//get CA key
		
		//Check validity of CA cert and Public key
		 ServerCert.checkValidity();
		 ServerCert.verify(CAkey);
		 
		 //Create cipher 
		 cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		 cipher.init(Cipher.ENCRYPT_MODE, privateKey);
		 
		 dcipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		 dcipher.init(Cipher.DECRYPT_MODE, privateKey);
		 
		 publicDcipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		 publicDcipher.init(Cipher.DECRYPT_MODE, publicKey);
	}
	
	public byte[][] splitBytes(byte[] in){
		int NumOfArrays;
		
		if(in.length%128==0){
			NumOfArrays=in.length/128;
		} else{
			NumOfArrays=in.length/128+1;
		}
		
		byte[][] out = new byte[NumOfArrays][];
		
		for(int i=0;i<NumOfArrays;i++){
			//Split up bytes into 128 byte blocks
			if(((i+1)*128<in.length)){
				out[i] = Arrays.copyOfRange(in, i*128, (i+1)*128);	
//				System.out.println("Array size= "+out[i].length);
			}
			else{
				out[i]=Arrays.copyOfRange(in, i*128, in.length);
			}
		}
		
		return out;
	}
	
	public byte[] concatenateByte(byte[][] in) throws IOException{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		
		for (byte[] a:in){
			outputStream.write(a);
		}

		byte[] out = outputStream.toByteArray( );
		return out;
	}
	
	/**Generates a PrivateKey with the given privateKeyName in RSA 
	 * @return PrivateKey object
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	private static PrivateKey getPrivateKey(String file) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException{
		 File f = new File(file);
		 FileInputStream fis = new FileInputStream(f);
		 DataInputStream dis = new DataInputStream(fis);
		 byte[] keyBytes = new byte[(int)f.length()];
		 dis.readFully(keyBytes);
		 dis.close();
		
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory fac = KeyFactory.getInstance("RSA");
		return fac.generatePrivate(keySpec);
	}
	
	private X509Certificate getX509Cert(String file) throws CertificateException, IOException{
		 InputStream inStream = null;
		 X509Certificate cert;
		 try {
		     inStream = new FileInputStream(file);
		     CertificateFactory cf = CertificateFactory.getInstance("X.509");
		     cert = (X509Certificate)cf.generateCertificate(inStream);
		 } finally {
		     if (inStream != null) {
		         inStream.close();
		     }
		 }
		 return cert;
	}
	
	/** Encrypt message using sever private key
	 * @param message
	 * @return
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public byte[] encrypt(byte[] message) throws IllegalBlockSizeException, BadPaddingException{
		byte[] ret = cipher.doFinal(message);
		return ret;
	}
	
	/** Decrypt message using server private key
	 * @param message
	 * @return
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public byte[] decrypt(byte[] message) throws IllegalBlockSizeException, BadPaddingException{
		byte [] ret = dcipher.doFinal(message);
		return ret;
	}
	
	public synchronized void addClient(Socket clientSocket) throws IOException{
		clients.add(clientSocket);
		Thread t = new Thread(new clientListenerThread(this,clientSocket));
		serverListeners.add(t);
		t.start();
	}
	
	public synchronized void removeClient(Socket socket){
		int index=clients.indexOf(socket);
		serverListeners.remove(index);
		clients.remove(socket);
	}
	
	public void startServer(){
		acceptThread= new Thread(new serverAcceptThread(this));
		acceptThread.start();
	}
	
	public void stopServer() throws IOException{
		this.acceptThread.interrupt();	//stop accepting new connections
		for(Thread t:serverListeners){	//stop all current listeners
			t.interrupt();
		}
		serverSocket.close();
	}
}

class serverAcceptThread implements Runnable{
	private Server1 server;
	public serverAcceptThread(Server1 server){
		this.server=server;
	}
	
	@Override
	public void run(){
		while(!Thread.interrupted()){
			try {
				Socket socket = server.serverSocket.accept();
				System.out.println("Client connected.");
				server.addClient(socket);
				
				
			} catch (IOException e) {
				System.out.println("Error accepting connection: "+e.getMessage());
				e.printStackTrace();
			}
		}
	}
}

class clientListenerThread implements Runnable{
	private Server1 server;
	private Socket socket;
	
	private DataInputStream serverInput;
	private DataOutputStream serverOutput;
	
	public clientListenerThread(Server1 server,Socket socket) throws IOException{
		System.out.println("Listener thread created.");
		this.server=server;
		this.socket=socket;
		this.serverInput= new DataInputStream(socket.getInputStream());
		this.serverOutput = new DataOutputStream(socket.getOutputStream());
	}
	
	@Override
	public void run(){
		while (!Thread.interrupted()){
			try{
				int clientNonceSize = serverInput.readInt();		//Read and get client generated nonce
				byte[] clientNonce = new byte[clientNonceSize]; 
				serverInput.read(clientNonce);
				
				byte[] encrytpedNonce = server.encrypt(clientNonce);	//Encrypt nonce with server private key  
				serverOutput.writeInt(encrytpedNonce.length);
				serverOutput.write(encrytpedNonce);				//Send  encrypted key back to client
				
//				System.out.println("Recevied nonce: "+Arrays.toString(clientNonce));
//				System.out.println("Encrypted nonce: "+Arrays.toString(server.publicDcipher.doFinal(encrytpedNonce)));
				
				
				int messageSize = serverInput.readInt();		//Read the size of incoming byte message
				byte[] byteBuffer = new byte[messageSize];		//Prepare a byte[] buffer of the incoming byte message
				serverInput.read(byteBuffer);					 //Read clients request for public key certificate
				
				byte[] serverCert = server.ServerCert.getEncoded();				//Change cert to byte[]
				serverOutput.writeInt(serverCert.length);
				serverOutput.write(serverCert); 				//Send server cert in the form of byte array over
				
			}catch(Exception e){
				System.out.println("Error authenticating: "+e);
				e.printStackTrace();
			}

			try{
				int result =serverInput.readInt(); 			//Read result of client authentication
				if (result==1){
					System.out.println("AP succeded. Start receiving file");
					
					//Read start time of FTP operation
					long startTime = serverInput.readLong();
					
					//Read name of file 
					int nameSize =serverInput.readInt();
					byte[] fileNameBytes = new byte[nameSize];
					serverInput.readFully(fileNameBytes);
					String fileName = new String (fileNameBytes);
					
					
					//Method 1: Read as a single byte[]
//					//Read from client
//					int messageSize = serverInput.readInt();		
//					byte[] byteBuffer = new byte[messageSize];		
//					serverInput.read(byteBuffer);			
//					
//					byte[][] splitBytes = server.splitBytes(byteBuffer);
//					
//					byte[][] dSplitBytes= new byte[splitBytes.length][];
//					for (int i=0;i<splitBytes.length;i++){
//						dSplitBytes[i]=server.decrypt(splitBytes[i]);
//					}
//					
//					byte[] dByte = server.concatenateByte(dSplitBytes);
					
					//Method 2: Read in 128 byte blocks
					int num;
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );

					while((num=serverInput.readInt())!=-1){	//while not the end of file
						byte[] temp = new byte[num];
						serverInput.read(temp);		//read block into byte buffer
						temp=server.decrypt(temp);	//decrypt block
						outputStream.write(temp);	//write decrypted bytes into another byte buffer for the file
						
					}
					
					byte[] outBytes = outputStream.toByteArray( );
					long endTime = System.currentTimeMillis();
					System.out.println("Time taken for operation :"+(endTime-startTime));
					
//					System.out.println("Message size: "+messageSize);
//					System.out.println("File received from client: "+Arrays.toString(dByte));
					
					System.out.println("File name="+fileName);
					 FileOutputStream out = new FileOutputStream("C:/Users/Wong/Desktop/"+fileName);
					 out.write(outBytes);
					 out.close();
					
					
					break;
					
				}else {
					System.out.println("AP failed. Closing connection");
					break;
				}
			}catch(SocketException e){
				System.out.println("Error getting file: "+e);
				e.printStackTrace();
				break;
			}
			catch(Exception e){
				System.out.println("Error getting file: "+e);
				e.printStackTrace();
			}
		
		try{
			server.removeClient(socket);
			serverInput.close();
			serverOutput.close();
		}catch(Exception e){
			System.out.println("Error shutting down listener thread: "+e.getMessage());
		}
	}
}
}