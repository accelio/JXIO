package com.mellanox.jxio.tests.random.storyrunner;

import java.io.File;

public interface StoryRunner extends Runnable{
	
	public void read(File storyFile);
	
}
