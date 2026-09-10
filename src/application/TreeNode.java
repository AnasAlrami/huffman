package application;

public class TreeNode implements Comparable<TreeNode> {

    private byte symbol;
    private int weight;
    private TreeNode lchild;
    private TreeNode rchild;

    /** Construct a leaf node for a specific byte symbol. */
    public TreeNode(byte symbol, int weight) {
        this.symbol  = symbol;
        this.weight  = weight;
        this.lchild  = null;
        this.rchild  = null;
    }

    /** Construct an internal (non-leaf) node that merges two sub-trees. */
    public TreeNode(int combinedWeight, TreeNode left, TreeNode right) {
        this.symbol  = 0;
        this.weight  = combinedWeight;
        this.lchild  = left;
        this.rchild  = right;
    }

    /** A node is a leaf when it has no children. */
    public boolean isLeaf() {
        return lchild == null && rchild == null;
    }

    public byte getSymbol()               { return symbol; }
    public void setSymbol(byte s)         { this.symbol = s; }
    public int  getWeight()               { return weight; }
    public TreeNode getLchild()           { return lchild; }
    public TreeNode getRchild()           { return rchild; }
    public void setLchild(TreeNode node)  { this.lchild = node; }
    public void setRchild(TreeNode node)  { this.rchild = node; }

    @Override
    public int compareTo(TreeNode other) {
        return Integer.compare(this.weight, other.weight);
    }

    @Override
    public String toString() {
        return "TreeNode[sym='" + (char) symbol + "', wt=" + weight + "]";
    }
}