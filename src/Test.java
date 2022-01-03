import carlos.utilities.BitSet;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        System.out.println(Integer.toBinaryString(-128));
        System.out.println(Integer.toBinaryString(127) + " &");
        System.out.println(Integer.toBinaryString(-128 & 127));
    }
}
