package com.sjsu.chandylamport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.lang.Object;

/**
 * Performs all the processor related tasks
 *
 * @author Sample
 * @version 1.0
 */


public class Processor implements Observer {
	private int messageCount = 0;

    List<Buffer> inChannels = new ArrayList<>();

    /**
     * List of output channels
     * //TODO: Homework: Use appropriate list implementation and replace null assignment with that
     *
     */
    List<Buffer> outChannels = new ArrayList<>();

    /**
     * This is a map that will record the state of each incoming channel and all the messages
     * that have been received by this channel since the arrival of marker and receipt of duplicate marker
     * //TODO: Homework: Use appropriate Map implementation.
     */
    Map<Buffer, List<Message>> channelState = new HashMap <Buffer, List<Message>>();

    /**
     * This map can be used to keep track of markers received on a channel. When a marker arrives at a channel
     * put it in this map. If a marker arrives again then this map will have an entry already present from before.
     * Before  doing a put in this map first do a get and check if it is not null. ( to find out if an entry exists
     * or not). If the entry does not exist then do a put. If an entry already exists then increment the integer value
     * and do a put again.
     */
    Map<Buffer, Integer> channelMarkerCount = new HashMap <Buffer, Integer>();
    Map<Buffer, RecorderThread> channelRecorder = new HashMap<Buffer,RecorderThread>();
    
    public int getMarkerCount(Buffer i) {
    	return channelMarkerCount.get(i);
    }
    /**
     * @param id of the processor
     */

  private int id;
    
    public int getMessageCount() {
    		return this.messageCount;
    }
    
    
    public Processor(int id, List<Buffer> inChannels, List<Buffer> outChannels) {
        this.inChannels = inChannels;
        this.outChannels = outChannels;
        //TODO: Homework make this processor as the observer for each of its inChannel
        //Hint [loop through each channel and add Observer (this) . Feel free to use java8 style streams if it makes
        // it look cleaner]
        for(Buffer i : inChannels)
        { i.addObserver(this);}
        for(Buffer channel : inChannels){
        	channelMarkerCount.put(channel, 0);
        }
    }


    /**
     * This is a dummy implementation which will record current state
     * of this processor
     */
    public void recordMyCurrentState() {
        System.out.println("Recording my registers...");
        System.out.println("Recording my program counters...");
        System.out.println("Recording my local variables...");
    }

    /**
     * This method marks the channel as empty
     * @param channel
     */
    public void recordChannelAsEmpty(Buffer channel) {

        channelState.put(channel, Collections.emptyList());

    }

    /**
     * You should send a message to this recording so that recording is stopped
     * //TODO: Homework: Move this method recordChannel(..) out of this class. Start this method in a
     *                  separate thread. This thread will start when the marker arrives and it will stop
     *                  when a duplicate marker is received. This means that each processor will have the
     *                  capability to start and stop this channel recorder thread. The processor manages the
     *                  record Channel thread. Processor will have the ability to stop the thread.
     *                  Feel free to use any java concurrency  and thread classes to implement this method
     *
     *
     * @param channel The input channel which has to be monitored
     */
	public void recordChannel(Buffer channel) {
		RecorderThread recorder = channelRecorder.get(channel);
		//start recording on channel
		recorder.start();
		synchronized(recorder) {
			recorder.notify();
		}
	}


    /**
     * Overloaded method, called with single argument
     * This method will add a message to this processors buffer.
     * Other processors will invoke this method to send a message to this Processor
     *
     * @param message Message to be sent
     */
    public void sendMessgeTo(Message message, Buffer channel) {
        channel.saveMessage(message);

    }

    /**
     *
     * @param fromChannel channel where marker has arrived
     * @return true if this is the first marker false otherwise
     */
    public boolean isFirstMarker(Buffer fromChannel) {
        //TODO: Implement this method
        //[ Hint : Use the channelMarkerCount]
    	boolean bool = false;
    	if (getMarkerCount(fromChannel) == 1)
    	{
    		//means first marker message
    		System.out.println("First Marker from " + fromChannel.getLabel());
    		bool = true;
    	}
    	else if(getMarkerCount(fromChannel) > 1)
    	{
    		System.out.println("Duplicate Marker from " + fromChannel.getLabel());
    		 bool = false;
    	}
		return bool;    	
    }
    
    

    /**
     * Gets called when a Processor receives a message in its buffer
     * Processes the message received in the buffer
     */
    public void update(Observable observable, Object arg) {
    	
		// Thread t= new Thread();
    	Buffer fromChannel = (Buffer) observable;
        Message message = (Message) arg;
        Processor fromProcessor = message.getFrom();
//        Buffer fromChannel - fromProcessor.
        if (message.getMessageType().equals(MessageType.MARKER)) {
//            //TODO: homework Record from Channel as Empty
//            if (message.getMessageType().equals(MessageType.MARKER)) {
//                recordChannelAsEmpty(fromChannel);
//            //TODO: add logic here so that if the marker comes back to the initiator then it should stop recording
//                if(channelMarkerCount.get(fromChannel)>=1) {
//            		stopChannel(fromChannel);
//            }
            for(Buffer c : inChannels) {
            	if(c!= fromChannel) {
            		recordChannel(c)  ;
            	}
            }
            if (isFirstMarker(fromChannel)) {
            recordChannelAsEmpty(fromChannel);
            channelMarkerCount.put(fromChannel, channelMarkerCount.get(fromChannel) + 1);

			 RecorderThread recorderThread = new RecorderThread(this,fromChannel);
			 channelRecorder.put(fromChannel, recorderThread);
			 recorderThread.run();
        		
            }else  {
    			for (RecorderThread recorderThread: channelRecorder.values()){
        			if(recorderThread.inChannel==fromChannel){
        				recorderThread.interrupt();
        			}
    			}
        	}
                
            
            //TODO: Homework Send marker messages to each of the out channels
            // Hint: invoke  sendMessgeTo((Message) arg, outChannel) for each of the out channels
                
                for(Buffer c : this.outChannels) {
	        		Message m = new Message(MessageType.MARKER);
	        		sendMessgeTo(m, c);
	        }
        }
        else{
            if (message.getMessageType().equals(MessageType.ALGORITHM)) 
            {
                System.out.println("Processing Algorithm message....");
                }

        }
//        }
//        else{
//            if (message.getMessageType().equals(MessageType.ALGORITHM)) {
//                System.out.println("Processing Algorithm message....");
//            }  //There is no other type
//        }


//    }
                
        }


//    private void stopChannel(Buffer fromChannel) {
//		// TODO Auto-generated method stub
//		
//	}

	public void initiateSnapShot() {
        recordMyCurrentState();
        //TODO: Follow steps from Chandy Lamport algorithm. Send out a marker message on outgoing channel
        //[Hint: Use the sendMessgeTo method
        for(Buffer outchannel : outChannels)
        {
            Message m = new Message(MessageType.MARKER);
            //channelMarkerCount.put(outchannel, 0);
            sendMessgeTo(m,outchannel);
        }

        //TODO: homework Start recording on each of the input channels

        for(Buffer inchannel : inChannels)
        {
        	//channelMarkerCount.put(inchannel, 0);
            RecorderThread recorderThread = new RecorderThread(this,inchannel);
            channelRecorder.put(inchannel, recorderThread);
            recorderThread.run();
        }
    }
}
