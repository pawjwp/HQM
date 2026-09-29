package hardcorequesting.common.io;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class LocalDataManager implements DataReader {
    
    private final Map<String, String> tempPaths = Maps.newHashMap();
    
    @Override
    public Optional<String> read(String name) {
        return Optional.ofNullable(tempPaths.get(name));
    }
    
    public void provide(String path, String str) {
        tempPaths.put(path, str);
    }

    @Override
    public Map<String, String> readFolder(String folder) {
        Map<String, String> files = new TreeMap<>();
        for (Map.Entry<String, String> entry : tempPaths.entrySet()) {
            if (entry.getKey().startsWith(folder + "/")) files.put(entry.getKey().substring(folder.length() + 1), entry.getValue());
        }
        return files;
    }
    
    @Override
    public String toString() {
        return String.format("Network data with %d temp paths (%s)", tempPaths.size(), tempPaths.keySet().stream().sorted().collect(Collectors.joining(", ")));
    }
}
