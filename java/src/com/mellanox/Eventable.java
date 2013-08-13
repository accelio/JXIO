package com.mellanox;

import java.nio.ByteBuffer;

public interface Eventable {
	public void onEvent (int eventType, ByteBuffer buffer);

}
