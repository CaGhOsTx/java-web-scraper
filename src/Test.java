import carlos.utilities.BitSet;

import java.util.List;

public class Test {
    public static void main(String[] args) {
        BitSet b1 = new BitSet(List.of(1,3,4,9,7)), b2 = new BitSet(List.of(1,5,4,2,3,10,9,15));
        System.out.println(b1);
        System.out.println(b2);
    }
}
