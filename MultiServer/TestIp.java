public class TestIp {
    public static void main(String[] args) throws Exception {
        System.out.println("GETLOCALHOST: " + java.net.InetAddress.getLocalHost().getHostAddress());
        System.out.println("FORMAT_UTIL: " + com.multiserver.common.util.FormatUtil.getLocalIpAddress());
    }
}

