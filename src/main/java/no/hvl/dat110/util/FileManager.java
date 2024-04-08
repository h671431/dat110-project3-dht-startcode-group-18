package no.hvl.dat110.util;


/**
 * @author tdoy
 * dat110 - project 3
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import no.hvl.dat110.middleware.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import no.hvl.dat110.middleware.Message;
import no.hvl.dat110.rpc.interfaces.NodeInterface;

import static no.hvl.dat110.util.Hash.hashOf;

public class FileManager {

    private static final Logger logger = LogManager.getLogger(FileManager.class);

    private BigInteger[] replicafiles;                            // array stores replicated files for distribution to matching nodes
    private int numReplicas;                                    // let's assume each node manages nfiles (5 for now) - can be changed from the constructor
    private NodeInterface chordnode;
    private String filepath;                                    // absolute filepath
    private String filename;                                    // only filename without path and extension
    private BigInteger hash;
    private byte[] bytesOfFile;
    private String sizeOfByte;

    private Set<Message> activeNodesforFile = null;

    public FileManager(NodeInterface chordnode) throws RemoteException {
        this.chordnode = chordnode;
    }

    public FileManager(NodeInterface chordnode, int N) throws RemoteException {
        this.numReplicas = N;
        replicafiles = new BigInteger[N];
        this.chordnode = chordnode;
    }

    public FileManager(NodeInterface chordnode, String filepath, int N) throws RemoteException {
        this.filepath = filepath;
        this.numReplicas = N;
        replicafiles = new BigInteger[N];
        this.chordnode = chordnode;
    }

    public void createReplicaFiles() {
        // set a loop where size = numReplicas
       /* for (int i = 0; i < numReplicas; i++) {
            // replicate by adding the index to filename
            String replicaFilename = filename + i;
            try {
                // hash the replica
                BigInteger hashReplica = Hash.hashOf(replicaFilename);
                // store the hash in the replicafiles array
                replicafiles[i] = hashReplica;
            } catch (RuntimeException e) {
                logger.error("Error hashing replica: " + e.getMessage());
            }
        }*/
        try {
            for(int i=0; i<numReplicas; i++) {
                String replica = filename + i;
                replicafiles[i] = hashOf(replica);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param bytesOfFile
     * @throws RemoteException
     */
    public int distributeReplicastoPeers() throws RemoteException {
        Random rnd = new Random();
        int index = rnd.nextInt(Util.numReplicas-1);
        int counter = 0;

        createReplicaFiles();
        // iterate over the replicas
        for (int i = 0; i < replicafiles.length; i++) {
            // for each replica, find its successor (peer/node) by performing findSuccessor(replica)
            NodeInterface successor = chordnode.findSuccessor(replicafiles[i]);
            // call the addKey on the successor and add the replica
            successor.addKey(replicafiles[i]); // Assuming addKey() method is used to add the replica
            boolean isPrimary = (counter == index);
            if(i == index) {
                successor.saveFileContent(filename, replicafiles[i], bytesOfFile, true);
            } else {
                successor.saveFileContent(filename, replicafiles[i], bytesOfFile, false);
            }
            counter++;
        }
        return counter;
    }

    /**
     * @param filename
     * @return list of active nodes having the replicas of this file
     * @throws RemoteException
     */
    public Set<Message> requestActiveNodesForFile(String filename) throws RemoteException {

        this.filename = filename;
        activeNodesforFile = new HashSet<Message>();

        // Task: Given a filename, find all the peers that hold a copy of this file

        // generate the N replicas from the filename by calling createReplicaFiles()
        createReplicaFiles();
        // iterate over the replicas of the file
        for(int i= 0; i < replicafiles.length; i++) {
            BigInteger replica = replicafiles[i];
            NodeInterface successor = chordnode.findSuccessor(replica);
            Message metadata = successor.getFilesMetadata(replica);
            activeNodesforFile.add(metadata);
        }
        return activeNodesforFile;
        // for each replica, do findSuccessor(replica) that returns successor s.

        // get the metadata (Message) of the replica from the successor (i.e., active peer) of the file

        // save the metadata in the set activeNodesforFile
    }

    /**
     * Find the primary server - Remote-Write Protocol
     *
     * @return
     */
    public NodeInterface findPrimaryOfItem() {

        // Task: Given all the active peers of a file (activeNodesforFile()),
        // find which is holding the primary copy
        for(Message message : activeNodesforFile) {
            if (message.isPrimaryServer()) {
                return Util.getProcessStub(message.getNodeName(), message.getPort());
            }
        }
        return null;
    }

    /**
     * Read the content of a file and return the bytes
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public void readFile() throws IOException, NoSuchAlgorithmException {

        File f = new File(filepath);

        byte[] bytesOfFile = new byte[(int) f.length()];

        FileInputStream fis = new FileInputStream(f);

        fis.read(bytesOfFile);
        fis.close();

        //set the values
        filename = f.getName().replace(".txt", "");
        hash = hashOf(filename);
        this.bytesOfFile = bytesOfFile;
        double size = (double) bytesOfFile.length / 1000;
        NumberFormat nf = new DecimalFormat();
        nf.setMaximumFractionDigits(3);
        sizeOfByte = nf.format(size);

        logger.info("filename=" + filename + " size=" + sizeOfByte);

    }

    public void printActivePeers() {

        activeNodesforFile.forEach(m -> {
            String peer = m.getNodeName();
            String id = m.getNodeID().toString();
            String name = m.getNameOfFile();
            String hash = m.getHashOfFile().toString();
            int size = m.getBytesOfFile().length;

            logger.info(peer + ": ID = " + id + " | filename = " + name + " | HashOfFile = " + hash + " | size =" + size);

        });
    }

    /**
     * @return the numReplicas
     */
    public int getNumReplicas() {
        return numReplicas;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return the hash
     */
    public BigInteger getHash() {
        return hash;
    }

    /**
     * @param hash the hash to set
     */
    public void setHash(BigInteger hash) {
        this.hash = hash;
    }

    /**
     * @return the bytesOfFile
     */
    public byte[] getBytesOfFile() {
        return bytesOfFile;
    }

    /**
     * @param bytesOfFile the bytesOfFile to set
     */
    public void setBytesOfFile(byte[] bytesOfFile) {
        this.bytesOfFile = bytesOfFile;
    }

    /**
     * @return the size
     */
    public String getSizeOfByte() {
        return sizeOfByte;
    }

    /**
     * @param size the size to set
     */
    public void setSizeOfByte(String sizeOfByte) {
        this.sizeOfByte = sizeOfByte;
    }

    /**
     * @return the chordnode
     */
    public NodeInterface getChordnode() {
        return chordnode;
    }

    /**
     * @return the activeNodesforFile
     */
    public Set<Message> getActiveNodesforFile() {
        return activeNodesforFile;
    }

    /**
     * @return the replicafiles
     */
    public BigInteger[] getReplicafiles() {
        return replicafiles;
    }

    /**
     * @param filepath the filepath to set
     */
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
}
