package dk.alexandra.fresco.demo;

import dk.alexandra.fresco.demo.cli.CmdLineUtil;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.suite.ProtocolSuite;
import java.io.IOException;
import java.math.BigInteger;

public class InputSumExample {

  int value;
  int numParties;
	
  public InputSumExample(int value, int numParties){
	  this.value = value;
	  this.numParties = numParties;
  }
  
  /**
   * Run the InputSumExample application.
   * @param sce The SCE to use
   * @param resourcePool The ResourcePool to use  
   * @param network The network to use
   */
  public <ResourcePoolT extends ResourcePool> void runApplication(
      SecureComputationEngine<ResourcePoolT, ProtocolBuilderNumeric> sce,
      ResourcePoolT resourcePool, Network network) {
    InputApplication inputApp;

    int myId = resourcePool.getMyId();
    inputApp = new InputApplication(value, myId, numParties);
    
    SumAndOutputApplication app = new SumAndOutputApplication(inputApp);

    BigInteger result = sce.runApplication(app, resourcePool, network);

    System.out.println("Result was: " + result);
  }

  /**
   * Main method for InputSumExample.
   * @param args arguments for the demo
   * @throws IOException if the network fails
   */
  public static <ResourcePoolT extends ResourcePool> void main(String[] args) throws IOException {
    CmdLineUtil<ResourcePoolT, ProtocolBuilderNumeric> util = new CmdLineUtil<>();

    String id = args[0];
    Integer total = Integer.parseInt(args[1]);
    String[] staticArgs = new String[] {"-e", "SEQUENTIAL_BATCHED", "-i", id, "-l", "INFO", "-p", "1:localhost:8081", "-p", "2:localhost:8082", "-p", "3:localhost:8083", "-p", "4:localhost:8080", "-s", "spdz", "-Dspdz.preprocessingStrategy=DUMMY"};
    
    util.parse(staticArgs);

    ProtocolSuite<ResourcePoolT, ProtocolBuilderNumeric> psConf = util.getProtocolSuite();

    SecureComputationEngine<ResourcePoolT, ProtocolBuilderNumeric> sce =
        new SecureComputationEngineImpl<>(psConf, util.getEvaluator());

    ResourcePoolT resourcePool = util.getResourcePool();
    new InputSumExample(total, 4).runApplication(sce, resourcePool, util.getNetwork());
    
    util.closeNetwork();
    sce.shutdownSCE();
  }

}
