package application;

public class SymbolRecord {

    private String charDisplay;   // printable character (or special label)
    private int    charCode;      // numeric ASCII / byte value
    private int    occurrences;   // how many times this byte appears
    private String ratio;         // percentage of total bytes, formatted
    private String huffBits;      // Huffman bit-string assigned to this byte
    private String canonBits;     // canonical Huffman bit-string
    private int    bitLength;     // length of the code in bits

    /** Constructor used during compression (canonical code filled in later). */
    public SymbolRecord(String charDisplay, int occurrences,
                        String ratio, String huffBits, int bitLength) {
        this.charDisplay  = charDisplay;
        this.charCode     = resolveCode(charDisplay);
        this.occurrences  = occurrences;
        this.ratio        = ratio;
        this.huffBits     = huffBits;
        this.canonBits    = "";
        this.bitLength    = bitLength;
    }

    /** Full constructor used when all fields are known up front. */
    public SymbolRecord(String charDisplay, int occurrences, String ratio,
                        String huffBits, String canonBits, int bitLength) {
        this.charDisplay  = charDisplay;
        this.charCode     = resolveCode(charDisplay);
        this.occurrences  = occurrences;
        this.ratio        = ratio;
        this.huffBits     = huffBits;
        this.canonBits    = canonBits;
        this.bitLength    = bitLength;
    }

    private static int resolveCode(String s) {
        return (s == null || s.isEmpty()) ? -1 : (int) s.charAt(0);
    }

    // --- Getters ---

    public String getCharDisplay() { return charDisplay; }
    public int    getCharCode()    { return charCode; }
    public int    getOccurrences() { return occurrences; }
    public String getRatio()       { return ratio; }
    public String getHuffBits()    { return huffBits; }
    public String getCanonBits()   { return canonBits; }
    public int    getBitLength()   { return bitLength; }

    // --- Setters ---

    public void setCharDisplay(String s) {
        this.charDisplay = s;
        this.charCode    = resolveCode(s);
    }
    public void setOccurrences(int n)  { this.occurrences = n; }
    public void setRatio(String r)     { this.ratio = r; }
    public void setHuffBits(String s)  { this.huffBits = s; }
    public void setCanonBits(String s) { this.canonBits = s; }
    public void setBitLength(int n)    { this.bitLength = n; }
}