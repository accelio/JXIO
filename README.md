<h1>JXIO</h1>

JXIO is Java API over AccelIO (C library).</br>  
AccelIO (http://www.accelio.org/) is a high-performance asynchronous reliable messaging and RPC library optimized for hardware acceleration. RDMA and TCP/IP transport are implemented, and other transports, such as shared-memory can take advantage of efficient and convenient API.


<h2>Build instructions:</h2>
1. Download: git clone https://github.com/accelio/JXIO.git</br>
2. Move into folder: cd JXIO</br>
3. Set JAVA_HOME: export JAVA_HOME=/usr/java/jdk1.7.0_25/</br>
4. Build: ./build.sh (this pulls the relevant C level Accelio library and builds everything you need)</br>

<h2>Examples:</h2>
In examples folder there is HelloWorld example. Both client and server are single threaded. Client sends a single message
to Server and exits after receiving a response.</br>

1. Run Server side: ./examples/runHelloWorld.sh server 36.0.0.120 1234</br>
LD library is: /.autodirect/mtrswgwork/katyak/tmp/jxio/examples</br>
Compiling JAVA files....</br>
Running Server side test</br>
2014-02-16 11:17:35,013 main INFO  HelloServer:44 waiting for JXIO incoming connections</br>
2014-02-16 11:17:46,576 main INFO  HelloServer:90 [SUCCESS] Got event onSessionNew from 36.0.0.121, URI='rdma://36.0.0.120:1234/'</br>
2014-02-16 11:17:46,578 main INFO  HelloServer:108 [SUCCESS] Got a message request! Prepare the champagne!</br>
2014-02-16 11:17:46,579 main INFO  HelloServer:116 msg is: 'Hello Server'</br>
2014-02-16 11:17:46,583 main INFO  HelloServer:135 [EVENT] Got event SESSION_CLOSED</br>

2. Run Client side: ./examples/runHelloWorld.sh client 36.0.0.120 1234</br>
LD library is: /.autodirect/mtrswgwork/katyak/tmp/jxio/examples</br>
Compiling JAVA files....</br>
Running Client side test...</br>
2014-02-16 11:17:46,552 main INFO  HelloClient:68 Try to establish a new session to 'rdma://36.0.0.120:1234/'</br>
2014-02-16 11:17:46,580 main INFO  HelloClient:102 [SUCCESS] Session established! Hurray !</br>
2014-02-16 11:17:46,581 main INFO  HelloClient:106 [SUCCESS] Got a message! Bring the champagne!</br>
2014-02-16 11:17:46,582 main INFO  HelloClient:114 msg is: 'Hello to you too, Client'</br>
2014-02-16 11:17:46,582 main INFO  HelloClient:118 Closing the session...</br>
2014-02-16 11:17:46,585 main INFO  HelloClient:126 [EVENT] Got event SESSION_CLOSED</br>
2014-02-16 11:17:46,586 main INFO  HelloClient:57 Client is releasing JXIO resources and exiting</br>


