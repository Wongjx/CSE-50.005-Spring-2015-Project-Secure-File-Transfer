package SecureFileTransfer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class CP1_Client {
	private Socket socket;
	private DataOutputStream clientOutput;
	private DataInputStream clientInput;
	
	private final int PORT=1044;
	private final String HOST="127.0.0.1";
	
	private final String CA_CERT_PATH="C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/CA.crt";
	
	private HashSet<byte[]> NonceSet;
	
	private X509Certificate CACert;
	private PublicKey CAkey;
	
	private X509Certificate serverCert;
	private PublicKey publicKey;	
	private Cipher dcipher;
	private Cipher cipher;
	
	
	public static void main(String[] args) {
		CP1_Client client = null;
//		File fileToSend = new File("C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/test.txt");
//		File fileToSend = new File("C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/test2.txt");
//		File fileToSend = new File("C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/test3.txt");
//		File fileToSend = new File("C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/CP_1 Sequence Diagram.jpg");
//		File fileToSend = new File("C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/FTP_diagrams.vpp");
//		File fileToSend = new File("C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/test.txt.txt");
		File fileToSend = new File("C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/test1.pdf");
//		File fileToSend = new File("C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/test2.class");
//		File fileToSend = new File("C:/Users/Wong/Dropbox/Academics/50.005 Com Systems Engineering/ComSystems/Assigments/src/SecureFileTransfer/Hearthstone_Screenshot_12.8.2014.17.04.47.png");

		try{
			client = new CP1_Client() ;
			client.init();
			if(client.sendFile(fileToSend)){
				System.out.println("File transfer succeded");
			}else{
				System.out.println("File transfer failed");
			}
			client.endSeisson();
			
		}catch(Exception e){
			System.out.println("Error: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public CP1_Client() throws UnknownHostException, IOException{
		socket = new Socket(HOST,PORT);
	}
	
	public void init() throws IOException, CertificateException{
		clientOutput=new DataOutputStream(socket.getOutputStream());
		clientInput = new DataInputStream(socket.getInputStream());
		CACert = getX509Cert(CA_CERT_PATH);
		CACert.checkValidity();
		CAkey = CACert.getPublicKey();
		NonceSet = new HashSet<byte[]>();
	}
	
	public boolean sendFile(File file) throws InvalidKeyException, ClassNotFoundException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException{
		if(startAP()){
			System.out.println("AP succeed!");
			clientOutput.writeInt(1);	//Tell server AP succeeded
			
			//Read bytes from file
			FileInputStream fis;
			fis = new FileInputStream(file);
			byte[] dataByte = new byte[(int) file.length()];
			fis.read(dataByte);
			fis.close();
			
			//Send start time over
			clientOutput.writeLong(System.currentTimeMillis());
			
			//Send filename over
			byte[] nameOfFile=file.getName().getBytes();
			clientOutput.writeInt(nameOfFile.length);
			clientOutput.write(nameOfFile);
	        
			byte[][] splitBytes = splitBytes(dataByte);			//split bytes into blocks of 117 bytes each
			
//			//Encrypt each block
//			byte[][] dSplitBytes= new byte[splitBytes.length][];
//			for (int i=0;i<splitBytes.length;i++){
//				dSplitBytes[i]=encrypt(splitBytes[i]);
//			}
//			byte[] eDataByte = concatenateByte(dSplitBytes);
//			
//			//send file over to server
//			clientOutput.writeInt(eDataByte.length);
//			clientOutput.write(eDataByte);
			
			//Method 2: Encrypt and send as blocks of size 128 bytes
			//Encrypt each block
			for (int i=0;i<splitBytes.length;i++){
				byte[] temp =encrypt(splitBytes[i]);
				clientOutput.writeInt(temp.length);
				clientOutput.write(temp);
//				System.out.println("Block being sent: "+new String(temp));
			}
			
			//Signal end of file send
			clientOutput.writeInt(-1);
			
			
//			System.out.println("File as byte[]: "+Arrays.toString(dataByte));
//			System.out.println("Encrypted file as byte[]: "+Arrays.toString(eDataByte));
			
//			System.out.println("Message Length: "+eDataByte.length);
			System.out.println("Size of file: "+dataByte.length);
//			System.out.println("Content of file being sent over: "+new String(dataByte));
			return true;
		}else{
			System.out.println("Ap failed!");
			clientOutput.writeInt(0);	//AP has failed, close connection
			return false;
		}
	}
	
	/** Encrypt using Server's public key
	 * @param message
	 * @return
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	private byte[] encrypt(byte[] message) throws IllegalBlockSizeException, BadPaddingException{
		return cipher.doFinal(message);
	}
	
	/**	Decrypt using Server's public key
	 * @param message
	 * @return
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	private byte[] decrypt(byte[] message) throws IllegalBlockSizeException, BadPaddingException{
		return dcipher.doFinal(message);
	}
	
	
	private byte[][] splitBytes(byte[] in){
		int NumOfArrays;
		
		if(in.length%128==0){
			NumOfArrays=in.length/117;
		} else{
			NumOfArrays=in.length/117+1;
		}
		
		byte[][] out = new byte[NumOfArrays][];
//		System.out.println("Size of array ="+in.length);
//		System.out.println("Number of Arrays ="+NumOfArrays);
		
		for(int i=0;i<NumOfArrays;i++){
			//Split up bytes into 117 byte blocks
			if((i+1)*117<in.length){
				out[i] = Arrays.copyOfRange(in, i*117, (i+1)*117);				
			}
			else{
				out[i]=Arrays.copyOfRange(in, i*117, in.length);
			}
		}
		
		return out;
	}
	
	private byte[] concatenateByte(byte[][] in) throws IOException{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		
		for (byte[] a:in){
			outputStream.write(a);
		}

		byte[] out = outputStream.toByteArray( );
		return out;
	}
	
	private X509Certificate getX509Cert(String file) throws CertificateException, IOException {
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
	
	private X509Certificate getX509Cert(byte[] input) throws CertificateException, IOException {
		InputStream in = new ByteArrayInputStream(input);
		
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate)cf.generateCertificate(in);
		 return cert;
	}
	
	private Cipher getdCipher(PublicKey publicKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException{
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, publicKey);
		 return cipher;
	}
	
	private Cipher getCipher(PublicKey publicKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException{
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		 return cipher;
	}
	
	public void endSeisson() throws IOException{
		this.NonceSet.clear();
		this.socket.close();
		
		
	}
	
	/** Start authentication protocol with server
	 * @return boolean: True if AP succeeds; False if AP Fails
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InvalidKeyException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws SignatureException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public boolean startAP() throws IOException, ClassNotFoundException, InvalidKeyException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{
		//Contact server saying want to start AP for a connection
		
		
		//Generate nonce and send to server
//		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//		symKey = keyGen.generateKey(); 
		
		byte[] nonce;
		SecureRandom ran = new SecureRandom();
		do{
			int nonceGen = ran.nextInt();
			ByteBuffer b = ByteBuffer.allocate(4);
			b.putInt(nonceGen);
			byte[] nonceByte = b.array();
			nonce = nonceByte;	
		}while (NonceSet.contains(nonce));
	
//		System.out.println("Sending nonce to server.");			
//		byte[] nonce = symKey.getEncoded();						//Send nonce(symkey) to server
		
		NonceSet.add(nonce);
		clientOutput.writeInt(nonce.length);
		clientOutput.write(nonce);
//		System.out.println("Message sent to server: "+nonce);
		
		int proofMessageSize=clientInput.readInt();				//Get encrypted nonce from server
		byte[] proofMessage = new byte[proofMessageSize]; 
		clientInput.read(proofMessage);
		
//		System.out.println("Request certificate from server.");	
		byte[] message = "request_cert".getBytes();
		clientOutput.writeInt(message.length);
		clientOutput.write(message);
//		System.out.println("Message sent to server: "+message);
		
//		int certMessageSize=clientInput.readInt();	
//		byte[] certMessage = new byte[certMessageSize]; 
//		clientInput.read(certMessage);
		
		int certMessageSize=clientInput.readInt();				//Get encrypted nonce from server
		byte[] certMessage = new byte[certMessageSize]; 
		clientInput.read(certMessage);
		serverCert=getX509Cert(certMessage);	//Get cert from server
		
		//Verify certificate and get public key from cert
//		System.out.println("Get public key from certificate");
		serverCert.checkValidity();
		serverCert.verify(CAkey);		//Verify server cert against CA key
		publicKey = serverCert.getPublicKey();
		
//		System.out.println("Initialize cipher from server public key");
		dcipher = getdCipher(publicKey); 	//Initialize decrypting cipher
		cipher = getCipher(publicKey);
		
//		System.out.println("Decrypt authentication message");
		byte[] dDataByte = dcipher.doFinal(proofMessage); 	//Decrypt message using public key from CA
		System.out.println("Deciphered Nonce from server: "+Arrays.toString(dDataByte));
		System.out.println("Generated Nonce : "+Arrays.toString(nonce));
		if (Arrays.equals(dDataByte,nonce)){
			return true;
		}
		return false;
	}
}
