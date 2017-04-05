package storage.netty;

import java.io.File;
import java.util.Timer;

public class Program {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		HttpUploadClient client = new HttpUploadClient("","");
		
		try {
			File[] files = new File("C:\\test").listFiles();
			
			long start = System.currentTimeMillis();
			
			//for (File file : files) {
				client.putBlob("C:\\test\\sample.txt");
			//}
			
			long stop = System.currentTimeMillis();
			System.out.println((stop - start)/1000);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
