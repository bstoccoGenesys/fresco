build: install move

install:
	cd ../../core && mvn clean install -DskipTests
	cd ../../suite/spdz && mvn clean install -DskipTests
	cd ../common && mvn clean install -DskipTests
	mvn clean install -DskipTests

move:
	mkdir -p server1;
	mkdir -p server2;
	mkdir -p server3;
	mkdir -p server4;
	cp target/fresco-demo-sum.jar server1;
	cp target/fresco-demo-sum.jar server2;
	cp target/fresco-demo-sum.jar server3;
	cp target/fresco-demo-sum.jar server4;

runSPDZ:
	cd server1 && java -jar fresco-demo-sum.jar 1 3 > log.txt 2>&1 &
	cd server2 && java -jar fresco-demo-sum.jar 2 58 > log.txt 2>&1 &
	cd server3 && java -jar fresco-demo-sum.jar 3 21 > log.txt 2>&1 &
	cd server4 && java -jar fresco-demo-sum.jar 4 11 2>&1 | tee log.txt

runDummy:
	cd server1 && java -jar fresco-demo-sum.jar -e SEQUENTIAL_BATCHED -i 1 -l INFO -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -s dummyarithmetic > log.txt 2>&1 &
	cd server2 && java -jar fresco-demo-sum.jar -e SEQUENTIAL_BATCHED -i 2 -l INFO -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -s dummyarithmetic > log.txt 2>&1 &
	cd server3 && java -jar fresco-demo-sum.jar -e SEQUENTIAL_BATCHED -i 3 -l INFO -p 1:localhost:8081 -p 2:localhost:8082 -p 3:localhost:8083 -s dummyarithmetic 2>&1 | tee log.txt
