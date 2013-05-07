
# Brooklyn MapR Roll-out

This project contains Brooklyn entities for the components of a MapR M3 system,
and a sample application which deploys it to Amazon.


### Compile

To compile brooklyn-mapr, simply `mvn clean install` in the project root.

Note: If this is a new machine run `ssh-keygen -t rsa` and `ssh-keygen -t dsa` to generate RSA and DSA keys in `~/.ssh`.
Then, copy the generated keys to `~/.ssh/authorized_keys` for the build user.

### Run

To run it, either:

* Build the mapr distribution, unpack it and run the static `main` in `io.cloudsoft.mapr.MyM3App`:

	export VERSION=0.2.0-SNAPSHOT
	mvn clean install assembly:assembly -DskipTests
	cp target/brooklyn-mapr-${VERSION}-dist.tar.gz /path/to/my/install
	cd /path/to/my/install
	tar xzf brooklyn-mapr-${VERSION}-dist.tar.gz
	cd brooklyn-mapr-${VERSION}
	./start.sh --location aws-ec2:us-east-1

* Or download and install the `brooklyn` CLI tool from [brooklyncentral.github.com](http://brooklyncentral.github.com/) and run in the project root.

        export VERSION=0.2.0-SNAPSHOT
	export BROOKLYN_CLASSPATH=target/brooklyn-mapr-${VERSION}.jar:~/.m2/repository/mysql/mysql-connector-java/5.1.6/mysql-connector-java-5.1.6.jar
	brooklyn launch -a io.cloudsoft.mapr.MyM3App -l aws-ec2:us-east-1


After about 20 minutes, it should print out the URL of the MapR master node and the Brooklyn console.  
You must manually accept the license in MapR (credentials defined in MyM3App).

Once fully booted, you can resize (scale out) the worker cluster, stop nodes, and see a few sensors.
As an exercise to the reader, add new sensors with the metrics you care about, and perhaps add a
policy to automatically scale out.  (See other Brooklyn examples for an illustration.)


### Setup

In both cases you'll need AWS credentials in `~/.brooklyn/brooklyn.properties`.

	brooklyn.jclouds.aws-ec2.identity=AKXXXXXXXXXXXXXXXXXX
	brooklyn.jclouds.aws-ec2.credential=secret01xxxxxxxxxxxxxxxxxxxxxxxxxxx

Most other clouds should work too, with minor variations to the code (in particular the disk setup in MyM3App),
as will fixed IP machines (bare-metal/byon).  MaaS clouds (metal-as-a-service) are in development, over at jclouds.org.


### Finally

This software is (c) 2013 Cloudsoft Corporation, released as open source under the Apache License v2.0.

Any questions drop a line to [brooklyn-users@googlegroups.com](http://groups.google.com/group/brooklyn-users‎).
