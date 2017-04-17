package storage.netty;

import java.io.File;
import java.util.Timer;

public class Program {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//HttpUploadClient client = new HttpUploadClient("account name","account key");
		HTTPUploadClientAsync client2 = new HTTPUploadClientAsync("account name","account key");
	
		try {
			//File[] files = new File("C:\\test").listFiles();
			
			long start = System.currentTimeMillis();
			
			//for (File file : files) {
				client2.putBlob(args[0]);
			//}
			
			long stop = System.currentTimeMillis();
			System.out.println((stop - start)/1000 + "seconds");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
