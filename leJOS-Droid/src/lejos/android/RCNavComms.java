package lejos.android;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import lejos.pc.comm.*;

/**
 * Provides  Bluetooth communications services to RCNavitationControl:<br>
 * 1. connect to NXT
 * 2. send commands  using the Command emum
 * 3. receives robot position
 * @author Roger
 */
public class RCNavComms
{

  /**
   * constructor establishes  call back path of the RCNavigationControl
   * @param control
   */
  public RCNavComms(RCNavigationControl control)
  {
    this.control = control;
    System.out.println(" RCNavComms start");
  }

  /**
   * connects to NXT using Bluetooth
   * @param name of NXT
   * @param address  bluetooth address
   */
  public boolean connect(String name, String address)
  {
    System.out.println(" connecting to " + name + " " + address);
    connector = new NXTConnector();
    boolean connected = connector.connectTo(name, address, NXTCommFactory.BLUETOOTH);
    System.out.println(" connect result " + connected);
    if (!connected)
    {
      return connected;
    }
    dataIn = connector.getDataIn();
    dataOut = connector.getDataOut();
    if (dataIn == null)
    {
      connected = false;
      return connected;
    }
    if (!reader.isRunning)
    {
      reader.start();
    }
    return connected;
  }

  /**
   * inner class to monitor for an incoming message after a command has been sent <br>
   * calls showRobotPosition() on the controller
   */
  class Reader extends Thread
  {

    public boolean reading = false;
    int count = 0;
    boolean isRunning = false;
    public void run()
    {
      isRunning = true;
      while (isRunning)
      {
        if (reading)  //reads one message at a time
        {
          System.out.println("reading ");
          float x = 0;
          float y = 0;
          float h = 0;
          boolean ok = false;
          try
          {
            x = dataIn.readFloat();
            y = dataIn.readFloat();
            h = dataIn.readFloat();
            ok = true;
            System.out.println("data  " + x + " " + y + " " + h);
          } catch (IOException e)
          {
            System.out.println("connection lost");
            count++;
            isRunning = count < 20;// give up
            ok = false;
          }
          if (ok)
          {
            control.showtRobotPosition(x, y, h);
            reading = false;
          }
          try
          {
            Thread.sleep(50);
          } catch (InterruptedException ex)
          {
            Logger.getLogger(RCNavComms.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
      }// if reading
      Thread.yield();
    }//while is running
  }
/**
 * sends a command with a variable number of float parameters.
 *  see http://java.sun.com/docs/books/tutorial/java/javaOO/arguments.html
 * the section on Arbitrary Number of Arguments
 * @param c a Command enum
 * @param data  an array of floats built from the collection list parameters.
 */
  public void send(Command c, float... data)
  {
    while(reader.reading)
    {
      Thread.yield();
    }
    try
    {
      dataOut.writeInt(c.ordinal());  // convert the enum to an integer
      for (float d : data)  // iterate over the   data   array
      {
        dataOut.writeFloat(d);
      }
      dataOut.flush();
    } catch (IOException e)
    {
      System.out.println(" send throws exception  " + e);
    }
    reader.reading = true;  //reader: listen for response
  }
  /**
   * used by reader
   */
  private DataInputStream dataIn;
  /**
   * used by send()
   */
  private DataOutputStream dataOut;
  private Reader reader = new Reader();
  private NXTConnector connector;
  public NXTConnector getConnector() {
	return connector;
}
private RCNavigationControl control;
  
  
  
}
