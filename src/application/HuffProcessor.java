package application;

import java.io.*;

public class HuffProcessor {

    private static final int ALPHABET  = 256;
    private static final int IO_BUFFER = 8192;

    private TreeNode        huffRoot;
    private final int[]     tally       = new int[ALPHABET];   // byte frequencies
    private final String[]  symbolCodes = new String[ALPHABET];// Huffman codes
    private final String[]  canonCodes  = new String[ALPHABET];// canonical codes
    private final SymbolRecord[] records = new SymbolRecord[ALPHABET];

    private long   originalBytes = 0;
    private long   encodedBytes  = 0;
    private String metadataText  = "";
    private String storedExt     = "";

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Compress inputFile and write a .huff archive into outputDir. */
    public void encode(File inputFile, String outputDir) throws IOException {
        resetState();
        tallyBytes(inputFile);
        constructTree();
        assignCodes(huffRoot, "");
        populateRecords();
        buildCanonical();

        String base    = stripExtension(inputFile.getName());
        File   outFile = new File(outputDir, base + ".huff");

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(outFile), IO_BUFFER))) {
            metadataText = writeMetadata(inputFile, out);
            writeBitstream(inputFile, out);
        }
        encodedBytes = outFile.length();
    }

    /** Decompress a .huff archive and write the restored file into outputDir. */
    public void decode(File inputFile, String outputDir) throws IOException {
        resetState();

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(inputFile), IO_BUFFER))) {

            metadataText = readMetadata(in);
            buildCanonical();
            TreeNode decodeRoot = rebuildDecodeTree();

            String restoredName = inputFile.getName().replace(".huff", "");
            File outFile = new File(outputDir,
                    restoredName + (storedExt.isEmpty() ? "" : "." + storedExt));

            decodeStream(in, outFile, decodeRoot);
        }
        encodedBytes = inputFile.length();
    }

    // -----------------------------------------------------------------------
    // Compression helpers
    // -----------------------------------------------------------------------

    private void resetState() {
        for (int i = 0; i < ALPHABET; i++) {
            tally[i]       = 0;
            symbolCodes[i] = null;
            canonCodes[i]  = null;
            records[i]     = null;
        }
        huffRoot      = null;
        originalBytes = 0;
        encodedBytes  = 0;
        metadataText  = "";
        storedExt     = "";
    }

    /** Count how many times each byte value appears in the file. */
    private void tallyBytes(File f) throws IOException {
        byte[] buf = new byte[IO_BUFFER];
        try (BufferedInputStream in =
                new BufferedInputStream(new FileInputStream(f), IO_BUFFER)) {
            int read;
            while ((read = in.read(buf)) != -1)
                for (int k = 0; k < read; k++)
                    tally[buf[k] & 0xFF]++;
        }
        originalBytes = f.length();
    }

    /** Build the Huffman tree using the custom MinPQ. */
    private void constructTree() {
        MinPQ<TreeNode> pq = new MinPQ<>();

        for (int i = 0; i < ALPHABET; i++)
            if (tally[i] > 0)
                pq.insert(new TreeNode((byte) i, tally[i]));

        while (pq.count() > 1) {
            TreeNode first  = pq.extractMin();
            TreeNode second = pq.extractMin();
            pq.insert(new TreeNode(first.getWeight() + second.getWeight(), first, second));
        }

        huffRoot = pq.extractMin();

        // Single distinct symbol edge case: wrap in an internal node
        if (huffRoot != null && huffRoot.isLeaf())
            huffRoot = new TreeNode(huffRoot.getWeight(), huffRoot, null);
    }

    /**
     * Recursive DFS that assigns a binary string to every leaf symbol.
     * Left edge appends '0', right edge appends '1'.
     */
    private void assignCodes(TreeNode node, String path) {
        if (node == null) return;

        if (node.isLeaf()) {
            symbolCodes[node.getSymbol() & 0xFF] = path.isEmpty() ? "0" : path;
            return;
        }

        assignCodes(node.getLchild(), path + "0");
        assignCodes(node.getRchild(), path + "1");
    }

    /** Fill the records array using tally counts and symbolCodes. */
    private void populateRecords() {
        for (int i = 0; i < ALPHABET; i++) {
            if (symbolCodes[i] != null) {
                records[i] = new SymbolRecord(
                        String.valueOf((char) i),
                        tally[i],
                        "",
                        symbolCodes[i],
                        "",
                        symbolCodes[i].length()
                );
            }
        }
    }

    // -----------------------------------------------------------------------
    // Canonical Huffman codes  (no java.util — plain arrays + selection sort)
    // -----------------------------------------------------------------------

    /**
     * Derive canonical codes from the code lengths in the records array.
     *
     * Steps:
     *  1. Collect active (byteValue, codeLength) pairs into two int arrays.
     *  2. Sort them with selection sort: primary key = length, secondary = byteValue.
     *  3. Walk the sorted list and assign incrementing binary codes, left-shifting
     *     whenever the code length increases.
     */
    private void buildCanonical() {
        // Step 1 — count active symbols and fill parallel arrays
        int n = 0;
        for (int i = 0; i < ALPHABET; i++)
            if (records[i] != null) n++;

        if (n == 0) return;

        int[] syms = new int[n];
        int[] lens = new int[n];

        int pos = 0;
        for (int i = 0; i < ALPHABET; i++) {
            if (records[i] != null) {
                syms[pos] = i;
                lens[pos] = records[i].getBitLength();
                pos++;
            }
        }

        // Step 2 — selection sort on (lens, syms)
        for (int i = 0; i < n - 1; i++) {
            int minAt = i;
            for (int j = i + 1; j < n; j++) {
                if (lens[j] < lens[minAt] ||
                        (lens[j] == lens[minAt] && syms[j] < syms[minAt])) {
                    minAt = j;
                }
            }
            int tmp  = lens[i];  lens[i]  = lens[minAt];  lens[minAt]  = tmp;
                tmp  = syms[i];  syms[i]  = syms[minAt];  syms[minAt]  = tmp;
        }

        // Step 3 — assign codes sequentially
        int code    = 0;
        int prevLen = lens[0];

        for (int i = 0; i < n; i++) {
            code <<= (lens[i] - prevLen);

            String bits = Integer.toBinaryString(code);
            while (bits.length() < lens[i])
                bits = "0" + bits;

            canonCodes[syms[i]] = bits;
            records[syms[i]].setCanonBits(bits);

            code++;
            prevLen = lens[i];
        }
    }

    // -----------------------------------------------------------------------
    // File I/O: writing
    // -----------------------------------------------------------------------

    /**
     * Write the compressed file header.
     *
     * Layout:
     *   [1 byte]  extension length N
     *   [N bytes] original extension characters
     *   [1 byte]  (distinct symbol count - 1)
     *   per symbol: [1 byte] value, [1 byte] canonical code length
     *   [8 bytes] original file size (long)
     */
    private String writeMetadata(File srcFile, DataOutputStream out) throws IOException {
        StringBuilder display = new StringBuilder();

        String name   = srcFile.getName();
        int    dotPos = name.lastIndexOf('.');
        String ext    = (dotPos == -1) ? "" : name.substring(dotPos + 1);

        out.writeByte(ext.length());
        display.append(ext.length()).append('\n');

        for (int i = 0; i < ext.length(); i++)
            out.writeByte((byte) ext.charAt(i));
        display.append(ext).append('\n');

        int distinct = 0;
        for (int i = 0; i < ALPHABET; i++)
            if (records[i] != null) distinct++;

        out.writeByte(distinct - 1);
        display.append(distinct).append('\n');

        for (int i = 0; i < ALPHABET; i++) {
            if (records[i] != null) {
                out.writeByte(i);
                out.writeByte(records[i].getBitLength());
                display.append(i).append(' ').append(records[i].getBitLength()).append('\n');
            }
        }

        out.writeLong(srcFile.length());
        display.append(srcFile.length()).append('\n');

        return display.toString();
    }

    /**
     * Re-read the source file and emit a bit-packed stream using canonical codes,
     * padding the final byte with zeros on the right.
     */
    private void writeBitstream(File srcFile, DataOutputStream out) throws IOException {
        byte[] inBuf  = new byte[IO_BUFFER];
        byte[] outBuf = new byte[IO_BUFFER];

        int pendingBits  = 0;
        int pendingValue = 0;
        int outPos       = 0;

        try (BufferedInputStream in =
                new BufferedInputStream(new FileInputStream(srcFile), IO_BUFFER)) {
            int read;
            while ((read = in.read(inBuf)) != -1) {
                for (int k = 0; k < read; k++) {
                    String code = canonCodes[inBuf[k] & 0xFF];
                    for (int b = 0; b < code.length(); b++) {
                        pendingValue = (pendingValue << 1) | (code.charAt(b) - '0');
                        if (++pendingBits == 8) {
                            outBuf[outPos++] = (byte) pendingValue;
                            pendingValue     = 0;
                            pendingBits      = 0;
                            if (outPos == IO_BUFFER) {
                                out.write(outBuf);
                                outPos = 0;
                            }
                        }
                    }
                }
            }
        }

        if (pendingBits > 0)
            outBuf[outPos++] = (byte) (pendingValue << (8 - pendingBits));

        if (outPos > 0)
            out.write(outBuf, 0, outPos);
    }

    // -----------------------------------------------------------------------
    // File I/O: reading (decompression)
    // -----------------------------------------------------------------------

    /**
     * Parse the header from the compressed stream, populate records with
     * code lengths, and return a human-readable snapshot of the header.
     */
    private String readMetadata(DataInputStream in) throws IOException {
        StringBuilder display = new StringBuilder();

        int extLen = in.readUnsignedByte();
        display.append(extLen).append('\n');

        char[] extChars = new char[extLen];
        for (int i = 0; i < extLen; i++)
            extChars[i] = (char) in.readUnsignedByte();
        storedExt = new String(extChars);
        display.append(storedExt).append('\n');

        int symbolCount = in.readUnsignedByte() + 1;
        display.append(symbolCount).append('\n');

        for (int i = 0; i < symbolCount; i++) {
            int byteVal = in.readUnsignedByte();
            int codeLen = in.readUnsignedByte();
            records[byteVal] = new SymbolRecord("", 0, "", "", "", codeLen);
            display.append(byteVal).append(' ').append(codeLen).append('\n');
        }

        originalBytes = in.readLong();
        display.append(originalBytes).append('\n');

        return display.toString();
    }

    /**
     * Build a binary trie from the canonical codes so the bit-stream can be
     * walked node-by-node during decompression.
     */
    private TreeNode rebuildDecodeTree() {
        TreeNode root = new TreeNode((byte) 0, 0);

        for (int i = 0; i < ALPHABET; i++) {
            if (canonCodes[i] == null) continue;

            TreeNode cursor = root;
            String   bits   = canonCodes[i];

            for (int k = 0; k < bits.length(); k++) {
                if (bits.charAt(k) == '0') {
                    if (cursor.getLchild() == null)
                        cursor.setLchild(new TreeNode((byte) 0, 0));
                    cursor = cursor.getLchild();
                } else {
                    if (cursor.getRchild() == null)
                        cursor.setRchild(new TreeNode((byte) 0, 0));
                    cursor = cursor.getRchild();
                }
            }
            cursor.setSymbol((byte) i);
        }
        return root;
    }

    /**
     * Walk the decode trie bit-by-bit and write exactly originalBytes bytes
     * to the output file.
     */
    private void decodeStream(DataInputStream in, File outFile,
                              TreeNode decodeRoot) throws IOException {
        byte[] inBuf = new byte[IO_BUFFER];

        try (BufferedOutputStream out =
                new BufferedOutputStream(new FileOutputStream(outFile), IO_BUFFER)) {

            long     written = 0;
            TreeNode cursor  = decodeRoot;
            int      read;

            while ((read = in.read(inBuf)) != -1 && written < originalBytes) {
                for (int k = 0; k < read && written < originalBytes; k++) {
                    int byteVal = inBuf[k] & 0xFF;

                    for (int bit = 7; bit >= 0 && written < originalBytes; bit--) {
                        cursor = ((byteVal >> bit) & 1) == 0
                                ? cursor.getLchild()
                                : cursor.getRchild();

                        if (cursor.isLeaf()) {
                            int sym = cursor.getSymbol() & 0xFF;
                            out.write(sym);
                            tally[sym]++;
                            written++;
                            cursor = decodeRoot;
                        }
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot == -1) ? filename : filename.substring(0, dot);
    }

    // -----------------------------------------------------------------------
    // Accessors for the UI layer
    // -----------------------------------------------------------------------

    public int[]          getFrequencies()  { return tally; }
    public String[]       getSymbolCodes()  { return symbolCodes; }
    public String[]       getCanonCodes()   { return canonCodes; }
    public SymbolRecord[] getRecords()      { return records; }
    public String         getMetadata()     { return metadataText; }
    public long           getOriginalSize() { return originalBytes; }
    public long           getEncodedSize()  { return encodedBytes; }
}