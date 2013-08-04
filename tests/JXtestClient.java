
public class JXtestClient {

	static private final int NUM_OF_MSG = 10000 ; // 
	static private final int SIZE_OF_MSG = 64 ; // size in Bytes

	
	int main(String[] args){
		
		String url = args[0];
		String port = args[1];
		JXMsgPool pool = new JXMsgPool(NUM_OF_MSG, SIZE_OF_MSG);
		ClientJXSession s = new ClientJXSession(pool);
		// starts a client session
		s.startSession(url, Integer.parseInt(port));
		return 1;
	}
}
