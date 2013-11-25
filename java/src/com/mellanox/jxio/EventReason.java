package com.mellanox.jxio;

public enum EventReason {
	SUCCESS(0), NOT_SUPPORTED(1), NO_BUFS(2),
	CONNECT_ERROR(3), ROUTE_ERROR(4), ADDR_ERROR(5),
	UNREACHABLE(6), MSG_SIZE(7), PARTIAL_MSG(8),
	MSG_INVALID(9), MSG_UNKNOWN(10), 
	SESSION_REFUSED(11), SESSION_ABORTED(12), SESSION_DISCONNECTED(13),
	BIND_FAILED(14), TIMEOUT(15), IN_PROGRESS(16), INVALID_VERSION(17), NOT_SESSION(18),
	OPEN_FAILED(19), READ_FAILED(20),WRITE_FAILED(21),CLOSE_FAILED(22), 
	UNSUCCESSFUL(23), MSG_CANCELED(24), MSG_CANCEL_FAILED(25), MSG_NOT_FOUND(26);
	
	private int index;
	
	private EventReason(int i) {
		   index = i;
	}
	public int getIndex(){return index;}
	
	private static EventReason [] allReasons = values();
	public static EventReason getEventByIndex(int index){
		return allReasons[index];
	}
}
