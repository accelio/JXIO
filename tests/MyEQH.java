import com.mellanox.*;


public class MyEQH extends EventQueueHandler implements Runnable{

	public MyEQH(int size) {
		super(size);
	}
	
	public void run() {
		while (!this.stopLoop) {
			System.out.println("EventQueueHandler: before run event loop");
			super.runEventLoop(1, 0);
		}
	}

}
