# SimpleConnectionTest

Simple test with SDK 1.4.9

This was developed on Java 1.8, with Couchbase SDK Java client version 1.4.9 from Maven, and run against Couchbase Server 3.0.1 and 3.0.3.

During the running of this program you can try various ways of breaking the network connection and see it recover and experiment with different settings. 

Sample output

    ################### Top of main loop - 2015/07/21 13:27:54 (document29750) ###################
    About to call set()
    About to call get()
    Exceptions during Set: 11 Get: 11                 # times get() was null: 0  docs match: 37  no match: 0
    Time since last set() exception:17615
    Time since last get() exception:17114
    Time since last Google exception:36016 Total seen: 2
    NetworkCheckThread: Google says 200 after about 61 ms
    ################### Bottom of main loop ###################
