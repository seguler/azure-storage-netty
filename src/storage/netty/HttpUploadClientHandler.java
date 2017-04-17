/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package storage.netty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Map.Entry;

import javax.activation.MimetypesFileTypeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

/**
 * Handler that just dumps the contents of the response from the server
 */
public class HttpUploadClientHandler extends SimpleChannelInboundHandler<HttpObject> {

    private boolean readingChunks;
    private String account_name = "sercan";
    private URI url =  URI.create("http://" + account_name + ".blob.core.windows.net");
    private String account_key= "ofibC2H0GHDP2sBMp/+yd6gFO3Rk+47MYi1kaOGa5Iy0cohE2KcRq3vDvlbRfKzdfyADfFsjN0joJLILa1qQDw==";
    private String FILE = "C:\\jdk-8u121-windows-x64.exe";
    final File file = new File(FILE);
    private String resourceUrl;
    private Mac hmacSha256;
    private byte[] key;

    final String AB = "abcdefghijklmnopqrstuvwxyz";
    SecureRandom rnd = new SecureRandom();
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws InvalidKeyException, IOException, InterruptedException {
        System.out.println("Sending: "+ ctx.channel().localAddress());  
        RandomAccessFile raf = new RandomAccessFile(ctx.channel().attr(HTTPUploadClientAsync.FILETOUPLOAD).get(), "r");
        long fileLength = raf.length();
        
        HttpRequest request = ctx.channel().attr(HTTPUploadClientAsync.HTTPREQUEST).get();
        ChannelFuture cf = ctx.channel().writeAndFlush(request);
        if (!cf.isSuccess()) {
            System.out.println("Send failed: " + cf.cause());
        }
        ctx.channel().write(new DefaultFileRegion(raf.getChannel(), 0, fileLength));
	      
        // Write the end marker.
        ctx.channel().writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
    	System.out.println("Channel closed");
    	ctx.channel().close();
    }
      
    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;

            System.err.println("STATUS: " + response.status());
            System.err.println("VERSION: " + response.protocolVersion());

            if (!response.headers().isEmpty()) {
                for (CharSequence name : response.headers().names()) {
                    for (CharSequence value : response.headers().getAll(name)) {
                        System.err.println("HEADER: " + name + " = " + value);
                    }
                }
            }

            if (response.status().code() == 200 && HttpUtil.isTransferEncodingChunked(response)) {
                readingChunks = true;
                System.err.println("CHUNKED CONTENT {");
            } else {
                System.err.println("CONTENT {");
            }
        }
        if (msg instanceof HttpContent) {
            HttpContent chunk = (HttpContent) msg;
            System.err.println(chunk.content().toString(CharsetUtil.UTF_8));

            if (chunk instanceof LastHttpContent) {
                if (readingChunks) {
                    System.err.println("} END OF CHUNKED CONTENT");
                } else {
                    System.err.println("} END OF CONTENT");
                }
                readingChunks = false;
            } else {
                System.err.println(chunk.content().toString(CharsetUtil.UTF_8));
            }
        }
        
        ctx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.channel().close();
    }
    
    private HttpRequest createRequest() throws IOException, InvalidKeyException {
        String url = this.url.toString();
        String host = this.url.getHost();
        
    	this.resourceUrl = "/mycontainer/" + randomString(5);

		  	HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, url);
			
		  	request.headers().set(HttpHeaderNames.HOST, host);
		  	request.headers().set("x-ms-version", "2016-05-31");

		  	final DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		  	formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		  	String dateTime = formatter.format(new Date());

		  	request.headers().set("x-ms-date", dateTime);
		  	request.headers().set("x-ms-blob-type", "BlockBlob");

		  	request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

		  	RandomAccessFile raf = new RandomAccessFile(this.file, "r");
		  	long fileLength = raf.length();

		  	MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();

		  	request.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypesMap.getContentType(this.file.getPath()));
		  	HttpUtil.setContentLength(request, fileLength);
		  	request.headers().set("Authorization", "SharedKey " + account_name + ":" + AuthorizationHeader(account_name, account_key, "PUT", dateTime, request, resourceUrl, "", ""));


        return request;
    }
    
    public String AuthorizationHeader(String storageAccountName, String storageAccountKey, String method, String now, HttpRequest request, String resourceUrl, String ifMatch, String md5) throws InvalidKeyException
    {
        String MessageSignature;

        // This is the raw representation of the message signature.
        MessageSignature = String.format("%s\n\n\n%s\n%s\n%s\n\n\n%s\n\n\n\n%s%s",
            method,
            request.headers().getAsString("Content-Length"),
            md5,
            request.headers().getAsString("Content-Type"),
            ifMatch,
            GetCanonicalizedHeaders(request),
            GetCanonicalizedResource(resourceUrl, storageAccountName));
        System.out.println(MessageSignature);
        // Create the HMACSHA256 version of the storage key.
        String AuthorizationHeader = computeHmac256(storageAccountKey, MessageSignature);
        System.out.println(AuthorizationHeader);
        
        return AuthorizationHeader;
    }

    public String GetCanonicalizedHeaders(HttpRequest request)  {
    	
    	List<String> headerNameList = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

        // Retrieve any headers starting with , 
        //   put them in a list and sort them by value.
        for (Entry<String, String> entry : request.headers()) 
        {        	
        	if (entry.getKey().toLowerCase().startsWith("x-ms-"))
            {
                headerNameList.add(entry.getKey().toLowerCase() + ":" + entry.getValue() + "\n");
            }
        }
        
        Collections.sort(headerNameList, (a, b) -> a.compareToIgnoreCase(b));
        
        for (String headerstring : headerNameList) 
        {        	
        	sb.append(headerstring);
        }  
        
        return sb.toString();

   }
    
    public String GetCanonicalizedResource(String resourceUrl, String storageAccountName)
    {
        StringBuilder builder = new StringBuilder("/");
        builder.append(storageAccountName);
        builder.append(resourceUrl);
        
        return builder.toString();

    }
    
    public synchronized String computeHmac256(String storage_key, final String value) throws InvalidKeyException {
            byte[] utf8Bytes = null;
            try {
                utf8Bytes = value.getBytes("UTF-8");

            
            if (this.hmacSha256 == null) {
                // Initializes the HMAC-SHA256 Mac and SecretKey.
                try {
                    this.hmacSha256 = Mac.getInstance("HmacSHA256");
                }
                catch (final NoSuchAlgorithmException e) {
                    throw new IllegalArgumentException();
                }
                hmacSha256.init(new SecretKeySpec(storage.netty.Base64.decode(storage_key), "HmacSHA256"));
            }
            
            return storage.netty.Base64.encode(hmacSha256.doFinal(utf8Bytes));
            
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (final UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e);
            }
            
            return null;
    }
    
    String randomString( int len ){
    	StringBuilder sb = new StringBuilder( len );
    	for( int i = 0; i < len; i++ ) 
    		sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
    	return sb.toString();
    }
    
    private void setContentTypeHeader(HttpRequest request, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
    }
}
