package service;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// Danh sách các client đang online và group mà mỗi client tham gia
public class ClientRegistry {
    private final Map<SocketAddress, Set<String>> clients = new ConcurrentHashMap<>();

    public void register(SocketAddress client) {
        clients.putIfAbsent(client, ConcurrentHashMap.newKeySet());
    }

    public void remove(SocketAddress client) {
        clients.remove(client);
    }

    public void join(SocketAddress client, String group) {
        register(client);
        clients.get(client).add(group);
    }

    public void leave(SocketAddress client, String group) {
        Set<String> groups = clients.get(client);
        if (groups != null) {
            groups.remove(group);
        }
    }

    public List<String> groupsOf(SocketAddress client) {
        Set<String> groups = clients.get(client);
        if (groups == null) {
            return Collections.emptyList();
        }
        return groups.stream().sorted().collect(Collectors.toList());
    }

    public List<SocketAddress> all() {
        return clients.keySet().stream().collect(Collectors.toList());
    }

    public List<SocketAddress> byIp(String ip) {
        return clients.keySet().stream()
                .filter(address -> ((InetSocketAddress) address).getAddress().getHostAddress().equals(ip))
                .collect(Collectors.toList());
    }

    public List<SocketAddress> byGroup(String group) {
        return clients.entrySet().stream()
                .filter(entry -> entry.getValue().contains(group))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}