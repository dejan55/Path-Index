package NeoIntegration;

import java.io.IOException;

public class Runner {
    public static void main(String[] args) throws IOException {
        if(args[0].equals("1")){
            BulkLUBMDataLoader.main(args);
        }
        else if (args[0].equals("2")){
            CleverIndexBuilder.main(args);
        }
        else if (args[0].equals("3")){
            LexographicIndexBuilder.main(args);
        }
        else if (args[0].equals("4")){
            simpleLUBMExperiments.main(args);
        }
        else if (args[0].equals("5")){
            LUBMExperiments.main(args);
        }
    }
}
