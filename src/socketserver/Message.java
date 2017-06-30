package socketserver;

import socketserver.SocketServer.SocketThread;

public class Message {
	
	private  int to;
	private int from;
	private String msg;
	private String time;
	private SocketThread thread;
	
	public int getTo() {
		return to;
	}
	public void setTo(int to) {
		this.to = to;
	}
	public int getFrom() {
		return from;
	}
	public void setFrom(int from) {
		this.from = from;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public SocketThread getThread() {
		return thread;
	}
	public void setThread(SocketThread thread) {
		this.thread = thread;
	}
	
}