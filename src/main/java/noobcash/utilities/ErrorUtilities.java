package noobcash.utilities;

import java.util.logging.Logger;

public class ErrorUtilities {

    private static final Logger LOGGER = Logger.getLogger("NOOBCASH");
    public static void fatal(String message) {
        LOGGER.severe(message);
        System.exit(1);
    }
}
