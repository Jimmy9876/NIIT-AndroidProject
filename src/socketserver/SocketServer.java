package socketserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONObject;

public class SocketServer {
	private static final int SOCKET_PORT = 7007;
	private ServerSocket serverSocket = null;
	private boolean flag = true;
	private int socketId;
	
	private ArrayList<Message> mMsgList = new ArrayList<Message>();
	private ArrayList<SocketThread> mThreadList = new ArrayList<SocketThread>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SocketServer socketServer = new SocketServer();
		socketServer.initSocket();
	}

	private void initSocket() {
		try {
			serverSocket = new ServerSocket(SOCKET_PORT);
			System.out.println("服务已经启动，端口号:" + SOCKET_PORT);
			startMessageThread();
			while (flag) {
				Socket clientSocket = serverSocket.accept();
				SocketThread socketThread = new SocketThread(clientSocket,socketId++);
				socketThread.start();
				mThreadList.add(socketThread);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public class SocketThread extends Thread {

		public Socket socket;
		public int mSocketId;
		public BufferedReader reader;
		public BufferedWriter writer;

		public SocketThread(Socket clientSocket, int socketId) {
			this.mSocketId=socketId;
			this.socket = clientSocket;
			System.out.println("新注册用户的id为："+mSocketId);
			//获取输入流
			InputStream inputStream;
			try {
				inputStream = socket.getInputStream();
				//得到读取BufferedReader对象
				reader = new BufferedReader(new InputStreamReader(inputStream,"utf-8"));
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"utf-8"));
				writer.write("用户名为："+mSocketId+"\n");
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		@Override
		public void run() {
			super.run();

			try {
				//循环读取客户端发过来的消息
				while (flag) {
					if (reader.ready()) {
						String comeData=reader.readLine();
						JSONObject msgJson = new JSONObject(comeData);
						Message msg = new Message();
						msg.setTo(msgJson.getInt("to"));
						msg.setMsg(msgJson.getString("msg"));
						msg.setFrom(mSocketId);	
						msg.setTime(getTime(System.currentTimeMillis()));
						mMsgList.add(msg);
						System.out.println("用户："+mSocketId+"向用户："+msg.getTo()+"发送的消息内容为："+msg.getMsg());
					}
					Thread.sleep(100);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
	
	public void startMessageThread() {
		new Thread(){
			@Override
			public void run() {
				super.run();
				try {
					while(flag) {
						if(mMsgList.size() > 0) {
							Message from = mMsgList.get(0);
							for(SocketThread toThread : mThreadList) {
								//遍历mThreadList如果to.socketID==from.to说明这个toThread与mMsgList中的这条内容是对应的
								//这里toThread的作用通过它得到这条消息的BufferedWriter，mMsgList.get(0)得到这条消息，然后通过
								//BufferedWriter将这条消息发送到 指定方
								if(toThread.mSocketId == from.getTo()) {
									//这里的writer是SocketThread中的writer,这样才能保证在调用writer.flush之后消息到达
									//我们的指定方
									BufferedWriter writer = toThread.writer;
									JSONObject json = new JSONObject();
									json.put("from", from.getFrom());
									json.put("msg", from.getMsg());
									json.put("time", from.getTime());
									writer.write(json.toString()+"\n");
									writer.flush();
									System.out.println("转发消息成功");
									break;
								}
							}
						mMsgList.remove(0);
						}
						Thread.sleep(200);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	private String getTime(long millTime) {
		Date d = new Date(millTime);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println(sdf.format(d));
		return sdf.format(d);
	}

}