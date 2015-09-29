// SimpleConnectionTest
// Started July 17, 2015
// Updated September 29, 2015
//
// For reference please see the following
// * http://docs.pub.couchbase.com/couchbase-sdk-java-1.4/
// * http://docs.pub.couchbase.com/couchbase-sdk-java-1.4/#hello-couchbase

package com.couchbase.support;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;

class NetworkCheckThread implements Runnable {

	public long getTimeOFLastException() {
		return timeOfLastException;
	}
	
	public int getTotalExceptionsSeen() {
		return totalExceptionsSeen;
		
	}
	
	private long timeOfLastException;
	private int totalExceptionsSeen;
	
	public void run() {
	
		timeOfLastException = 0;
		totalExceptionsSeen = 0;
		
		long t1 = 0, t2 = 0;
		
		while (true) {

			t1 = System.currentTimeMillis();

			try {
				URL tryGoogle = new URL("http://www.google.com");
				HttpURLConnection urlConnection = (HttpURLConnection) tryGoogle.openConnection();
				urlConnection.setConnectTimeout(500);
				urlConnection.setReadTimeout(500);

				urlConnection.setRequestMethod("GET");
				int responseCode = urlConnection.getResponseCode();

				t2 = System.currentTimeMillis();
				System.out.println("NetworkCheckThread: Google says " + responseCode 
						+ " after about " + ( t2 - t1 ) + " ms");
			}
			catch (Exception e) {
				timeOfLastException = System.currentTimeMillis();
				totalExceptionsSeen++;
				t2 = System.currentTimeMillis();
				System.out.println("NetworkCheckThread: Saw an exception.  Was blocked for about " + ( t2 - t1 ) + " ms");
			}

			try {
				Thread.sleep(1000);
			}
			catch(Exception e) {
				System.out.println("NetworkCheckThread: Exception while sleeping");
			}
		}
		
	}
		
}


public class SimpleConnectionTest {

	public static void main(String[] args) {

		System.out.println("Starting SimpleConnectionTest");

		try {

			NetworkCheckThread nct = new NetworkCheckThread();
			new Thread(nct).start();
			
			List<URI> uriList = new ArrayList<URI>();
			URI u = new URI("http://10.4.2.121:8091/pools");
			uriList.add(u);
			String bucketname = "BUCKETNAME", password = "";
			
			CouchbaseConnectionFactoryBuilder cfb = new CouchbaseConnectionFactoryBuilder();
			cfb.setOpTimeout(25);
			// cfb.setTimeoutExceptionThreshold(0);
			// cfb.setOpQueueMaxBlockTime(500);
			
			//cfb.setAuthWaitTime(500);
			//cfb.setReconnectThresholdTime(1, TimeUnit.SECONDS);
			//cfb.setFailureMode(FailureMode.Cancel);

			CouchbaseConnectionFactory cf = cfb.buildCouchbaseConnection(uriList, bucketname, password);
			CouchbaseClient cc = new CouchbaseClient(cf);
		
			int MAXDOCUMENTNUMBER = 100000;
			String documentValue = "This is my document";
			String returnedDocument = null;
			
			int returnedDocumentWasNull = 0;
			int documentsMatch = 0;
			int documentsDontMatch = 0;
			int exceptionDuringSet = 0;
			int exceptionDuringGet = 0;
			
			long lastSetExceptionTime = 0;
			long lastGetExceptionTime = 0;
			
			boolean keepGoing = true;
			
			while (keepGoing) {

				// Choose a document key at random
				int randomIdentifier = (int) (Math.random() * MAXDOCUMENTNUMBER);
				String documentKey = "document" + randomIdentifier;

				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date date = new Date();
				String formattedDate = dateFormat.format(date);
				
				long t1, t2;
				
				System.out.println("################### Top of main loop - " + formattedDate 
						+ " (" + documentKey + ") ###################");

				// Do a set to write the document to Couchbase
				System.out.println("About to call set()");
				try {
					t1 = System.currentTimeMillis();
					cc.set(documentKey, documentValue);
					t2 = System.currentTimeMillis();
					System.out.println("set() took " + ( t2 - t1 ) + " ms");
				}
				catch (Exception e) {
					System.out.println("################### Exception during set() ###################");
					lastSetExceptionTime = System.currentTimeMillis();
					e.printStackTrace();
					exceptionDuringSet++;
				}
				
				System.out.println("About to call get()");
				try {
					t1 = System.currentTimeMillis();
					returnedDocument = (String) cc.get(documentKey);
					t2 = System.currentTimeMillis();
					System.out.println("get() took " + ( t2 - t1 ) + " ms");
				}
				catch (Exception e) {
					System.out.println("################### Exception during get() ###################");
					lastGetExceptionTime = System.currentTimeMillis();
					e.printStackTrace();
					exceptionDuringGet++;
				}

				if (returnedDocument == null) {
					returnedDocumentWasNull++;
				}
				else {
					if (returnedDocument.equals(documentValue)) {
						documentsMatch++;
					}
					else {
						documentsDontMatch++;
					}
				}
				
				System.out.printf("Exceptions during Set: %d Get: %d                 # times get() was null: %d  docs match: %d  no match: %d\n", 
						exceptionDuringSet, exceptionDuringGet, returnedDocumentWasNull, 
						documentsMatch, documentsDontMatch);
				
				if (lastSetExceptionTime == 0) {
					System.out.println("Have not seen a set() exception");
				}
				else {
					System.out.println("Time since last set() exception:" + 
							( System.currentTimeMillis() - lastSetExceptionTime ));
				}

				if (lastGetExceptionTime == 0) {
					System.out.println("Have not seen a get() exception");
				}
				else {
					System.out.println("Time since last get() exception:" + 
							( System.currentTimeMillis() - lastGetExceptionTime ));
				}

				if (nct.getTimeOFLastException() == 0) {
					System.out.println("Have not seen a Google exception");
				}
				else {
					System.out.println("Time since last Google exception:" + 
							( System.currentTimeMillis() - nct.getTimeOFLastException() )
							+ " Total seen: " + nct.getTotalExceptionsSeen());
				}
				
				
				try {
					Thread.sleep(1000);
				}
				catch(Exception e) {
					System.out.println("main thread: Exception while sleeping");
				}

				System.out.println("################### Bottom of main loop ###################");

			}

			System.out.println("################### About to Shutdown CC ###################");
			cc.shutdown();
			
		}
		catch(Exception e) {
			System.out.println("################### Caught Exception ###################");
			e.printStackTrace();
		}
		
		
		System.out.println("Leaving SimpleConnectionTest");
		System.out.println("################### GOODBYE!!!! ###################");
		System.exit(1);
		
	}

}

// EOF