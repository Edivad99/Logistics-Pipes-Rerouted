package logisticspipes.util;

public final class SystemUtil {

    private SystemUtil() {
    }

    public static boolean checkBooleanProperty(String name) {
        String value = System.getProperty(name);
        return value != null && value.equalsIgnoreCase("true");
    }
}
