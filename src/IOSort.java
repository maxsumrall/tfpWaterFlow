import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;

/**
 * Created by max on 3/13/14.
 */
public class IOSort {
    /**
     * At the end of this function, the file has the smaller subsections sorted, still in the same file.
     * @param edgesFile
     */
    MappedByteBuffer edgesBuffer;
    FileChannel edgesFileChannel;
    RandomAccessFile RAFile;
    File edgesFile;
    int smallestSubsetSize = 8; //how many edges to sort in-memory. NUMBER OF EDGES!
    long EDGES_IN_FILE;
    long BYTES_IN_FILE;
    int N;      //the first number in the input file of edges
    long edgesBufferSize;
    IOSort(File edgesFile){
        //Open the file containing the edges for reading only.
        this.edgesFile = edgesFile;
        ByteBuffer tempBuffer = ByteBuffer.allocate(8);
        try{
            this.RAFile = new RandomAccessFile(edgesFile,"rw");
            this.edgesFileChannel = this.RAFile.getChannel();
            this.BYTES_IN_FILE = this.edgesFileChannel.size();
            this.EDGES_IN_FILE = this.edgesFileChannel.size()/8;//divide by 8 because thats how many bytes there are per edge, and thats what I want to know
            edgesFileChannel.read(tempBuffer);  //read the first number from the file
            tempBuffer.flip();
            N = tempBuffer.getInt() + tempBuffer.getInt();   //remember the padding which makes the first one 8 bytes.
            edgesBufferSize =  (N*3*2*4);
            this.edgesBuffer = this.edgesFileChannel.map(FileChannel.MapMode.READ_WRITE, 8, edgesBufferSize); //Prepared to handle huge number of edges without consuming heap space
        }
        catch (IOException e){e.printStackTrace();}

    }
    public void sortSegments(){

        ArrayList<IOEdge> temp = new ArrayList<IOEdge>();
        int count = edgesBuffer.remaining();
        int bytesPerSubset = smallestSubsetSize*8;//an edge is two ints, which are 4 bytes
        for (int i = 0; i < count; i+=bytesPerSubset){//for each subset...
            edgesBuffer.mark();
            for(int j = 0; (j < smallestSubsetSize) && (edgesBuffer.remaining() > 8); j++){
                temp.add(new IOEdge(edgesBuffer.getInt(),edgesBuffer.getInt()));
            }
            edgesBuffer.reset();
            Collections.sort(temp);
            System.out.println(temp);
            for (IOEdge e: temp){
                edgesBuffer.putInt(e.getID());
                edgesBuffer.putInt(e.getTo());
            }
         temp.clear();
        }
       edgesBuffer.force();
    }



    public void printData(int n) throws IOException{
        RandomAccessFile in = new RandomAccessFile("edgeData" + n + ".dat","r");
        FileChannel fc = in.getChannel();
        int i = 0;
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY,0,fc.size());
        while(mbb.hasRemaining()){

            System.out.println(i++ + ": " + mbb.getInt() + ", " + mbb.getInt());
        }
        System.out.println("-----------");
    }

    /**
     * We have two buffers which are actual files but we can treat them like buffers in memory since the OS handles paging
     *
     * EDGES = buffer holding the edges in various states of unorderedness
     * TEMP = buffer where merges go
     *
     * Edges are group into subsets. Initialy I set this as 8. Every set of 8 edges in the EDGES buffer are brought into memory and sorted naively.
     * Then, we do mergesort starting with these subsets.
     *
     * P and Q represent the current active point within a subset while merging. Initially they refer to the first element in their respective subsets
     *
     * You compare the edge at P and the edge at Q. Take the smaller one and put it in TEMP (along with the second element, since an edge is 2 Ints but we compare only the first)
     * Whichever one (P or Q) was smaller and was put in TEMP, increment P or Q so that the next comparison works.
     * Continue until either P or Q reaches the end of its subsection, and put the remaining elements of the other subsection into TEMP
     * Increment both P and Q to the next subsections.
     * Repeat until all subsections have been merged into TEMP
     * Swap EDGES and TEMP, double the size of the subsections, and repeat until the size of the subsections is the size of the full buffer.
     * Make sure you get EDGES and TEMP swapped back or something.
     */
    public void mergeSort() throws IOException{

        int subsetSize = smallestSubsetSize*8;  //bytes
        int currentIndex = 0;     //bytes?  the 8 means I skip the first guy
        FileChannel P;//P and Q are location indices into the two sets I will merge[sort]
        FileChannel Q;
        MappedByteBuffer PBuffer;
        MappedByteBuffer QBuffer;
        RandomAccessFile rTempFile;
        RandomAccessFile rTempFileP;
        RandomAccessFile rTempFileQ;
        boolean flip = true;
        //make two maps over the file channel, one for each subset which will be merged.
        // File = [<------P------>|<------Q------>|....................]

        //Make an intermediary file/buffer to move the merged copies to.
        MappedByteBuffer temp;
        File tempFile = new File("temp.dat");
        FileChannel tempFileChannel;
       try{
            rTempFile = new RandomAccessFile(tempFile,"rw");
            rTempFileP = new RandomAccessFile(this.edgesFile,"rw");
            rTempFileQ = new RandomAccessFile(this.edgesFile,"rw");
            tempFileChannel = rTempFile.getChannel();
           temp = tempFileChannel.map(FileChannel.MapMode.READ_WRITE,0,this.edgesFileChannel.size());  //size of the original buffer
           P = rTempFileP.getChannel();
            Q = rTempFileQ.getChannel();
       }
        catch(IOException e){e.printStackTrace();return;}

        while(subsetSize < BYTES_IN_FILE){//continue until the current size of groups to merge is the size of the whole file
            //System.out.println("subsetSize: " + subsetSize + ", BytesinFile: " + BYTES_IN_FILE);
            //temp = tempFileChannel.map(FileChannel.MapMode.READ_WRITE,0,this.edgesFileChannel.size());  //size of the original buffer
            temp.position(0);
            printData(40);
            while(currentIndex + 2*subsetSize < (this.BYTES_IN_FILE)){
                //System.out.println("P: "+currentIndex +", " + (currentIndex + subsetSize));
                //System.out.println("Q: "+(currentIndex + subsetSize) +", " + (currentIndex + 2*subsetSize));
                rTempFileP = new RandomAccessFile(this.edgesFile,"rw");
                rTempFileQ = new RandomAccessFile(this.edgesFile,"rw");
                P = rTempFileP.getChannel();
                Q = rTempFileQ.getChannel();
                PBuffer = P.map(FileChannel.MapMode.READ_WRITE,currentIndex,subsetSize);
                QBuffer = Q.map(FileChannel.MapMode.READ_WRITE,currentIndex+subsetSize+8, subsetSize);
                int x = PBuffer.getInt();
                int y = QBuffer.getInt();

                while(PBuffer.hasRemaining() && QBuffer.hasRemaining()){
                    //System.out.println(x+ " <--x, y-->"+y);

                    if (x < y){
                        temp.putInt(x);
                        temp.putInt(PBuffer.getInt());//put both the values for the edge
                        if(PBuffer.hasRemaining()){x = PBuffer.getInt();}
                        else{temp.putInt(y);}
                    }
                    else{
                        temp.putInt(y);
                        temp.putInt(QBuffer.getInt());
                        if (QBuffer.hasRemaining()){y = QBuffer.getInt();}
                        else{temp.putInt(x);}
                    }
                }
                //Either P or Q has been emptied, so the other one with remaining elements should be dumped into B
                while (PBuffer.hasRemaining()){temp.putInt(PBuffer.getInt());}
                while(QBuffer.hasRemaining()){temp.putInt(QBuffer.getInt());}
                PBuffer.force();
                QBuffer.force();
                currentIndex += 2*subsetSize;

            }

            currentIndex = 0;
            subsetSize = subsetSize * 2; //merge done, repeat for larger subset sizes until all merges are done.
            //Everything from P and Q were merged into temp, now I need to setup these files to merge again
            temp.force();
            temp.position(0);
            this.edgesFileChannel.position(0);
            this.edgesFileChannel.transferFrom(tempFileChannel, 0, tempFileChannel.size());
            this.edgesFileChannel.force(true);
            temp.position(0);

            /*if(flip){
                flip = false;
                rTempFile = new RandomAccessFile(this.edgesFile,"rw");
                rTempFileP = new RandomAccessFile(tempFile,"rw");
                rTempFileQ = new RandomAccessFile(tempFile,"rw");
                tempFileChannel = rTempFile.getChannel();
                P = rTempFileP.getChannel();
                Q = rTempFileQ.getChannel();
            }
            else{
                flip = true;
                rTempFile = new RandomAccessFile(tempFile,"rw");
                rTempFileP = new RandomAccessFile(this.edgesFile,"rw");
                rTempFileQ = new RandomAccessFile(this.edgesFile,"rw");
                tempFileChannel = rTempFile.getChannel();
                P = rTempFileP.getChannel();
                Q = rTempFileQ.getChannel(); */
             
            rTempFile.seek(0);
            rTempFileP.seek(0);
            rTempFileQ.seek(0);
            P.position(0);
            Q.position(0);
            tempFileChannel.position(0);

        }
    }
}

