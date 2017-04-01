package com.cloudera.fce.bootcamp;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.UUID;

public class MeasurementGenerator {

    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception {

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        Socket echoSocket = new Socket(hostName, portNumber);
        PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
        
        while (true) {
            Random random = new Random();
            
            String measurementID = UUID.randomUUID().toString();
            int detectorID = random.nextInt(8) + 1;
            int galaxyID = random.nextInt(128) + 1;
            int astrophysicistID = random.nextInt(106) + 1;
            long measurementTime = System.currentTimeMillis();
            double amplitude1 = random.nextDouble();
            double amplitude2 = random.nextDouble();
            double amplitude3 = random.nextDouble();
            
            String delimiter = ",";
            String measurement = measurementID + delimiter + detectorID + delimiter + galaxyID +
                    delimiter + astrophysicistID + delimiter + measurementTime + delimiter +
                    amplitude1 + delimiter + amplitude2 + delimiter + amplitude3;
            
            out.println(measurement);
        }
        
    }

}
