import com.mellanox.*;


public class MyEQH extends EventQueueHandler implements Runnable{

	public MyEQH(int size) {
		super(size);
	}
	
	public void run() {
		super.runEventLoop(1, 0);
		super.runEventLoop(1, 0);

	}

}
