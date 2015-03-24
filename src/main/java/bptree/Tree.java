package bptree;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by max on 2/10/15.
 */
public class Tree implements Closeable, Serializable, ObjectInputValidation {

    protected static String DEFAULT_TREE_FILE_NAME = "tree.bin";
    protected String tree_filename;
    private long nextAvailablePageID = 0l;
    protected long rootNodePageID;
    private DiskCache diskCache;
    /**
     * Constructs a new Tree object
     * @param file The file where the tree should be based.
     * @throws IOException
     */
    private Tree(String tree_filename, DiskCache diskCache) throws IOException {
        this.diskCache =  diskCache;
        this.tree_filename = tree_filename;
        Node rootNode = createLeafNode();
        rootNodePageID = rootNode.id;
    }

    public static Tree initializeNewTree() throws IOException {
        return initializeNewTree(DEFAULT_TREE_FILE_NAME, DiskCache.defaultDiskCache()); //Delete on exit
    }

    public static Tree initializeNewTree(String tree_filename, DiskCache diskCache) throws IOException {
        return new Tree(tree_filename, diskCache);
    }

    public static Tree loadTreeFromFile(String tree_location) throws IOException {
        FileInputStream fis = new FileInputStream(tree_location);
        byte[] bytes = new byte[fis.available()];
        fis.read(bytes);
        Tree tree;
        try {
            tree = deserialize(bytes);
        }
        catch (InvalidClassException e){
            throw new InvalidClassException("Invalid object found at file: " + tree_location);
        }
        return tree;
    }


    /**
     * Gets a node.
     * @param id of the node, also representing it's page id.
     * @return a reference to this node.
     */
    public Node getNode(long id) throws IOException {
        if(id < 0){throw new IOException("Invalid Node ID");}
        Node node;
        ByteBuffer buffer = this.diskCache.readPage(id);
        if(buffer.capacity() == 0){
            throw new IOException("Unable to read page from cache. Page: " + id);
        }
        if(NodeHeader.isLeafNode(buffer)){
            node = new LeafNode(buffer, this, id);
        }
        else{
            node = new InternalNode(buffer, this, id);
        }
        return node;
    }

    public long getNewID(){
        return nextAvailablePageID++;
    }

    public LeafNode createLeafNode() throws IOException {
        return new LeafNode(this, getNewID());
    }

    public LeafNode createLeafNode(LinkedList<Long[]> keys, Long followingNodeID) throws IOException {
        return new LeafNode(this, getNewID(), keys, followingNodeID);
    }


    public InternalNode createInternalNode() throws IOException {
        return new InternalNode(this, getNewID());
    }
    public InternalNode createInternalNode(LinkedList<Long[]> keys, LinkedList<Long> children) throws IOException {
        return new InternalNode(this, getNewID(), keys, children);
    }

    /**
     * When a node is changed, it will call this function to have itself written to the disk.
     * This better controls the instantiation of PageCursors,
     * limiting it to only being instantiated in the Tree.
     * @param node the Node which would like to be serialized.
     */
    public void writeNodeToPage(Node node){
        this.diskCache.writePage(node.id, node.serialize().array());
    }

    /**
     * Returns a Cursor at the first relevant result given a key.
     * @param key
     * @return
     * @throws IOException
     */
    public Cursor find(Long[] key) throws IOException {
        return getNode(rootNodePageID).find(key);
    }

    /**
     * TODO return a cursor where this was inserted to do bulk insertion later, maybe.
     * Get the root block and call insert on it.
     * If the root returns a split result, make a new block and set it as the root.
     * @param key
     */
    public void insert(Long[] key) throws IOException {
        SplitResult result = getNode(rootNodePageID).insert(key);
        if (result != null){ //Root block split.
            LinkedList<Long[]> keys = new LinkedList<>();
            LinkedList<Long> children = new LinkedList<>();
            keys.add(result.key);
            children.add(result.left);
            children.add(result.right);
            InternalNode newRoot = createInternalNode(keys, children);
            rootNodePageID = newRoot.id;
        }
    }

    /**
     * Save the state of this tree to the file to load later.
     */
    public void close() throws IOException {
        FileOutputStream fos = new FileOutputStream(tree_filename);
        fos.write(serialize(this));
        fos.close();
    }

    /**
     * Serialize this Tree object
     * @param o
     * @return
     * @throws IOException
     */
    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.flush();
        oos.close();
        return baos.toByteArray();
    }

    /**
     * Deserialize this Tree object
     * @param bytes
     * @return
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private static Tree deserialize(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        Tree tree = null;
        try{
            Object o = ois.readObject();
            ois.close();
            if (o instanceof Tree){
                tree = (Tree) o;
            }
        }
        catch(ClassNotFoundException e){
            throw new InvalidClassException("Attempted to read invalid Tree object from byte array");
        }
        return tree;
    }
    /**
     * Methods implementing serialization of Tree
     * @throws InvalidObjectException
     */
    @Override
    public void validateObject() throws InvalidObjectException {

    }

    /**
     * Methods implementing serialization of Tree
     * @param out
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    /**
     * Method implementing serialization of Tree
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.registerValidation(this, 0);
        in.defaultReadObject();
    }
}
